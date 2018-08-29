package com.swrve.sdk.firebase;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.swrve.sdk.ISwrveBase;
import com.swrve.sdk.Swrve;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrvePushHelper;
import com.swrve.sdk.SwrvePushSDK;
import com.swrve.sdk.SwrvePushService;
import com.swrve.sdk.SwrveSDK;

/**
 * Used internally to process push notifications inside for your app.
 */
public class SwrveFirebaseMessagingService extends FirebaseMessagingService implements SwrvePushService {
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
    public void onNewToken(String token) {
        super.onNewToken(token);
        ISwrveBase sdk = SwrveSDK.getInstance();
        if (sdk != null && sdk instanceof Swrve) {
            ((Swrve) sdk).setRegistrationId(token);
        } else {
            SwrveLogger.e("Could not notify the SDK of a new token.");
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData() != null) {
            SwrveLogger.i("Received Firebase notification: %s" + remoteMessage.getData().toString());

            if (pushSDK != null) {
                // Convert from map to Bundle
                Bundle pushBundle = new Bundle();
                for (String key : remoteMessage.getData().keySet()) {
                    pushBundle.putString(key, remoteMessage.getData().get(key));
                }
                pushSDK.processRemoteNotification(pushBundle, false);
            }
        }
    }

    /**
     * Override this function to process notifications in a different way.
     *
     * @param msg
     */
    @Override
    public void processNotification(final Bundle msg) {
        pushSDK.processNotification(msg);
        SwrvePushHelper.qaUserPushNotification(msg);
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
     * @param notificationManager
     * @param notification
     * @return the notification id so that it can be dismissed by other UI elements.
     */
    @Override
    public int showNotification(NotificationManager notificationManager, Notification notification) {
        return pushSDK.showNotification(notificationManager, notification);
    }

    /**
     * Override this function to change the attributes of a notification.
     *
     * @param msgText
     * @param msg
     * @return the notification builder.
     */
    @Override
    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        return pushSDK.createNotificationBuilder(msgText, msg);
    }

    /**
     * Override this function to change the way the notifications are created.
     *
     * @param msg
     * @param contentIntent
     * @return the notification that will be displayed.
     */
    @Override
    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        return pushSDK.createNotification(msg, contentIntent);
    }

    /**
     * Override this function to change what the notification will do
     * once clicked by the user.
     * <p>
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
     * <p>
     * Note: sending the Bundle in an extra parameter
     * "notification" is essential so that the Swrve SDK
     * can be notified that the app was opened from the
     * notification.
     *
     * @param msg
     * @return the notification intent.
     */
    @Override
    public Intent createIntent(Bundle msg) {
        return pushSDK.createIntent(msg);
    }
}
