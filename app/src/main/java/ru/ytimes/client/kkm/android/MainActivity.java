package ru.ytimes.client.kkm.android;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import ru.ytimes.client.kkm.android.printer.LogPrinter;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private KKMServer kkmServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if ("google_sdk".equals( Build.PRODUCT )) {
            java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
            java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        }
    }

    public void startServer(View view) {
        Log.i(TAG, "start kkm server");
        try {
            SSLContext context = getSSLContext();
            kkmServer = new KKMServer(4900, "87fa");
            kkmServer.setPrinter(new LogPrinter());
            kkmServer.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(context));
            kkmServer.start();
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
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
