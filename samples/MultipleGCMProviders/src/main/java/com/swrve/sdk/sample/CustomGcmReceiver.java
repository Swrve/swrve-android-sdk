package com.swrve.sdk.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GcmReceiver;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrvePushSDK;
import com.swrve.sdk.gcm.SwrveGcmConstants;
import com.swrve.sdk.gcm.SwrveGcmIntentService;

/*
 * Use this class when you have multiple push providers, that is, multiple GcmListenerServices.
 * It will detect Swrve pushes and redirect them to the Swrve GcmListenerService so that they can
 * be properly displayed and processed.
 */
public class CustomGcmReceiver extends GcmReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean interceptedIntent = false;
        // Call the Swrve intent service if the push contains the Swrve identifier
        if("com.google.android.c2dm.intent.RECEIVE".equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String msgId = SwrvePushSDK.getPushId(extras);
                if (!SwrveHelper.isNullOrEmpty(msgId)) {
                    // It is a Swrve push!
                    SwrveLogger.d("Received Swrve push, starting %s instead", SwrveGcmConstants.SWRVE_DEFAULT_INTENT_SERVICE);
                    SwrveGcmIntentService swrveIntent = new SwrveGcmIntentService();
                    swrveIntent.onCreate();
                    swrveIntent.onMessageReceived("CustomGcmReceiver", extras);
                    interceptedIntent = true;
                }
            }
        }
        if (!interceptedIntent) {
            // Continue normally if the push is not a Swrve push
            super.onReceive(context, intent);
        }
    }
}
