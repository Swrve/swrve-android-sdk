package com.swrve.sdk.demo;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.gcm.ISwrvePushNotificationListener;

public class DemoApplication extends Application {

    private static final String LOG_TAG = "SwrveDemo";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            SwrveConfig config = new SwrveConfig();
            // Comment the following line if you do not want to do GCM push notifications
            config.setSenderId(YOUR_SENDER_ID);

            SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config);

            // Comment the following lines if you do not want to do GCM push notifications
            // React to the push notification when the user clicks on it
            SwrveSDK.setPushNotificationListener(new ISwrvePushNotificationListener() {
                @Override
                public void onPushNotification(Bundle bundle) {
                    if (bundle.containsKey("custom_key")) {
                        String customValue = bundle.getString("custom_key");
                        // Do something awesome with custom value!
                        Log.d("DemoApplication", "Received push payload " + customValue);
                    }
                }
            });
        } catch (Exception exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}
