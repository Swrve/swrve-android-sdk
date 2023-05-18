package com.swrve.sdk.geo.sample;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;

import androidx.core.app.NotificationCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.multidex.MultiDexApplication;

import com.swrve.sdk.SwrveNotificationConfig;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.geo.SwrveGeoConfig;
import com.swrve.sdk.geo.SwrveGeoSDK;

public class SampleApplication extends MultiDexApplication {

    private static final int YOUR_APP_ID = 0;
    private static final String YOUR_API_KEY = "YOUR_API_KEY";
    protected static final int FOREGROUND_NOTIFICATION_ID = 345782346; // your unique notification id
    protected static final String CHANNEL_ID = "123";

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification defaults and SwrveSDK
        NotificationChannel channel = getNotificationChannel(this);
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(R.drawable.logo, R.drawable.swrve_s_transparent, channel)
                .activityClass(MainActivity.class)
                .largeIconDrawableId(R.drawable.swrve_s_solid)
                .accentColorHex("#20aaad");
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config);

        // Create SwrveGeoConfig
        SwrveGeoConfig geoConfig = new SwrveGeoConfig.Builder()
                .notificationFilter((builder, id, properties, notificationDetails, jsonPayload) -> {
                    return builder.build(); // add custom modifications if necessary or return null to suppress it
                })
                .geofenceTransitionListener((name, transition, triggeredLocation, customProperties) -> {
                    // add custom code to execute upon enter/exit a geofence. Note this executes in same BroadcastReceiver as Swrve code so has limitations.
                })
                .foregroundNotification(getGeoForegroundNotification(this).build(), FOREGROUND_NOTIFICATION_ID)
                .permissionPrePrompt(getString(R.string.pre_prompt_rationale),
                        getString(R.string.pre_prompt_proceed_button_text),
                        getString(R.string.pre_prompt_cancel_button_text))
                .permissionDeniedPostPrompt(getString(R.string.post_prompt_text),
                        getString(R.string.post_prompt_settings_button_text),
                        getString(R.string.post_prompt_cancel_button_text))
                .build();

        // Create SwrveGeoSDK instance using the SwrveGeoConfig
        SwrveGeoSDK.init(this, geoConfig);
    }

    public static NotificationCompat.Builder getGeoForegroundNotification(Context context) {
        NotificationChannel channel = getNotificationChannel(context);
        Bitmap largeIcon = ((BitmapDrawable) ResourcesCompat.getDrawable(context.getResources(), R.drawable.swrve_s_solid, null)).getBitmap();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        String content = "Default custom content that provides value.";
        builder.setSmallIcon(R.drawable.swrve_s_transparent)
                .setLargeIcon(largeIcon)
                .setContentTitle("Swrve Geo Sample")
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setColor(Color.parseColor("#20aaad"));
        return builder;
    }

    public static NotificationChannel getNotificationChannel(Context context) {
        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID, "your_custom_name", NotificationManager.IMPORTANCE_DEFAULT);
            if (context.getSystemService(Context.NOTIFICATION_SERVICE) != null) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }
        }
        return channel;
    }

}
