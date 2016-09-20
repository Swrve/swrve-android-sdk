package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;

import java.util.Date;

public class SwrvePushSDK implements ISwrvePushSDK {
    private final static String TAG = "SwrvePush";
    private static SwrvePushSDK instance;

    public SwrvePushSDK() {
    }

    @Override
    public String initialisePushSDK(Context context) {
        return SwrvePushSDKImp.getInstance().initialiseNotificationSDK(context);
    }

    @Override
    public void setPushSDKListener(ISwrvePushSDKListener listener) {
        SwrvePushSDKImp.getInstance().setPushSDKListener(listener);
    }

    @Override
    public void showNotification(Context context, Bundle msg) {
        boolean mustShowNotification = mustShowNotification();
        if (mustShowNotification) {
            try {
                // Put the message into a notification and post it.
                final NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

                final PendingIntent contentIntent = createPendingIntent(context, msg);
                if (contentIntent != null) {
                    final Notification notification = createNotification(context, msg, contentIntent);
                    if (notification != null) {
                        showNotification(mNotificationManager, notification);
                    }
                }
            } catch (Exception ex) {
                SwrveLogger.e(TAG, "Error processing ADM push notification", ex);
            }
        } else {
            SwrveLogger.i(TAG, "ADM notification: not processing as mustShowNotification is false.");
        }
    }

    public boolean mustShowNotification() {
        return true;
    }

    private int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }

    public int showNotification(NotificationManager notificationManager, Notification notification) {
        int id = generateTimestampId();
        notificationManager.notify(id, notification);
        return id;
    }

    public Notification createNotification(Context context, Bundle msg, PendingIntent contentIntent) {
        String msgText = msg.getString("text");
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            // Build notification
            NotificationCompat.Builder mBuilder = createNotificationBuilder(context, msgText, msg);
            mBuilder.setContentIntent(contentIntent);
            return mBuilder.build();
        }
        return null;
    }

    public NotificationCompat.Builder createNotificationBuilder(Context context, String msgText, Bundle msg) {
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

    public PendingIntent createPendingIntent(Context context, Bundle msg) {
        Intent intent = createIntent(context, msg);
        if (intent != null) {
            return PendingIntent.getBroadcast(context, generateTimestampId(), intent,PendingIntent.FLAG_CANCEL_CURRENT);
        }
        return null;
    }

    public Intent createIntent(Context context, Bundle msg) {
        Intent intent = new Intent(context, SwrveNotificationEngageReceiver.class);
        intent.putExtra(SwrvePushSDKConstants.NOTIFICATION_BUNDLE, msg);
        return intent;
    }

    public static ISwrvePushSDK getInstance() throws RuntimeException {
        if (instance == null) {
            instance = new SwrvePushSDK();
        }
        return instance;
    }

}
