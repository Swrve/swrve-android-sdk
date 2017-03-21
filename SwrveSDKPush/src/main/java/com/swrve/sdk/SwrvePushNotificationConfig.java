package com.swrve.sdk;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

/**
 * Used internally to build a local notification from a push notification.
 */
public class SwrvePushNotificationConfig {

    private static final String TAG = "SwrvePush";

    private static final String SWRVE_PUSH_ICON_METADATA = "SWRVE_PUSH_ICON";
    private static final String SWRVE_PUSH_ICON_MATERIAL_METADATA = "SWRVE_PUSH_ICON_MATERIAL";
    private static final String SWRVE_PUSH_ICON_LARGE_METADATA = "SWRVE_PUSH_ICON_LARGE";
    private static final String SWRVE_PUSH_ACCENT_COLOR_METADATA = "SWRVE_PUSH_ACCENT_COLOR";
    private static final String SWRVE_PUSH_ACTIVITY_METADATA = "SWRVE_PUSH_ACTIVITY";
    private static final String SWRVE_PUSH_TITLE_METADATA = "SWRVE_PUSH_TITLE";

    private static SwrvePushNotificationConfig instance;

    private final Class<?> activityClass;
    private final int iconDrawableId;
    private final int iconMaterialDrawableId;
    private final Bitmap largeIconDrawable;
    private final Integer accentColor;
    private final String notificationTitle;

    // Called by Unity Swrve SDK
    public SwrvePushNotificationConfig(Class<?> activityClass, int iconDrawableId, int iconMaterialDrawableId, Bitmap largeIconDrawable, Integer accentColor, String notificationTitle) {
        this.activityClass = activityClass;
        this.iconDrawableId = iconDrawableId;
        this.iconMaterialDrawableId = iconMaterialDrawableId;
        this.largeIconDrawable = largeIconDrawable;
        this.accentColor = accentColor;
        this.notificationTitle = notificationTitle;
    }

    // Called by Swrve SDK Native
    public static SwrvePushNotificationConfig getInstance(Context context) {
        if (instance == null) {
            instance = createNotificationFromMetaData(context);
        }
        return instance;
    }

    private static SwrvePushNotificationConfig createNotificationFromMetaData(Context context) {
        SwrvePushNotificationConfig swrvePushNotificationConfig = null;
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo app = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = app.metaData;
            if (metaData == null) {
                throw new RuntimeException("No SWRVE metadata specified in AndroidManifest.xml");
            }

            int iconId = metaData.getInt(SWRVE_PUSH_ICON_METADATA, -1);
            if (iconId < 0) {
                // Default to the application icon
                iconId = app.icon;
            }

            int iconMaterialId = metaData.getInt(SWRVE_PUSH_ICON_MATERIAL_METADATA, -1);
            if (iconMaterialId < 0) {
                // No material (Android L+) icon specified in the metadata
                SwrveLogger.w(TAG, "No " + SWRVE_PUSH_ICON_MATERIAL_METADATA + " specified. We recommend setting a special material icon for Android L+");
            }

            Bitmap largeIconBitmap = null;
            int largeIconBitmapId = metaData.getInt(SWRVE_PUSH_ICON_LARGE_METADATA, -1);
            if (largeIconBitmapId < 0) {
                // No large icon specified in the metadata
                SwrveLogger.w(TAG, "No " + SWRVE_PUSH_ICON_LARGE_METADATA + " specified. We recommend setting a large image for your notifications");
            } else {
                largeIconBitmap = BitmapFactory.decodeResource(context.getResources(), largeIconBitmapId);
            }

            int accentColorResourceId = metaData.getInt(SWRVE_PUSH_ACCENT_COLOR_METADATA, -1);
            Integer accentColor = null;
            if (accentColorResourceId < 0) {
                // No accent color specified in the metadata
                SwrveLogger.w(TAG, "No " + SWRVE_PUSH_ACCENT_COLOR_METADATA + " specified. We recommend setting an accent color for your notifications");
            } else {
                accentColor = ContextCompat.getColor(context, accentColorResourceId);
            }

            Class<?> pushActivityClass = null;
            String pushActivity = metaData.getString(SWRVE_PUSH_ACTIVITY_METADATA);
            if (SwrveHelper.isNullOrEmpty(pushActivity)) {
                ResolveInfo resolveInfo = packageManager.resolveActivity(
                        packageManager.getLaunchIntentForPackage(context.getPackageName()),
                        PackageManager.MATCH_DEFAULT_ONLY);

                if (resolveInfo != null) {
                    pushActivity = resolveInfo.activityInfo.name;
                } else {
                    // No activity specified in the metadata
                    throw new RuntimeException("No " + SWRVE_PUSH_ACTIVITY_METADATA + " specified in AndroidManifest.xml");
                }
            }

            if (pushActivity != null) {
                // Check that the Activity exists and can be found
                pushActivityClass = getClassForActivityClassName(context, pushActivity);
                if (pushActivityClass == null) {
                    throw new RuntimeException("The Activity with name " + pushActivity + " could not be found");
                }
            }

            String pushTitle = metaData.getString(SWRVE_PUSH_TITLE_METADATA);
            if (SwrveHelper.isNullOrEmpty(pushTitle)) {
                CharSequence appTitle = app.loadLabel(packageManager);
                if (appTitle != null) {
                    pushTitle = appTitle.toString();
                }
                if (SwrveHelper.isNullOrEmpty(pushTitle)) {
                    // No title specified in the metadata and could not find default
                    throw new RuntimeException("No " + SWRVE_PUSH_TITLE_METADATA + " specified in AndroidManifest.xml");
                }
            }

            swrvePushNotificationConfig = new SwrvePushNotificationConfig(pushActivityClass, iconId, iconMaterialId, largeIconBitmap, accentColor, pushTitle);
        } catch (Exception ex) {
            SwrveLogger.e(TAG, "Error creating push notification from metadata", ex);
        }
        return swrvePushNotificationConfig;
    }

    private static Class<?> getClassForActivityClassName(Context context, String className) throws ClassNotFoundException {
        if (!SwrveHelper.isNullOrEmpty(className)) {
            if (className.startsWith(".")) {
                className = context.getPackageName() + className; // Append application package as it starts with .
            }
            return Class.forName(className);
        }
        return null;
    }

    Class<?> getActivityClass() {
        return activityClass;
    }

    public NotificationCompat.Builder createNotificationBuilder(Context context, String msgText, Bundle msg) {
        boolean materialDesignIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        int iconResource = (materialDesignIcon && iconMaterialDrawableId >= 0) ? iconMaterialDrawableId : iconDrawableId;

        // Build notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(iconResource)
                .setTicker(msgText)
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msgText))
                .setContentText(msgText)
                .setAutoCancel(true);

        if (largeIconDrawable != null) {
            mBuilder.setLargeIcon(largeIconDrawable);
        }

        if (accentColor >= 0) {
            mBuilder.setColor(accentColor);
        }

        String msgSound = msg.getString("sound");
        if (!SwrveHelper.isNullOrEmpty(msgSound)) {
            Uri soundUri;
            if (msgSound.equalsIgnoreCase("default")) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            } else {
                String packageName = context.getApplicationContext().getPackageName();
                soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/raw/" + msgSound);
            }
            mBuilder.setSound(soundUri);
        }
        return mBuilder;
    }
}
