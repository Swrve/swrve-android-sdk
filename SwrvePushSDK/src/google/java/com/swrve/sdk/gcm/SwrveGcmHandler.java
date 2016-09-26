package com.swrve.sdk.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrvePushConstants;
import com.swrve.sdk.SwrvePushHelper;
import com.swrve.sdk.SwrvePushSDK;

public class SwrveGcmHandler implements ISwrveGcmHandler {

    protected static final String TAG = "SwrveGcm";

    private Context context;
    private SwrveGcmIntentService swrveGcmService;

    protected SwrveGcmHandler(Context context, SwrveGcmIntentService swrveGcmService) {
        this.context = context;
        this.swrveGcmService = swrveGcmService;
    }

    /**
     * @deprecated Use {@link #onMessageReceived} instead.
     */
    @Deprecated
    @Override
    public boolean onHandleIntent(Intent intent, GoogleCloudMessaging gcm) {
        return onMessageReceived("", intent.getExtras());
    }

    @Override
    public boolean onMessageReceived(String from, Bundle data) {
        boolean handled = false;
        if (data != null && !data.isEmpty()) {  // has effect of unparcelling Bundle
            SwrveLogger.i(TAG, "Received GCM notification: " + data.toString());
            handled = processRemoteNotification(data);
        }
        return handled;
    }

    private boolean processRemoteNotification(Bundle msg) {
        boolean handled = false;
        if (SwrvePushHelper.isSwrveRemoteNotification(msg)) {
            //Get tracking key
            Object rawId = msg.get(SwrvePushConstants.SWRVE_TRACKING_KEY);
            String msgId = (rawId != null) ? rawId.toString() : null;

            swrveGcmService.processNotification(msg);

            //Inform swrve push sdk
            SwrvePushSDK pushSdk = SwrvePushSDK.getInstance();
            if (pushSdk != null) {
                pushSdk.onMessage(msgId, msg);
            } else {
                SwrveLogger.e(TAG, "Unable to send msg to pushSDK from GCM Handler.");
            }

            handled = true;
        } else {
            SwrveLogger.i(TAG, "GCM notification: but not processing as its missing the " + SwrvePushConstants.SWRVE_TRACKING_KEY);
        }
        return handled;
    }

    @Override
    public void processNotification(final Bundle msg) {
        boolean mustShowNotification = swrveGcmService.mustShowNotification();
        if (mustShowNotification) {
            try {
                // Put the message into a notification and post it.
                final NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

                final PendingIntent contentIntent = swrveGcmService.createPendingIntent(msg);
                if (contentIntent != null) {
                    final Notification notification = swrveGcmService.createNotification(msg, contentIntent);
                    if (notification != null) {
                        swrveGcmService.showNotification(mNotificationManager, notification);
                    }
                }
            } catch (Exception ex) {
                SwrveLogger.e(TAG, "Error processing GCM push notification", ex);
            }
        } else {
            SwrveLogger.i(TAG, "GCM notification: not processing as mustShowNotification is false.");
        }
    }

    @Override
    public boolean mustShowNotification() {
        return true;
    }

    @Override
    public int showNotification(NotificationManager notificationManager, Notification notification) {
        int id = SwrvePushHelper.generateTimestampId();
        notificationManager.notify(id, notification);
        return id;
    }

    @Override
    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        return SwrvePushHelper.createNotificationBuilder(context, msgText, msg);
    }

    @Override
    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        String msgText = msg.getString("text");
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            // Build notification
            NotificationCompat.Builder mBuilder = swrveGcmService.createNotificationBuilder(msgText, msg);
            mBuilder.setContentIntent(contentIntent);
            return mBuilder.build();
        }
        return null;
    }

    @Override
    public PendingIntent createPendingIntent(Bundle msg) {
        Intent intent = swrveGcmService.createIntent(msg);
        if (intent != null) {
            return PendingIntent.getBroadcast(context, SwrvePushHelper.generateTimestampId(), intent,PendingIntent.FLAG_CANCEL_CURRENT);
        }
        return null;
    }

    @Override
    public Intent createIntent(Bundle msg) {
        return SwrvePushHelper.createPushEngagedIntent(context, msg);
    }
}
