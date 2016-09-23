package com.swrve.sdk;

import android.content.Intent;
import android.os.Bundle;

import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.amazon.device.messaging.ADMMessageReceiver;

public class SwrveAdmIntentService extends ADMMessageHandlerBase {
    private final static String TAG = "SwrveAdm";
    SwrveAdmHandler handler;

    //SwrveMessageReceiver listens for messages from ADM
    public static class SwrveAdmMessageReceiver extends ADMMessageReceiver {
        public SwrveAdmMessageReceiver() {
            super(SwrveAdmIntentService.class);
        }
    }

    public SwrveAdmIntentService() {
        super(SwrveAdmIntentService.class.getName());
    }

    public SwrveAdmIntentService(final String className) {
        super(className);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new SwrveAdmHandler(getApplicationContext());
    }

    @Override
    protected void onRegistered(final String registrationId) {
        SwrveLogger.i(TAG, "ADM Registered. RegistrationId: " + registrationId);
        SwrvePushSDK.getInstance().onPushTokenUpdated(registrationId);
    }

    @Override
    protected void onMessage(final Intent intent) {
        final Bundle extras = intent.getExtras();
        handler.onMessageReceived(extras);
    }

    @Override
    protected void onRegistrationError(final String string) {
        //This is considered fatal for ADM
        SwrveLogger.e(TAG, "ADM Registration Error. Error string: " + string);
    }

    @Override
    protected void onUnregistered(final String registrationId) {
        SwrveLogger.i(TAG, "ADM Unregistered. RegistrationId: " + registrationId);
    }
}
