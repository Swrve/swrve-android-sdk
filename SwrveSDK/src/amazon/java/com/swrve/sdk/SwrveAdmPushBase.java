package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_PRIORITY;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class SwrveAdmPushBase {

    private static final String EXTRA_PRIORITY = "adm_message_priority";

    protected void onMessage(Context context, Intent intent) {
        if (!SwrveHelper.sdkAvailable()) {
            return;
        }

        if (intent == null) {
            SwrveLogger.e("ADM messaging runtimes have called onMessage() with unexpected null intent.");
            return;
        }

        final Bundle extras = intent.getExtras();
        if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
            SwrveLogger.i("Received ADM notification: %s", extras.toString());

            if (!SwrveHelper.isSwrvePush(extras)) {
                SwrveLogger.i("Received Push: but not processing as it doesn't contain: %s or %s", SwrveNotificationConstants.SWRVE_TRACKING_KEY, SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY);
                return;
            }

            //Get the received *message* (not notification) priority so we can forward it with push delivered event
            if (extras.containsKey(EXTRA_PRIORITY)) {
                extras.putString(GENERIC_EVENT_PAYLOAD_PRIORITY, String.valueOf(extras.getString(EXTRA_PRIORITY)));
            }

            boolean isDupe = new SwrvePushDeDuper(context).isDupe(extras);
            if (!isDupe) {
                getSwrvePushManager(context).processMessage(extras);
            }
        }
    }

    protected void onRegistered(String registrationId) {
        SwrveLogger.i("ADM Registered. RegistrationId: %s", registrationId);
        ISwrveBase sdk = SwrveSDK.getInstance();
        if (sdk != null && sdk instanceof Swrve) {
            ((Swrve) sdk).onRegistrationIdReceived(registrationId);
        } else {
            SwrveLogger.e("Could not notify the SDK of a new token. Consider using the shared instance.");
        }
    }

    protected SwrvePushManager getSwrvePushManager(Context context) {
        return new SwrvePushManagerImp(context);
    }
}
