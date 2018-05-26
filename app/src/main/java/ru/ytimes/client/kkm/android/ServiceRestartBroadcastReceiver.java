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

        Intent i = new Intent(context, MainService.class);
        context.startService(i);
    }



}
