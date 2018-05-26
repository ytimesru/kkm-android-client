package ru.ytimes.client.kkm.android;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import fi.iki.elonen.NanoHTTPD;
import ru.ytimes.client.kkm.android.printer.AtolPrinter;
import ru.ytimes.client.kkm.android.printer.Printer;
import ru.ytimes.client.kkm.android.printer.TestPrinter;

/**
 * Created by andrey on 18.06.17.
 */

public class MainService extends Service {
    private static final String TAG = "YTIMES";
    private KKMWebServer kkmWebServer;
    private Printer printer;

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
            if (kkmWebServer != null) {
                kkmWebServer.stop();
            }
        }
        catch (Exception e) {
        }
        if (printer != null) {
            try {
                printer.stop();
            }
            catch (Exception e) {
            }
        }
    }

    public boolean startServer() {
        System.setProperty("AccessControlAllowHeader", "*");

        Log.i(TAG, "start kkm server");
        try {
            SSLContext context = getSSLContext();
            int port = 4900;
            kkmWebServer = new KKMWebServer(port, context.getServerSocketFactory(), getApplication());
            kkmWebServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            showMessage("Веб сервер успешно запущен на порту: " + port);
            try {
                String ipAddress = Utils.getIPAddress(true);
                showMessage("IP адрес: " + ipAddress);
            }
            catch (Exception e) {}
            kkmWebServer.initPrinter();
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

}
