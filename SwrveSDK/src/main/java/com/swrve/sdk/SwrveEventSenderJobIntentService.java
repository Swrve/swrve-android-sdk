package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.JobIntentService;

import java.util.ArrayList;

import static com.swrve.sdk.SwrveBackgroundEventSender.EXTRA_EVENTS;
import static com.swrve.sdk.SwrveBackgroundEventSender.EXTRA_USER_ID;

@RequiresApi(api = Build.VERSION_CODES.O)
public class SwrveEventSenderJobIntentService extends JobIntentService {

    private static final int JOB_ID = R.integer.swrve_event_sender_job_id; // use the actual generated integer value to guarantee uniqueness

    @RequiresApi(api = Build.VERSION_CODES.O)
    static void enqueueWork(Context context, String userId, ArrayList<String> events) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putStringArrayListExtra(EXTRA_EVENTS, events);
        enqueueWork(context, SwrveEventSenderJobIntentService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(Intent intent) {
        Bundle extras = intent.getExtras();
        try {
            final Swrve swrve = (Swrve) SwrveSDKBase.getInstance();
            SwrveBackgroundEventSender sender = new SwrveBackgroundEventSender(swrve, getApplicationContext());
            sender.handleSendEvents(extras);
        } catch (Exception ex) {
            SwrveLogger.e("SwrveEventSenderJobIntentService exception (extras: %s): ", ex, extras);
        }
    }
}
