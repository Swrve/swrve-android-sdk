package com.swrve.sdk;

import android.content.Intent;
import android.support.v4.app.JobIntentService;

public class SwrvePushServiceDefaultJobIntentService extends JobIntentService {

    @Override
    protected void onHandleWork(Intent intent) {
        try {
            new SwrvePushServiceManager(this).processMessage(intent.getExtras());
        } catch (Exception ex) {
            SwrveLogger.e("SwrvePushServiceDefaultJobIntentService exception (intent: %s): ", ex, intent);
        }
    }
}
