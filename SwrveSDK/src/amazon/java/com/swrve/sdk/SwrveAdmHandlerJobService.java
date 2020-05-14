package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;

import com.amazon.device.messaging.ADMMessageHandlerJobBase;

// Used for latest Fire OS
public class SwrveAdmHandlerJobService extends ADMMessageHandlerJobBase {
    @Override
    protected void onMessage(Context context, Intent intent) {
        getPushBase().onMessage(context, intent);
    }

    @Override
    protected void onRegistrationError(Context context, String string) {
        SwrveLogger.e("ADM Registration Error. Error string: %s", string); //This is considered fatal for ADM
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        getPushBase().onRegistered(registrationId);
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        SwrveLogger.i("ADM Unregistered. RegistrationId: %s", registrationId);
    }

    protected SwrveAdmPushBase getPushBase() {
        return new SwrveAdmPushBase();
    }
}

