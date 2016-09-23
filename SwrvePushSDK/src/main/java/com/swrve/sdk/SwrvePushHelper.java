package com.swrve.sdk;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;

import java.util.Date;

public class SwrvePushHelper {

    public static boolean isSwrveRemoteNotification(final Bundle msg) {
        Object rawId = msg.get(SwrvePushConstants.SWRVE_TRACKING_KEY);
        String msgId = (rawId != null) ? rawId.toString() : null;
        return !SwrveHelper.isNullOrEmpty(msgId);
    }

    public static int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }

    public static NotificationCompat.Builder createNotificationBuilder(Context context, String msgText, Bundle msg) {
        SwrveNotification notificationHelper = SwrveNotification.getInstance(context);
        boolean materialDesignIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        int iconResource = (materialDesignIcon && notificationHelper.iconMaterialDrawableId >= 0) ? notificationHelper.iconMaterialDrawableId : notificationHelper.iconDrawableId;

        // Build notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(iconResource)
                .setTicker(msgText)
                .setContentTitle(notificationHelper.notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msgText))
                .setContentText(msgText)
                .setAutoCancel(true);

        if (notificationHelper.largeIconDrawable != null) {
            mBuilder.setLargeIcon(notificationHelper.largeIconDrawable);
        }

        if (notificationHelper.accentColor >= 0) {
            mBuilder.setColor(ContextCompat.getColor(context, notificationHelper.accentColor));
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

    public static Intent createPushEngagedIntent(Context context, Bundle msg) {
        Intent intent = new Intent(context, SwrvePushEngageReceiver.class);
        intent.putExtra(SwrvePushConstants.NOTIFICATION_BUNDLE, msg);
        return intent;
    }
}
