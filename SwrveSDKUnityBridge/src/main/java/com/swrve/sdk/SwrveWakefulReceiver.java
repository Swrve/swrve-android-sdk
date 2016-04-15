package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.swrve.sdk.SwrveLogger;

import java.util.ArrayList;

public class SwrveWakefulReceiver extends WakefulBroadcastReceiver {

    private static final String LOG_TAG = "SwrveWakeful";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, SwrveWakefulService.class);
        if(intent.hasExtra(SwrveWakefulService.EXTRA_EVENTS)) {
            ArrayList<String> events = intent.getExtras().getStringArrayList(SwrveWakefulService.EXTRA_EVENTS);
            SwrveLogger.i(LOG_TAG, "SwrveWakefulReceiver. Events: " + events);
            service.putExtras(intent);
        }
        startWakefulService(context, service);
    }
}
