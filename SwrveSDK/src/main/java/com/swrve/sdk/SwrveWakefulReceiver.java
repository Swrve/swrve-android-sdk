package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import java.util.ArrayList;

public class SwrveWakefulReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, SwrveWakefulService.class);
        if (intent.hasExtra(SwrveBackgroundEventSender.EXTRA_EVENTS)) {
            ArrayList<String> events = intent.getExtras().getStringArrayList(SwrveBackgroundEventSender.EXTRA_EVENTS);
            SwrveLogger.i("SwrveWakefulReceiver. Events: %s", events);
            service.putExtras(intent);
        }
        startWakefulService(context, service);
    }
}
