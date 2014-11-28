package com.swrve.sdk.gcm;

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
import com.swrve.sdk.qa.SwrveQAUser;

import java.util.Iterator;

public class SwrveGcmHandler {

    protected static final String TAG = "SwrveGcmIntentService";
    private static int tempNotificationId = 1;

    private Context context;

    protected SwrveGcmHandler (Context context) {
        this.context = context;
    }

    protected boolean onHandleIntent(Intent intent) {
        boolean gcmHandled = false;
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
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
                // Process notification.
                processRemoteNotification(extras);
                Log.i(TAG, "Received notification: " + extras.toString());
                gcmHandled = true;
            }
        }
        return gcmHandled;
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

    protected void processNotification(final Bundle msg) {
        if (mustShowNotification()) {
            try {
                // Put the message into a notification and post it.
                final NotificationManager mNotificationManager = (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);

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

    protected boolean mustShowNotification() {
        return true;
    }

    protected int showNotification(NotificationManager notificationManager, Notification notification) {
        int notificationId = tempNotificationId++;
        notificationManager.notify(notificationId, notification);
        return notificationId;
    }

    protected NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        String msgSound = msg.getString("sound");

        // Build notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(SwrveGcmNotification.getInstance(context).iconDrawableId)
                .setContentTitle(SwrveGcmNotification.getInstance(context).notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msgText))
                .setContentText(msgText)
                .setAutoCancel(true);

        if (!SwrveHelper.isNullOrEmpty(msgSound)) {
            Uri soundUri;
            if (msgSound.equalsIgnoreCase("default")) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            } else {
                String packageName = context.getApplicationContext().getPackageName();
                soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                        + "://" + packageName + "/raw/" + msgSound);
            }
            mBuilder.setSound(soundUri);
        }
        return mBuilder;
    }

    protected Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        String msgText = msg.getString("text");
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            // Build notification
            NotificationCompat.Builder mBuilder = createNotificationBuilder(msgText, msg);
            mBuilder.setContentIntent(contentIntent);
            return mBuilder.build();
        }

        return null;
    }

    protected PendingIntent createPendingIntent(Bundle msg) {
        // Add notification to bundle
        Intent intent = createIntent( msg);
        if (intent != null) {
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return null;
    }

    protected Intent createIntent(Bundle msg) {
        Intent intent = null;
        if (SwrveGcmNotification.getInstance(context).activityClass != null) {
            intent = new Intent(context, SwrveGcmNotification.getInstance(context).activityClass);
            intent.putExtra(SwrveGcmNotification.GCM_BUNDLE, msg);
            intent.setAction("openActivity");
        }
        return intent;
    }
}
