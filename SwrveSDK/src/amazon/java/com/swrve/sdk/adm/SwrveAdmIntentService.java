package com.swrve.sdk.adm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.swrve.sdk.ISwrveBase;
import com.swrve.sdk.Swrve;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrvePushSDK;
import com.swrve.sdk.SwrvePushService;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.qa.SwrveQAUser;
import com.swrve.sdk.SwrveHelper;

import java.util.Iterator;

public class SwrveAdmIntentService extends ADMMessageHandlerBase implements SwrvePushService {
    private final static String TAG = "SwrvePush";

    private SwrvePushSDK pushSDK;

    public SwrveAdmIntentService() {
        super(SwrveAdmIntentService.class.getName());
    }

    public SwrveAdmIntentService(final String className) {
        super(className);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        pushSDK = SwrvePushSDK.getInstance();
        pushSDK.setService(this);
    }

    @Override
    protected void onMessage(final Intent intent) {
        if (!SwrveHelper.sdkAvailable()) {
            return;
        }

        if (intent == null) {
            SwrveLogger.e(TAG, "ADM messaging runtimes have called onMessage() with unexpected null intent.");
            return;
        }

        final Bundle extras = intent.getExtras();
        if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
            SwrveLogger.i(TAG, "Received ADM notification: " + extras.toString());
            pushSDK.processRemoteNotification(extras, true);
        }
    }

    @Override
    protected void onRegistrationError(final String string) {
        //This is considered fatal for ADM
        SwrveLogger.e(TAG, "ADM Registration Error. Error string: " + string);
    }

    @Override
    protected void onRegistered(final String registrationId) {
        SwrveLogger.i(TAG, "ADM Registered. RegistrationId: " + registrationId);
        ISwrveBase sdk = SwrveSDK.getInstance();
        if (sdk != null && sdk instanceof Swrve) {
            ((Swrve) sdk).onRegistrationIdReceived(registrationId);
        } else {
            SwrveLogger.e(TAG, "Could not notify the SDK of a new token. Consider using the shared instance.");
        }
    }

    @Override
    protected void onUnregistered(final String registrationId) {
        SwrveLogger.i(TAG, "ADM Unregistered. RegistrationId: " + registrationId);
    }

    /**
     * Override this function to process notifications in a different way.
     *
     * @param msg
     */
    @Override
    public void processNotification(final Bundle msg) {
        pushSDK.processNotification(msg);

        // Notify bound clients
        Iterator<SwrveQAUser> iter = SwrveQAUser.getBindedListeners().iterator();
        while (iter.hasNext()) {
            SwrveQAUser sdkListener = iter.next();
            sdkListener.pushNotification(pushSDK.getSwrveId(msg), msg);
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
     * @param msg
     * @return the notification intent.
     */
    @Override
    public Intent createIntent(Bundle msg) {
        return pushSDK.createIntent(msg);
    }

}

