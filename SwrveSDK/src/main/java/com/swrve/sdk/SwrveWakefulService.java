package com.swrve.sdk;

import android.app.IntentService;
import android.content.Intent;

public class SwrveWakefulService extends IntentService {

    public SwrveWakefulService() {
        super("SwrveWakefulService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            SwrveBackgroundEventSender sender = getBackgroundEventSender();
            sender.handleSendEvents(intent.getExtras());
        } catch (Exception ex) {
            SwrveLogger.e("SwrveWakefulService exception (intent: %s): ", ex, intent);
        } finally {
            SwrveWakefulReceiver.completeWakefulIntent(intent);
        }
    }

    protected SwrveBackgroundEventSender getBackgroundEventSender() {
        final Swrve swrve = (Swrve) SwrveSDKBase.getInstance();
        return new SwrveBackgroundEventSender(swrve, getApplicationContext());
    }
}
