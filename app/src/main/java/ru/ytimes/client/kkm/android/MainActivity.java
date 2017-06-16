package ru.ytimes.client.kkm.android;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.ytimes.client.kkm.android.printer.LogPrinter;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

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
        kkmServer = new KKMServer(4900, "87fa");
        kkmServer.setPrinter(new LogPrinter());
    }


}
