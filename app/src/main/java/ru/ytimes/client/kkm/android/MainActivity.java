package ru.ytimes.client.kkm.android;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "YTIMES";

    private TextView kkmStatusText;

    private Intent serviceIntent;
    private BroadcastReceiver uiReceiver;
    private Context ctx;
    private List<String> messages = new LinkedList<>();
    private SimpleDateFormat formatter = new SimpleDateFormat("hh:mm");

    public Context getCtx() {
        return ctx;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if ("google_sdk".equals( Build.PRODUCT )) {
            java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
            java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        }

        ctx = this;
        serviceIntent = new Intent(getCtx(), MainService.class);
        kkmStatusText = (TextView)findViewById(R.id.kkmStatusView);
        kkmStatusText.setMovementMethod(new ScrollingMovementMethod());

        IntentFilter filter = new IntentFilter();
        filter.addAction("ytimes.message");
        uiReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String mes = intent.getStringExtra("message");
                mes = formatter.format(new Date()) + ": " + mes;
                messages.add(mes);
                if (messages.size() > 20) {
                    messages.remove(0);
                }
                StringBuilder res = new StringBuilder();
                for(String s: messages) {
                    if (res.length() > 0) {
                        res = res.append("\n");
                    }
                    res = res.append(s);
                }
                kkmStatusText.setText(res);

                final Layout layout = kkmStatusText.getLayout();
                if(layout != null){
                    int scrollDelta = layout.getLineBottom(kkmStatusText.getLineCount() - 1)
                            - kkmStatusText.getScrollY() - kkmStatusText.getHeight();
                    if(scrollDelta > 0) {
                        kkmStatusText.scrollBy(0, scrollDelta);
                    }
                }
            }

        };

        registerReceiver(uiReceiver, filter);
        startService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        stopService(serviceIntent);
        unregisterReceiver(uiReceiver);
        Log.i(TAG, "onDestroy activity");
        super.onDestroy();
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i (TAG, "Service is already running");
                return true;
            }
        }
        Log.i (TAG, "Service is not running");
        return false;
    }

    public void onConnectButtonClick(View view) {
        stopService(serviceIntent);
    }

}
