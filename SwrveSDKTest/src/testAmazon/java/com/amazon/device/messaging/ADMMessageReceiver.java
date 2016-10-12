package com.amazon.device.messaging;

import android.content.Context;
import android.content.Intent;

//Mocked out ADM class, the .jars Amazon provides are just stub implementations.
public class ADMMessageReceiver extends android.content.BroadcastReceiver {
    public ADMMessageReceiver(Class<?> clazz) {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    }
}
