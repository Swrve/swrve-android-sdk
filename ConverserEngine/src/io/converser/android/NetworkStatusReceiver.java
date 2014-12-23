package io.converser.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import io.converser.android.engine.BuildConfig;

public class NetworkStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        //We have new netowrk state info.

        if (BuildConfig.DEBUG) {
            Log.d(Constants.LOGTAG, "We got a network broadcast!!");
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = cm.getActiveNetworkInfo();

        if (activeInfo != null && activeInfo.isConnectedOrConnecting()) {
            //Ok, so something CHANGED and we have connection? Means we just got connection, right?
            //Ok, the fact that we are active means there's probably something we want to do. call the service.

            context.startService(new Intent(context, ConverserQueueService.class));
        }

    }

}
