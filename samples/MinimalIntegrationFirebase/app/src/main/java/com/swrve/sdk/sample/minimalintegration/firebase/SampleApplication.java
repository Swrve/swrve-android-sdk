package com.swrve.sdk.sample.minimalintegration.firebase;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import com.swrve.sdk.SwrveNotificationConfig;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;

import java.util.ArrayList;
import java.util.List;

public class SampleApplication extends Application {

    private static final int YOUR_APP_ID = 0;
    private static final String YOUR_API_KEY = "YOUR_API_KEY";

    @Override
    public void onCreate() {
        super.onCreate();
        SwrveConfig config = new SwrveConfig();
        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel("123", "Swrve default channel", NotificationManager.IMPORTANCE_DEFAULT);
            if (getSystemService(Context.NOTIFICATION_SERVICE) != null) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }
        }
        List<String> notificationEvents = new ArrayList<>();
        notificationEvents.add("notification_permission_request");
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(R.drawable.logo, R.drawable.swrve_s_transparent, channel)
                .activityClass(MainActivity.class)
                .largeIconDrawableId(R.drawable.swrve_s_solid).accentColorHex("#3949AB")
                .pushNotificationPermissionEvents(notificationEvents);
        config.setNotificationConfig(notificationConfig.build());

        SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config);
    }
}
