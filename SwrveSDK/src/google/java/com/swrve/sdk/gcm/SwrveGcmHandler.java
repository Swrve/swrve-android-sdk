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
import com.swrve.sdk.SwrveLogger;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.qa.SwrveQAUser;

import java.util.Date;
import java.util.Iterator;

public class SwrveGcmHandler implements ISwrveGcmHandler {

    protected static final String TAG = "SwrveGcm";

    private Context context;
    private SwrveGcmIntentService swrveGcmService;

    protected SwrveGcmHandler (Context context, SwrveGcmIntentService swrveGcmService) {
        this.context = context;
        this.swrveGcmService = swrveGcmService;
    }

    @Override
    public boolean onHandleIntent(Intent intent, GoogleCloudMessaging gcm) {
        boolean gcmHandled = false;
        if(intent != null) {
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
                    SwrveLogger.e(TAG, "Send error: " + extras.toString());
                } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                    SwrveLogger.e(TAG, "Deleted messages on server: " + extras.toString());
                    // If it's a regular GCM message, do some work.
                } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                    // Process notification.
                    gcmHandled = processRemoteNotification(extras);
                    SwrveLogger.i(TAG, "Received notification: " + extras.toString());
                }
            }
        }
        return gcmHandled;
    }

    private boolean processRemoteNotification(Bundle msg) {
        if (isSwrveRemoteNotification(msg)) {
            // Notify binded clients
            Iterator<SwrveQAUser> iter = SwrveQAUser.getBindedListeners().iterator();
            Object rawId = msg.get(SwrveGcmConstants.SWRVE_TRACKING_KEY);
            String msgId = (rawId != null) ? rawId.toString() : null;
            while (iter.hasNext()) {
                SwrveQAUser sdkListener = iter.next();
                sdkListener.pushNotification(msgId, msg);
            }

            // Process notification
            swrveGcmService.processNotification(msg);
            
            return true;
        }
        return false;
    }

    private static boolean isSwrveRemoteNotification(final Bundle msg) {
        Object rawId = msg.get(SwrveGcmConstants.SWRVE_TRACKING_KEY);
        String msgId = (rawId != null) ? rawId.toString() : null;
        return !SwrveHelper.isNullOrEmpty(msgId);
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
                SwrveLogger.e(TAG, "Error processing push notification", ex);
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

        SwrveGcmNotification notificationHelper = SwrveGcmNotification.getInstance(context);
        boolean materialDesignIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        int iconResource = (materialDesignIcon && notificationHelper.iconMaterialDrawableId >= 0) ? notificationHelper.iconMaterialDrawableId : notificationHelper.iconDrawableId;

        // Build notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(iconResource)
                .setTicker(msgText)
                .setContentTitle(notificationHelper.notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msgText))
                .setContentText(msgText)
                .setAutoCancel(true);

        if (notificationHelper.largeIconDrawable != null) {
            mBuilder.setLargeIcon(notificationHelper.largeIconDrawable);
        }

        if (notificationHelper.accentColor >= 0) {
            mBuilder.setColor(notificationHelper.accentColor);
        }

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
