package com.amazon.device.messaging;

import android.app.IntentService;
import android.content.Intent;

public class ADMMessageHandlerBase extends IntentService {

    public ADMMessageHandlerBase(String className) {
        super(className);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    protected void onMessage(final Intent intent) {

    }

    protected void onRegistrationError(final String string) {

    }

    protected void onRegistered(final String registrationId) {
    }

    protected void onUnregistered(final String registrationId) {

    }
}
