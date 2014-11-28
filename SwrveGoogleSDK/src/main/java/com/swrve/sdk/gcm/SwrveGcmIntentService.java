package com.swrve.sdk.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

/**
 * Used internally to process push notifications inside for your app.
 */
public class SwrveGcmIntentService extends IntentService {

    public SwrveGcmIntentService() {
        super("SwrveGcmIntentService");
    }

    private SwrveGcmHandler handler;

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            handler = new SwrveGcmHandler(this);
            handler.onHandleIntent(intent);
        } finally {
            SwrveGcmBroadcastReceiver.completeWakefulIntent(intent); // Always release the wake lock provided by the WakefulBroadcastReceiver.
        }
    }

    /**
     * Override this function to process notifications in a different way.
     *
     * @param msg
     */
    public void processNotification(final Bundle msg) {
        handler.processNotification(msg);
    }

    /**
     * Override this function to decide when to show a notification.
     *
     * @return true when you want to display notifications
     */
    public boolean mustShowNotification() {
        return handler.mustShowNotification();
    }

    /**
     * Override this function to change the way a notification is shown.
     *
     * @param notificationManager
     * @param notification
     * @return the notification id so that it can be dismissed by other UI elements
     */
    public int showNotification(NotificationManager notificationManager, Notification notification) {
        return handler.showNotification(notificationManager, notification);
    }

    /**
     * Override this function to change the attributes of a notification.
     *
     * @param msgText
     * @param msg
     * @return
     */
    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        return handler.createNotificationBuilder(msgText, msg);
    }

    /**
     * Override this function to change the way the notifications are created.
     *
     * @param msg
     * @param contentIntent
     * @return
     */
    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        return handler.createNotification(msg, contentIntent);
    }

    /**
     * Override this function to change what the notification will do
     * once clicked by the user.
     *
     * Note: sending the Bundle in an extra parameter
     * "notification" is essential so that the Swrve SDK
     * can be notified that the app was opened from the
     * notification.
     *
     * @param msg
     * @return
     */
    public PendingIntent createPendingIntent(Bundle msg) {
        return handler.createPendingIntent(msg);
    }

    /**
     * Override this function to change what the notification will do
     * once clicked by the user.
     *
     * Note: sending the Bundle in an extra parameter
     * "notification" is essential so that the Swrve SDK
     * can be notified that the app was opened from the
     * notification.
     *
     * @param msg
     * @return
     */
    public Intent createIntent(Bundle msg) {
        return handler.createIntent(msg);
    }
}
