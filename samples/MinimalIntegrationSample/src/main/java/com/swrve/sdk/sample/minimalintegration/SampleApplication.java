package com.swrve.sdk.sample.minimalintegration;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.swrve.sdk.SwrveNotificationConfig;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;

public class SampleApplication extends Application {

    private static final int YOUR_APP_ID = 0;
    private static final String YOUR_API_KEY = "YOUR_API_KEY";

    @Override
    public void onCreate() {
        super.onCreate();
        SwrveConfig config = new SwrveConfig();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default", "Default", NotificationManager.IMPORTANCE_DEFAULT);
            config.setDefaultNotificationChannel(channel);
        }

        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder()
                .activityClass(MainActivity.class)
                .iconDrawableId(R.drawable.logo)
                .iconMaterialDrawableId(R.drawable.swrve_s_transparent)
                .largeIconDrawableId(R.drawable.swrve_s_solid)
                .notificationTitle(getString(R.string.app_name))
                .accentColorResourceId(R.color.dark_blue);
        config.setNotificationConfig(notificationConfig.build());

        SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config);
    }
}
