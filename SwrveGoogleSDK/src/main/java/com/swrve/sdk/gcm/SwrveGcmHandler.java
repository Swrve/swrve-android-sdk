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

import java.util.Date;
import java.util.Iterator;

public class SwrveGcmHandler implements ISwrveGcmHandler {

    protected static final String TAG = "SwrveGcm";
    private static int tempNotificationId = 1;

    private Context context;
    private ISwrveGcmService swrveGcmService;

    protected SwrveGcmHandler (Context context, ISwrveGcmService swrveGcmService) {
        this.context = context;
        this.swrveGcmService = swrveGcmService;
    }

    @Override
    public boolean onHandleIntent(Intent intent, GoogleCloudMessaging gcm) {
        boolean gcmHandled = false;
        if(intent !=null) {
            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
                // The getMessageType() intent parameter must be the intent you received in your BroadcastReceiver.
                String messageType = gcm.getMessageType(intent);

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
        swrveGcmService.processNotification(msg);
    }

    @Override
    public void processNotification(final Bundle msg) {
        if (swrveGcmService.mustShowNotification()) {
            try {
                // Put the message into a notification and post it.
                final NotificationManager mNotificationManager = (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);

                final PendingIntent contentIntent = swrveGcmService.createPendingIntent(msg);
                if (contentIntent != null) {
                    final Notification notification = swrveGcmService.createNotification(msg, contentIntent);
                    if (notification != null) {
                        swrveGcmService.showNotification(mNotificationManager, notification);
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error processing push notification", ex);
            }
        }
    }

    @Override
    public boolean mustShowNotification() {
        return true;
    }

    @Override
    public int showNotification(NotificationManager notificationManager, Notification notification) {
        int id = generateTimestampId();
        notificationManager.notify(id, notification);
        return id;
    }

    private int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }

    @Override
    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
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

    @Override
    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        String msgText = msg.getString("text");
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            // Build notification
            NotificationCompat.Builder mBuilder =  swrveGcmService.createNotificationBuilder(msgText, msg);
            mBuilder.setContentIntent(contentIntent);
            return mBuilder.build();
        }

        return null;
    }

    @Override
    public PendingIntent createPendingIntent(Bundle msg) {
        // Add notification to bundle
        Intent intent = swrveGcmService.createIntent(msg);
        if (intent != null) {
            return PendingIntent.getActivity(context, generateTimestampId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return null;
    }

    @Override
    public Intent createIntent(Bundle msg) {
        Intent intent = null;
        if (SwrveGcmNotification.getInstance(context).activityClass != null) {
            intent = new Intent(context, SwrveGcmNotification.getInstance(context).activityClass);
            intent.putExtra(SwrveGcmNotification.GCM_BUNDLE, msg);
            intent.setAction("openActivity");
        }
        return intent;
    }
}
