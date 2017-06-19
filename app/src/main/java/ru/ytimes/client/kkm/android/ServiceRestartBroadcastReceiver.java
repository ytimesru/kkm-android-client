package ru.ytimes.client.kkm.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by andrey on 18.06.17.
 */

public class ServiceRestartBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(ServiceRestartBroadcastReceiver.class.getSimpleName(), "Service Stops!");
        Intent i = new Intent(context, MainService.class);
        context.startService(i);
    }



}
