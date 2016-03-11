package com.swrve.sdk.demo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GcmReceiver;
import com.swrve.sdk.SwrveHelper;

/*
 * Use this class when you have multiple push providers, that is, multiple GcmListenerServices.
 * It will detect Swrve pushes and redirect them to the Swrve GcmListenerService so that they can
 * be properly displayed and processed.
 */
public class CustomGcmReceiver extends GcmReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean interceptedIntent = false;
        // Call the Swrve intent service if the push contains the Swrve payload _p
        if("com.google.android.c2dm.intent.RECEIVE".equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Object rawId = extras.get("_p");
                String msgId = (rawId != null) ? rawId.toString() : null;
                if (!SwrveHelper.isNullOrEmpty(msgId)) {
                    // It is a Swrve push!
                    interceptedIntent = true;
                    ComponentName comp = new ComponentName(context.getPackageName(), "com.swrve.sdk.gcm.SwrveGcmIntentService");
                    intent = intent.setComponent(comp);
                    context.startService(intent);
                }
            }
        }
        if (!interceptedIntent) {
            // Continue normally if the push is not a Swrve push
            super.onReceive(context, intent);
        }
    }
}
