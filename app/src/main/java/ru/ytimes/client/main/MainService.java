package ru.ytimes.client.main;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;

import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import fi.iki.elonen.NanoHTTPD;
import ru.ytimes.client.kkm.android.printer.Printer;
import ru.ytimes.client.kkm.android.record.DeviceModuleCheckRecord;
import ru.ytimes.client.kkm.android.record.PrintCheckCommandRecord;
import ru.ytimes.client.kkm.android.record.ServerResult;
import ru.ytimes.client.utils.StringUtils;

/**
 * Created by andrey on 18.06.17.
 */

public class MainService extends Service {
    private static final String TAG = "YTIMES";
    private WebServer webServer;
    private WSServer wsServer;
    private Timer timer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Service")
                .setContentText("Обработка запросов")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        stop();
        startServer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        Log.i(MainService.class.getSimpleName(), "Destroy service");
        Intent broadcastIntent = new Intent("ytimes.restartService");
        sendBroadcast(broadcastIntent);
    }

    public void stop() {
        try {
            if (webServer != null) {
                webServer.stop();
            }
            if (wsServer != null) {
                wsServer.stop();
            }
            if (timer != null) {
                timer.cancel();
            }
        }
        catch (Exception e) {
        }
    }

    public boolean startServer() {
        System.setProperty("AccessControlAllowHeader", "*");

        Log.i(TAG, "start kkm server");
        try {
            SSLContext context = getSSLContext();
            int port = 4900;
            webServer = new WebServer(port, context.getServerSocketFactory(), getApplication());
            webServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            showMessage("Веб сервер успешно запущен на порту: " + port);

            wsServer = new WSServer(getApplication(), 4910);
            wsServer.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(context));
            wsServer.start();
            webServer.setScreenWsServer(wsServer);

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                try {
                    processLoadChecksForPrint();
                }
                catch (Exception e) {
                    showMessage("ERROR: " + e.getMessage());
                }
                }
            }, 60000, 180000);

            try {
                String ipAddress = Utils.getIPAddress(true);
                showMessage("IP адрес: " + ipAddress);
            }
            catch (Exception e) {
            }
            webServer.initPrinter(false);
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            showMessage("Ошибка запуска сервера: " + e.getMessage());
            return false;
        }


        return true;
    }

    public void showMessage(String message) {
        Intent local = new Intent();
        local.setAction("ytimes.message");
        local.putExtra("message", message);
        this.sendBroadcast(local);
    }

    private SSLContext getSSLContext() throws Exception {
        InputStream keystoreInput = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("keystore");
        SSLContext context = getSSLFactories(keystoreInput, "ytimes");
        keystoreInput.close();
        return context;
    }

    private SSLContext getSSLFactories(InputStream keyStream, String keyStorePassword) throws Exception {
        // Get keyStore
        KeyStore keyStore = KeyStore.getInstance("BKS");

        // if your store is password protected then declare it (it can be null however)
        char[] keyPassword = keyStorePassword.toCharArray();

        // load the stream to your store
        keyStore.load(keyStream, keyPassword);

        // initialize a trust manager factory with the trusted store
        KeyManagerFactory keyFactory =
                KeyManagerFactory.getInstance("PKIX");
        keyFactory.init(keyStore, keyPassword);

        // get the trust managers from the factory
        KeyManager[] keyManagers = keyFactory.getKeyManagers();

        // Now get trustStore
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

        // if your store is password protected then declare it (it can be null however)
        //char[] trustPassword = password.toCharArray();

        // initialize a trust manager factory with the trusted store
        TrustManagerFactory trustFactory =
                TrustManagerFactory.getInstance("PKIX");
        trustFactory.init(keyStore);

        // get the trust managers from the factory
        TrustManager[] trustManagers = trustFactory.getTrustManagers();

        // initialize an ssl context to use these managers and set as default
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);
        return sslContext;
    }

    private void processLoadChecksForPrint() throws Exception {
        SharedPreferences preferences = getApplication().getSharedPreferences("kkm", Context.MODE_PRIVATE);
        Map<String, ?> preferencesAll = preferences.getAll();

        String accountExternalId = (String) preferencesAll.get("accountExternalId");
        if (StringUtils.isEmpty(accountExternalId)) {
            return;
        }
        String externalBaseUrl = (String) preferencesAll.get("accountExternalBaseUrl");
        if (StringUtils.isEmpty(externalBaseUrl)) {
            return;
        }

        String url = externalBaseUrl + "util/module/check/listForPrint";

        Map<String, String> params = new HashMap<>();
        params.put("accountExternalId", accountExternalId);
        String s = performPostCall(url, params);

        ServerResult<DeviceModuleCheckRecord> result = Utils.parseMessage(s, new TypeReference<ServerResult<DeviceModuleCheckRecord>>() {});
        if (!result.isSuccess()) {
            String error = "Server error";
            if (result.getErrors() != null && result.getErrors().size() > 0) {
                error = result.getErrors().get(0).getMessage();
            }
            showMessage("ERROR: " + error);
            return;
        }
        if (result.getRows().size() == 0) {
            return;
        }
        showMessage("Receive check list for print: " + s);

        Printer printer = webServer.getPrinter();
        if (!printer.isConnected()) {
            printer.connect(getApplication());
        }

        for(DeviceModuleCheckRecord checkRecord: result.getRows()) {
            PrintCheckCommandRecord record = Utils.parseMessage(checkRecord.body, PrintCheckCommandRecord.class);
            try {
                printer.printCheck(record);
            }
            catch (Exception e) {
                showMessage("ERROR: " + e.getMessage());
                try {
                    sendPrintCheckErrorToServer(externalBaseUrl, checkRecord.guid, e.getMessage());
                }
                catch (Exception e1) {
                    showMessage("ERROR: " + e1.getMessage());
                }
            }
        }
    }

    private void sendPrintCheckErrorToServer(String baseUrl, String checkGuid, String errorMessage) throws IOException {
        showMessage("Send error to server: " + checkGuid + ", " + errorMessage);

        String url = baseUrl + "module/check/updateError";

        Map<String, String> params = new HashMap<>();
        params.put("guid", checkGuid);
        params.put("errorMessage", errorMessage);

        String s = performPostCall(url, params);
        showMessage("Send error response: " + s);
    }

    public String  performPostCall(String requestURL,
                                   Map<String, String> postDataParams) {

        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(30000);
            conn.setConnectTimeout(30000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);


            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else {
                showMessage("ERROR: server response code is " + responseCode);
                response="";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    private String getPostDataString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

}
