package com.swrve.sdk.sample;

import android.app.IntentService;
import android.content.Intent;

import com.swrve.sdk.SwrveLogger;

public class GcmWakefulIntentService extends IntentService {

    public GcmWakefulIntentService() {
        super("GcmWakefulIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            new GcmSwrvePushService().processNotification(intent.getExtras());
        } catch (Exception ex) {
            SwrveLogger.e("GcmSwrvePushService exception (intent: %s): ", ex, intent);
        } finally {
            GcmWakefulReceiver.completeWakefulIntent(intent);
        }
    }
}
