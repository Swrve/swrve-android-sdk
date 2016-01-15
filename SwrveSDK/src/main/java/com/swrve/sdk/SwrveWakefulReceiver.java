package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

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
        if(intent.hasExtra(SwrveWakefulService.EXTRA_LOCATIONS_IMPRESSION_IDS)) {
            ArrayList<Integer> events = intent.getExtras().getIntegerArrayList(SwrveWakefulService.EXTRA_LOCATIONS_IMPRESSION_IDS);
            SwrveLogger.i(LOG_TAG, "SwrveWakefulReceiver. Location impression events: " + events);
            service.putExtras(intent);
        }
        if(intent.hasExtra(SwrveWakefulService.EXTRA_LOCATIONS_ENGAGED_IDS)) {
            ArrayList<Integer> events = intent.getExtras().getIntegerArrayList(SwrveWakefulService.EXTRA_LOCATIONS_ENGAGED_IDS);
            SwrveLogger.i(LOG_TAG, "SwrveWakefulReceiver. Location engagement events: " + events);
            service.putExtras(intent);
        }
        startWakefulService(context, service);
    }
}
