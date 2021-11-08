package com.swrve.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SwrveNotificationEngageReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            getSwrveNotificationEngage(context).processIntent(intent);
        } catch (Exception ex) {
            SwrveLogger.e("SwrveNotificationEngageReceiver. Error processing intent. Intent: %s", ex, intent.toString());
        }
    }

    protected SwrveNotificationEngage getSwrveNotificationEngage(Context context) {
        return new SwrveNotificationEngage(context);
    }
}
