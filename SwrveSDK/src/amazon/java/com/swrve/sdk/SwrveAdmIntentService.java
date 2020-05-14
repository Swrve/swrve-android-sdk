package com.swrve.sdk;

import android.content.Intent;
import androidx.annotation.VisibleForTesting;

import com.amazon.device.messaging.ADMMessageHandlerBase;

// Used for old devices
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
        getPushBase().onMessage(this, intent);
    }

    @Override
    protected void onRegistrationError(final String string) {
        SwrveLogger.e("ADM Registration Error. Error string: %s", string); //This is considered fatal for ADM
    }

    @Override
    protected void onRegistered(final String registrationId) {
        getPushBase().onRegistered(registrationId);
    }

    @Override
    protected void onUnregistered(final String registrationId) {
        SwrveLogger.i("ADM Unregistered. RegistrationId: %s", registrationId);
    }

    protected SwrveAdmPushBase getPushBase() {
        return new SwrveAdmPushBase();
    }
}

