package com.swrve.sdk.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GcmListenerService;
import com.swrve.sdk.SwrvePushHelper;
import com.swrve.sdk.SwrvePushSDK;
import com.swrve.sdk.SwrvePushService;

/**
 * Used internally to process push notifications inside for your app.
 */
public class SwrveGcmIntentService extends GcmListenerService implements SwrvePushService {

    private SwrvePushSDK pushSDK;

    @Override
    public void onCreate() {
        super.onCreate();
        pushSDK = SwrvePushSDK.getInstance();
        if (pushSDK != null) {
            pushSDK.setService(this);
        }
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        if (pushSDK != null) {
            pushSDK.processRemoteNotification(data, false);
        }
    }

    /**
     * Override this function to process notifications in a different way.
     *
     * @param msg The bundle from push provider
     */
    @Override
    public void processNotification(final Bundle msg) {
        if (pushSDK != null) {
            pushSDK.processNotification(msg);
            SwrvePushHelper.qaUserPushNotification(msg);
        }
    }

    /**
     * Override this function to decide when to show a notification.
     *
     * @return true when you want to display notifications.
     */
    @Override
    public boolean mustShowNotification() {
        return true;
    }

    /**
     * Override this function to change the way a notification is shown.
     *
     * @param notificationManager Android NotificationManager
     * @param notification Notification that is being rendered.
     * @return the notification id so that it can be dismissed by other UI elements.
     */
    @Override
    public int showNotification(NotificationManager notificationManager, Notification notification) {
        return pushSDK.showNotification(notificationManager, notification);
    }

    /**
     * Override this function to change the attributes of a notification.
     *
     * @param msgText The message text to display
     * @param msg The original message bundle to push provider
     * @return the notification builder.
     */
    @Override
    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        return pushSDK.createNotificationBuilder(msgText, msg);
    }

    /**
     * Override this function to change the way the notifications are created.
     *
     * @param msg The message bundle from server
     * @param contentIntent The pendingintent to trigger when notification is tapped.
     * @return the notification that will be displayed.
     */
    @Override
    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        return pushSDK.createNotification(msg, contentIntent);
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
     * @param msg push message payload
     * @return pending intent.
     */
    @Override
    public PendingIntent createPendingIntent(Bundle msg) {
        return pushSDK.createPendingIntent(msg);
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
     * @param msg The original message bundle.
     * @return the notification intent.
     */
    @Override
    public Intent createIntent(Bundle msg) {
        return pushSDK.createIntent(msg);
    }
}
