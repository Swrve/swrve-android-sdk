package com.swrve.sdk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.swrve.sdk.push.SwrvePushDeDuper;

import java.util.Date;

import static android.content.ContentValues.TAG;

public class SwrvePushSDK {

    protected static SwrvePushSDK instance;

    public static synchronized SwrvePushSDK createInstance(Context context) {
        if (instance == null) {
            instance = new SwrvePushSDK(context);
        }
        return instance;
    }

    private final Context context;
    private ISwrvePushNotificationListener pushNotificationListener;
    private SwrvePushService service;

    protected SwrvePushSDK(Context context) {
        this.context = context;
    }

    public static SwrvePushSDK getInstance() {
        return instance;
    }

    public ISwrvePushNotificationListener getPushNotificationListener() {
        return pushNotificationListener;
    }

    public void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener) {
        this.pushNotificationListener = pushNotificationListener;
    }

    public void setService(SwrvePushService service) {
        this.service = service;
    }

    public void processRemoteNotification(Bundle msg, boolean checkDupes) {
        if (!isSwrveRemoteNotification(msg)) {
            SwrveLogger.i(TAG, "Received Push: but not processing as it doesn't contain: " + SwrvePushConstants.SWRVE_TRACKING_KEY);
            return;
        }

        if(!(checkDupes && new SwrvePushDeDuper(context).isDupe(msg))) {
            service.processNotification(msg);
        }
    }

    public static String getSwrveId(final Bundle msg) {
        Object rawId = msg.get(SwrvePushConstants.SWRVE_TRACKING_KEY);
        return (rawId != null) ? rawId.toString() : null;
    }

    // Used by Unity
    public static boolean isSwrveRemoteNotification(final Bundle msg) {
        return !SwrveHelper.isNullOrEmpty(getSwrveId(msg));
    }

    public void processNotification(final Bundle msg) {
        if(!service.mustShowNotification()) {
            SwrveLogger.i(TAG, "Not processing as mustShowNotification is false.");
            return;
        }

        try {
            // Put the message into a notification and post it.
            final NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

            final PendingIntent contentIntent = service.createPendingIntent(msg);
            if (contentIntent == null) {
                SwrveLogger.e(TAG, "Error processing push notification. Unable to create intent");
                return;
            }

            final Notification notification = service.createNotification(msg, contentIntent);
            if (notification == null) {
                SwrveLogger.e(TAG, "Error processing push. Unable to create notification.");
                return;
            }

            // Time to show notification
            service.showNotification(mNotificationManager, notification);
        } catch (Exception ex) {
            SwrveLogger.e(TAG, "Error processing push.", ex);
        }
    }

    public int showNotification(NotificationManager notificationManager, Notification notification) {
        int id = generateTimestampId();
        notificationManager.notify(id, notification);
        return id;
    }

    private int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }

    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        String msgText = msg.getString("text");
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            NotificationCompat.Builder mBuilder = service.createNotificationBuilder(msgText, msg);
            mBuilder.setContentIntent(contentIntent);
            return mBuilder.build();
        }
        return null;
    }

    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        SwrvePushNotificationConfig notificationHelper = SwrvePushNotificationConfig.getInstance(context);
        return notificationHelper.createNotificationBuilder(context, msgText, msg);
    }

    public PendingIntent createPendingIntent(Bundle msg) {
        Intent intent = service.createIntent(msg);
        if (intent != null) {
            return PendingIntent.getBroadcast(context, generateTimestampId(), intent,PendingIntent.FLAG_CANCEL_CURRENT);
        }
        return null;
    }

    public Intent createIntent(Bundle msg) {
        Intent intent = new Intent(context, SwrvePushEngageReceiver.class);
        intent.putExtra(SwrvePushConstants.PUSH_BUNDLE, msg);
        return intent;
    }

}
