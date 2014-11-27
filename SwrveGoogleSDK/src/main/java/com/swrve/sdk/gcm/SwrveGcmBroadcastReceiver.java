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
    private Class<?> intentServiceClass = SwrveGcmIntentService.class;

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

            ComponentName comp = new ComponentName(context.getPackageName(), intentServiceClass.getName());
            startWakefulService(context, (intent.setComponent(comp)));

            if (!intent.hasExtra(SwrveGcmNotification.GCM_BUNDLE)) {
                setResultCode(Activity.RESULT_OK); // set result code for GCM ordered broadcast only.
            }
        }
    }
}