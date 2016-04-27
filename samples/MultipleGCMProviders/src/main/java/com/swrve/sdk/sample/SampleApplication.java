package com.swrve.sdk.sample;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.gcm.ISwrvePushNotificationListener;

public class SampleApplication extends Application {

    private static final String LOG_TAG = "SwrveSample";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            SwrveConfig config = new SwrveConfig();
            // Configure your Sender Id
            config.setSenderId(YOUR_SENDER_ID);
            SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config);
            // React to the push notification when the user clicks on it
            SwrveSDK.setPushNotificationListener(new ISwrvePushNotificationListener() {
                @Override
                public void onPushNotification(Bundle bundle) {
                    if (bundle.containsKey("custom_key")) {
                        String customValue = bundle.getString("custom_key");
                        // Do something awesome with custom value!
                        Log.d(LOG_TAG, "Received push payload " + customValue);
                    }
                }
            });
        } catch (Exception exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}
