package com.swrve.sdk.gcm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.swrve.sdk.SwrveHelper;

/**
 * Used internally to receive push notifications inside for your app.
 */
public class SwrveGcmBroadcastReceiver extends WakefulBroadcastReceiver {
    private static String workaroundRegistrationId;
    private Class<?> intentServiceClass;

    public SwrveGcmBroadcastReceiver() {
        this(SwrveGcmIntentService.class);
    }

    public SwrveGcmBroadcastReceiver(Class<?> intentServiceClass) {
        this.intentServiceClass = intentServiceClass;
    }

    public static String getWorkaroundRegistrationId() {
        return workaroundRegistrationId;
    }

    public static void clearWorkaroundRegistrationId() {
        workaroundRegistrationId = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            final String registrationId = intent.getStringExtra("registration_id");
            if (!SwrveHelper.isNullOrEmpty(registrationId)) {
                // We got a registration id!
                workaroundRegistrationId = registrationId;
            }

            ComponentName comp = new ComponentName(context.getPackageName(),
                    intentServiceClass.getName());
            // Start the service, keeping the device awake while it is launching.
            startWakefulService(context, (intent.setComponent(comp)));
        }
        setResultCode(Activity.RESULT_OK);
    }
}