package com.swrve.sdk.geo.sample;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.multidex.MultiDexApplication;

import com.swrve.sdk.SwrveNotificationConfig;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.geo.SwrveGeoConfig;
import com.swrve.sdk.geo.SwrveGeoSDK;

public class SampleApplication extends MultiDexApplication {

    private static final int YOUR_APP_ID = 0;
    private static final String YOUR_API_KEY = "YOUR_API_KEY";

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification defaults and SwrveSDK
        NotificationChannel channel = getNotificationChannel();
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(R.drawable.logo, R.drawable.swrve_s_transparent, channel)
                .activityClass(MainActivity.class)
                .largeIconDrawableId(R.drawable.swrve_s_solid)
                .accentColorHex("#20aaad");
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config);

        // Create SwrveGeoConfig and SwrveGeoSDK
        SwrveGeoConfig geoConfig = new SwrveGeoConfig.Builder()
                .permissionPrePrompt(getString(R.string.pre_prompt_rationale), // Note that the pre_prompt_rationale text depends on OS version. See res/values, res/values-v29, res/values-v30, etc
                        getString(R.string.pre_prompt_proceed_button_text),
                        getString(R.string.pre_prompt_cancel_button_text))
                .permissionDeniedPostPrompt(getString(R.string.post_prompt_text),
                        getString(R.string.post_prompt_settings_button_text),
                        getString(R.string.post_prompt_cancel_button_text))
                .build();
        SwrveGeoSDK.init(this, geoConfig);
    }

    public NotificationChannel getNotificationChannel() {
        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel("default", "your_custom_name", NotificationManager.IMPORTANCE_DEFAULT);
            if (getSystemService(Context.NOTIFICATION_SERVICE) != null) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }
        }
        return channel;
    }
}
