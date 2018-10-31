package com.swrve.sdk;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import com.amazon.device.messaging.ADMMessageHandlerBase;

public class SwrveAdmIntentService extends ADMMessageHandlerBase {

    public SwrveAdmIntentService() {
        super(SwrveAdmIntentService.class.getName());
    }

    public SwrveAdmIntentService(final String className) {
        super(className);
    }

    @Override
    @VisibleForTesting
    public void onMessage(final Intent intent) {
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

            boolean isDupe = new SwrvePushDeDuper(this).isDupe(extras);
            if (!isDupe) {
                getSwrvePushServiceManager().processMessage(extras);
            }
        }
    }

    @Override
    protected void onRegistrationError(final String string) {
        SwrveLogger.e("ADM Registration Error. Error string: %s", string); //This is considered fatal for ADM
    }

    @Override
    protected void onRegistered(final String registrationId) {
        SwrveLogger.i("ADM Registered. RegistrationId: %s", registrationId);
        ISwrveBase sdk = SwrveSDK.getInstance();
        if (sdk != null && sdk instanceof Swrve) {
            ((Swrve) sdk).onRegistrationIdReceived(registrationId);
        } else {
            SwrveLogger.e("Could not notify the SDK of a new token. Consider using the shared instance.");
        }
    }

    @Override
    protected void onUnregistered(final String registrationId) {
        SwrveLogger.i("ADM Unregistered. RegistrationId: %s", registrationId);
    }

    protected SwrvePushServiceManager getSwrvePushServiceManager() {
        return new SwrvePushServiceManager(this);
    }
}

