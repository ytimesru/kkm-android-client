package ru.ytimes.client.kkm.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by andrey on 18.06.17.
 */

public class ServiceRestartBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "YTIMES";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Service Stops!");

        SharedPreferences sharedPref = context.getSharedPreferences("kkm", Context.MODE_PRIVATE);
        String settings = sharedPref.getString(context.getString(R.string.settings_kkm), null);
        if (settings != null && !settings.isEmpty()) {
            MainService.setSettings(settings);
        }

        Intent i = new Intent(context, MainService.class);
        context.startService(i);
    }



}
