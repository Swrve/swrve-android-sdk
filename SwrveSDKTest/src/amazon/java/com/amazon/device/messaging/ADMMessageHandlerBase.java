package com.amazon.device.messaging;

//import android.app.IntentService;
import android.app.IntentService;
import android.content.Intent;

//Mocked out ADM class, the .jars Amazon provides are just stub implementations.
public abstract class ADMMessageHandlerBase extends IntentService {
    public ADMMessageHandlerBase(String var1) {
        super(var1);
    }

    protected final void onHandleIntent(Intent var1) {
    }

    protected abstract void onMessage(Intent var1);

    protected abstract void onRegistrationError(String var1);

    protected abstract void onRegistered(String var1);

    protected abstract void onUnregistered(String var1);
}
