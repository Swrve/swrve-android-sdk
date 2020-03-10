package com.swrve.sdk.sample;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import com.swrve.sdk.SwrveNotificationConfig;
import com.swrve.sdk.SwrvePushNotificationListener;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONException;
import org.json.JSONObject;

public class SampleApplication extends Application {

    private static final String LOG_TAG = "SwrveSample";
    private int YOUR_APP_ID = -1;
    private String YOUR_API_KEY = "api_key";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            SwrveConfig config = new SwrveConfig();

            NotificationChannel channel = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                channel = new NotificationChannel("123", "Swrve default channel", NotificationManager.IMPORTANCE_DEFAULT);
                if (getSystemService(Context.NOTIFICATION_SERVICE) != null) {
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.createNotificationChannel(channel);
                }
            }
            SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(R.drawable.logo, R.drawable.swrve_s_transparent, channel)
                    .activityClass(MainActivity.class)
                    .largeIconDrawableId(R.drawable.swrve_s_solid)
                    .accentColorHex("#3949AB"); // Darkblue
            config.setNotificationConfig(notificationConfig.build());

            // React to the push notification when the user clicks on it
            config.setNotificationListener(payload -> {
                try {
                    if (payload.has("custom_key")) {
                        String customValue = payload.getString("custom_key");
                        // Do something awesome with custom value!
                        Log.d(LOG_TAG, "Received push payload " + customValue);
                    }
                } catch(JSONException exp) {
                    exp.printStackTrace();
                }
            });

            SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config);

        } catch (Exception exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}
