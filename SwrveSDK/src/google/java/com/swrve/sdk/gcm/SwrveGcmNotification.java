package com.swrve.sdk.gcm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import com.swrve.sdk.SwrveLogger;

import com.swrve.sdk.SwrveHelper;

public class SwrveGcmNotification {

    private static final String TAG = "SwrveGcm";

    private static final String SWRVE_PUSH_ICON_METADATA = "SWRVE_PUSH_ICON";
    private static final String SWRVE_PUSH_ICON_MATERIAL_METADATA = "SWRVE_PUSH_ICON_MATERIAL";
    private static final String SWRVE_PUSH_ICON_LARGE_METADATA = "SWRVE_PUSH_ICON_LARGE";
    private static final String SWRVE_PUSH_ACCENT_COLOR_METADATA = "SWRVE_PUSH_ACCENT_COLOR";
    private static final String SWRVE_PUSH_ACTIVITY_METADATA = "SWRVE_PUSH_ACTIVITY";
    private static final String SWRVE_PUSH_TITLE_METADATA = "SWRVE_PUSH_TITLE";

    private static SwrveGcmNotification instance;

    protected final Class<?> activityClass;
    protected final int iconDrawableId;
    protected final int iconMaterialDrawableId;
    protected final Bitmap largeIconDrawable;
    protected final int accentColor;
    protected final String notificationTitle;

    private SwrveGcmNotification(Class<?> activityClass, int iconDrawableId, int iconMaterialDrawableId, Bitmap largeIconDrawable, int accentColor, String notificationTitle) {
        this.activityClass = activityClass;
        this.iconDrawableId = iconDrawableId;
        this.iconMaterialDrawableId = iconMaterialDrawableId;
        this.largeIconDrawable = largeIconDrawable;
        this.accentColor = accentColor;
        this.notificationTitle = notificationTitle;
    }

    protected static SwrveGcmNotification getInstance(Context context) {
        if (instance == null) {
            instance = createNotificationFromMetaData(context);
        }
        return instance;
    }

    protected static SwrveGcmNotification createNotificationFromMetaData(Context context) {
        SwrveGcmNotification swrveGcmNotification = null;
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

            int accentColor = metaData.getInt(SWRVE_PUSH_ACCENT_COLOR_METADATA, -1);
            if (accentColor < 0) {
                // No accent color specified in the metadata
                SwrveLogger.w(TAG, "No " + SWRVE_PUSH_ACCENT_COLOR_METADATA + " specified. We recommend setting an accent color for your notifications");
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

            swrveGcmNotification = new SwrveGcmNotification(pushActivityClass, iconId, iconMaterialId, largeIconBitmap, accentColor, pushTitle);
        } catch (Exception ex) {
            SwrveLogger.e(TAG, "Error creating push notification from metadata", ex);
        }
        return swrveGcmNotification;
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

}