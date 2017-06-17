package ru.ytimes.client.kkm.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.atol.drivers.fptr.settings.SettingsActivity;

import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import ru.ytimes.client.kkm.android.printer.AtolPrinter;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView kkmStatusText;

    private KKMServer kkmServer;
    private AtolPrinter printer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        printer = new AtolPrinter(getApplication());
        kkmStatusText = (TextView)findViewById(R.id.kkmStatusView);

        if ("google_sdk".equals( Build.PRODUCT )) {
            java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
            java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        }

        if (true) {
            startServer();

            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            String settings = sharedPref.getString(getString(R.string.settings_kkm), null);
            if (settings != null && !settings.isEmpty()) {
                printer.connect(getApplication(), settings, kkmStatusText);
            }
        }
    }

    protected void setStatus(String status, String isError) {
        kkmStatusText.setText(status);
        if ("true".equals(isError)) {
            kkmStatusText.setTextColor(Color.parseColor("#ffcc00"));
        }
        else {
            kkmStatusText.setTextColor(Color.parseColor("#ff6699"));
        }
    }

    public void onKKMSettingsClick(View view){
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(SettingsActivity.DEVICE_SETTINGS, printer.getDefaultSettings());
        startActivityForResult(intent, 1);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1){
            if(data!=null && data.getExtras()!=null){
                String settings  = data.getExtras().getString(SettingsActivity.DEVICE_SETTINGS);

                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.settings_kkm), settings);
                editor.commit();

                printer.connect(getApplication(), settings, kkmStatusText);
            }
        }
    }

    public boolean startServer() {
        Log.i(TAG, "start kkm server");
        setStatus("Запуск сервера", "");
        try {
            SSLContext context = getSSLContext();
            int port = 4900;
            kkmServer = new KKMServer(port, "87fa");
            kkmServer.setPrinter(printer);
            kkmServer.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(context));
            kkmServer.start();
            setStatus("Сервер успешно запущен на порту: " + port, "");
            return true;
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            setStatus("Ошибка запуска сервера: " + e.getMessage(), "true");
            return false;
        }
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
        SSLContext.setDefault(sslContext);
        return sslContext;
    }


}
