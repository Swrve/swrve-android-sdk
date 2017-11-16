package com.swrve.sdk.sample;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.SwrvePushNotificationListener;

import org.json.JSONException;
import org.json.JSONObject;

public class SampleApplication extends Application {

    private static final String LOG_TAG = "SwrveSample";
    private int YOUR_APP_ID = -1;
    private String YOUR_API_KEY = "api_key";
    private String YOUR_SENDER_ID = "sender_id";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            SwrveConfig config = new SwrveConfig();
            // Configure your default Notification Channel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("default", "Default", NotificationManager.IMPORTANCE_DEFAULT);
                config.setDefaultNotificationChannel(channel);
            }
            // Configure your Sender Id
            config.setSenderId(YOUR_SENDER_ID);
            SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config);
            // React to the push notification when the user clicks on it
            SwrveSDK.setPushNotificationListener(new SwrvePushNotificationListener() {
                @Override
                public void onPushNotification(JSONObject payload) {
                    try {
                        if (payload.has("custom_key")) {
                            String customValue = payload.getString("custom_key");
                            // Do something awesome with custom value!
                            Log.d(LOG_TAG, "Received push payload " + customValue);
                        }
                    } catch(JSONException exp) {
                        exp.printStackTrace();
                    }
                }
            });
        } catch (Exception exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}
