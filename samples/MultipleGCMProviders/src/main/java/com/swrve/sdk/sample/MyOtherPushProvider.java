package com.swrve.sdk.sample;

import android.os.Bundle;
import com.google.android.gms.gcm.GcmListenerService;

/**
 * Fake 'other' push provider to show how to cope with multiple in your app
 */
public class MyOtherPushProvider extends GcmListenerService {

    @Override
    public void onMessageReceived(String from, Bundle data) {
        // This won't be executed for Swrve pushes if you enable
        // the CustomGcmReceiver
        super.onMessageReceived(from, data);
    }
}
