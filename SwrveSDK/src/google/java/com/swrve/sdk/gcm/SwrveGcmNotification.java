package com.swrve.sdk.gcm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.swrve.sdk.SwrveHelper;

public class SwrveGcmNotification {

    private static final String TAG = "SwrveGcm";

    public static final String GCM_BUNDLE = "notification";

    private static final String SWRVE_PUSH_ICON_METADATA = "SWRVE_PUSH_ICON";
    private static final String SWRVE_PUSH_ACTIVITY_METADATA = "SWRVE_PUSH_ACTIVITY";
    private static final String SWRVE_PUSH_TITLE_METADATA = "SWRVE_PUSH_TITLE";

    private static SwrveGcmNotification instance;

    protected final Class<?> activityClass;
    protected final int iconDrawableId;
    protected final String notificationTitle;

    private SwrveGcmNotification(Class<?> activityClass, int iconDrawableId, String notificationTitle) {
        this.activityClass = activityClass;
        this.iconDrawableId = iconDrawableId;
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
            ApplicationInfo app = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = app.metaData;
            if (metaData == null) {
                throw new RuntimeException("No SWRVE metadata specified in AndroidManifest.xml");
            }

            int pushIconId = metaData.getInt(SWRVE_PUSH_ICON_METADATA, -1);
            if (pushIconId < 0) {
                // No icon specified in the metadata
                throw new RuntimeException("No " + SWRVE_PUSH_ICON_METADATA + " specified in AndroidManifest.xml");
            }

            Class<?> pushActivityClass = null;
            String pushActivity = metaData.getString(SWRVE_PUSH_ACTIVITY_METADATA);
            if (SwrveHelper.isNullOrEmpty(pushActivity)) {
                // No activity specified in the metadata
                throw new RuntimeException("No " + SWRVE_PUSH_ACTIVITY_METADATA + " specified in AndroidManifest.xml");
            } else {
                // Check that the Activity exists and can be found
                pushActivityClass = getClassForActivityClassName(context, pushActivity);
                if (pushActivityClass == null) {
                    throw new RuntimeException("The Activity with name " + pushActivity + " could not be found");
                }
            }

            String pushTitle = metaData.getString(SWRVE_PUSH_TITLE_METADATA);
            if (SwrveHelper.isNullOrEmpty(pushTitle)) {
                // No activity specified in the metadata
                throw new RuntimeException("No " + SWRVE_PUSH_TITLE_METADATA + " specified in AndroidManifest.xml");
            }

            swrveGcmNotification = new SwrveGcmNotification(pushActivityClass, pushIconId, pushTitle);

        } catch (Exception ex) {
            Log.e(TAG, "Error creating push notification from metadata", ex);
        }
        return swrveGcmNotification;
    }

    private static Class<?> getClassForActivityClassName(Context context, String className) throws ClassNotFoundException {
        if (!SwrveHelper.isNullOrEmpty(className)) {
            if (className.startsWith("")) {
                className = context.getPackageName() + className; // Append application package as it starts with .
            }
            return Class.forName(className);
        }
        return null;
    }

}