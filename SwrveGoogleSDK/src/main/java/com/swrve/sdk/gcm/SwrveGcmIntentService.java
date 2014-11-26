package com.swrve.sdk.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveInstance;
import com.swrve.sdk.qa.SwrveQAUser;

import java.util.Date;
import java.util.Iterator;

/**
 * Used internally to process push notifications inside for your app.
 */
public class SwrveGcmIntentService extends IntentService {
    protected static final String TAG = "SwrveGcmIntentService";

    private SwrveGcmNotification swrveGcmNotification;

    public SwrveGcmIntentService() {
        super("SwrveGcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            swrveGcmNotification = SwrveGcmNotification.getInstance(this);
            if (swrveGcmNotification != null) {
                if (intent.hasExtra(SwrveGcmNotification.GCM_BUNDLE)) {
                    processGCMEngaged(swrveGcmNotification.activityClass, intent);
                } else {
                    processInitialGCM(intent);
                }
            }
        } finally {
            SwrveGcmBroadcastReceiver.completeWakefulIntent(intent); // Always release the wake lock provided by the WakefulBroadcastReceiver.
        }
    }

    private void processGCMEngaged(Class<?> activityClass, Intent intent) {
        Bundle extras = intent.getExtras();
        if (!extras.isEmpty() && !intent.getBundleExtra(SwrveGcmNotification.GCM_BUNDLE).isEmpty()) {  // has effect of un-parcelling Bundle
            Log.d(TAG, "Starting activity " + activityClass.toString() + " with:" + intent.getBundleExtra(SwrveGcmNotification.GCM_BUNDLE));
        }
        Intent activityIntent = new Intent(this, activityClass);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityIntent.putExtras(intent);
        startActivity(activityIntent);

        SwrveInstance.getInstance().processIntent(intent); // try generating the engaged event now.
    }

    private void processInitialGCM(Intent intent)
    {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);
        if (!extras.isEmpty()) {  // has effect of un-parcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                Log.e(TAG, "Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                Log.e(TAG, "Deleted messages on server: " + extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                Log.i(TAG, "Received GCM notification: " + extras.toString());
                // Process notification.
                processRemoteNotification(extras);
            }
        }
    }

    private void processRemoteNotification(Bundle msg) {
        // Notify binded clients
        Iterator<SwrveQAUser> iter = SwrveQAUser.getBindedListeners().iterator();
        while (iter.hasNext()) {
            SwrveQAUser sdkListener = iter.next();
            sdkListener.pushNotification(msg);
        }

        // Process notification
        processNotification(msg);
    }

    /**
     * Override this function to process notifications in a different way.
     *
     * @param msg
     */
    public void processNotification(final Bundle msg) {
        if (mustShowNotification()) {
            try {
                // Put the message into a notification and post it.
                final NotificationManager mNotificationManager = (NotificationManager)
                        this.getSystemService(Context.NOTIFICATION_SERVICE);

                final PendingIntent contentIntent = createPendingIntent(msg);
                if (contentIntent != null) {
                    final Notification notification = createNotification(msg, contentIntent);
                    if (notification != null) {
                        showNotification(mNotificationManager, notification);
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error processing push notification", ex);
            }
        }
    }

    /**
     * Override this function to decide when to show a notification.
     *
     * @return true when you want to display notifications
     */
    public boolean mustShowNotification() {
        return true;
    }

    /**
     * Override this function to change the way a notification is shown.
     *
     * @param notificationManager
     * @param notification
     * @return the notification id so that it can be dismissed by other UI elements
     */
    public int showNotification(NotificationManager notificationManager, Notification notification) {
        int notificationId = (int)new Date().getTime();
        notificationManager.notify(notificationId, notification);
        return notificationId;
    }

    /**
     * Override this function to change the attributes of a notification.
     *
     * @param msgText
     * @param msg
     * @return
     */
    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        String msgSound = msg.getString("sound");

        // Build notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(swrveGcmNotification.iconDrawableId)
                .setContentTitle(swrveGcmNotification.notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(msgText))
                .setContentText(msgText)
                .setAutoCancel(true);

        if (!SwrveHelper.isNullOrEmpty(msgSound)) {
            Uri soundUri;
            if (msgSound.equalsIgnoreCase("default")) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            } else {
                String packageName = getApplicationContext().getPackageName();
                soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                        + "://" + packageName + "/raw/" + msgSound);
            }
            mBuilder.setSound(soundUri);
        }
        return mBuilder;
    }

    /**
     * Override this function to change the way the notifications are created.
     *
     * @param msg
     * @param contentIntent
     * @return
     */
    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        String msgText = msg.getString("text");
        // Log.d(TAG, "Notification:" + msgText + " _p=" + msg.getString("_p"));
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            // Build notification
            NotificationCompat.Builder mBuilder = createNotificationBuilder(msgText, msg);
            mBuilder.setContentIntent(contentIntent);
            return mBuilder.build();
        }
        return null;
    }

    /**
     * Override this function to change what the notification will do
     * once clicked by the user.
     * <p/>
     * Note: sending the Bundle in an extra parameter
     * "notification" is essential so that the Swrve SDK
     * can be notified that the app was opened from the
     * notification.
     *
     * @param msg
     * @return
     */
    public PendingIntent createPendingIntent(Bundle msg) {
        // Add notification to bundle
        Intent intent = createIntent(msg);
        if (intent != null) {
            return PendingIntent.getBroadcast(this, (int)new Date().getTime(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return null;
    }

    /**
     * Override this function to change what the notification will do
     * once clicked by the user.
     * <p/>
     * Note: sending the Bundle in an extra parameter
     * "notification" is essential so that the Swrve SDK
     * can be notified that the app was opened from the
     * notification.
     *
     * @param msg
     * @return
     */
    public Intent createIntent(Bundle msg) {
        Intent intent = null;
        if (swrveGcmNotification.activityClass != null) {
            intent = new Intent(this, SwrveGcmBroadcastReceiver.class);
            intent.putExtra(SwrveGcmNotification.GCM_BUNDLE, msg);
            intent.setAction("openActivity");
        }
        return intent;
    }
}
