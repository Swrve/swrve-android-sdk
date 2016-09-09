package com.swrve.sdk.adm;

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
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.amazon.device.messaging.ADMMessageReceiver;
import com.swrve.sdk.ISwrveBase;
import com.swrve.sdk.Swrve;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrvePushEngageReceiver;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.qa.SwrveQAUser;

import java.util.Date;
import java.util.Iterator;

public class SwrveAdmMessageHandler extends ADMMessageHandlerBase {
    /** Tag for logs. */
    private final static String TAG = "SwrveAdm";

    //SwrveMessageReceiver listens for messages from ADM
    public static class SwrveAdmMessageReceiver extends ADMMessageReceiver {
        public SwrveAdmMessageReceiver() {
            super(SwrveAdmMessageHandler.class);
        }
    }

    public SwrveAdmMessageHandler() {
        super(SwrveAdmMessageHandler.class.getName());
    }

    public SwrveAdmMessageHandler(final String className) {
        super(className);
    }

    @Override
    protected void onMessage(final Intent intent) {
        Log.i(TAG, "SwrveAdmMessageHandler:onMessage");

        final Bundle extras = intent.getExtras();
        verifyMD5Checksum(extras);

        if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
            SwrveLogger.i(TAG, "Received ADM notification: " + extras.toString());
            processRemoteNotification(extras);
        }
    }

    private void processRemoteNotification(Bundle msg) {
        if (isSwrveRemoteNotification(msg)) {
            // Notify bound clients
            Iterator<SwrveQAUser> iter = SwrveQAUser.getBindedListeners().iterator();
            Object rawId = msg.get(SwrveAdmConstants.SWRVE_TRACKING_KEY);
            String msgId = (rawId != null) ? rawId.toString() : null;
            while (iter.hasNext()) {
                SwrveQAUser sdkListener = iter.next();
                sdkListener.pushNotification(msgId, msg);
            }
            processNotification(msg);
        } else {
            SwrveLogger.i(TAG, "ADM notification: but not processing as it's missing " + SwrveAdmConstants.SWRVE_TRACKING_KEY);
        }
    }

    private boolean isSwrveRemoteNotification(final Bundle msg) {
        Object rawId = msg.get(SwrveAdmConstants.SWRVE_TRACKING_KEY);
        String msgId = (rawId != null) ? rawId.toString() : null;
        return !SwrveHelper.isNullOrEmpty(msgId);
    }

    public void processNotification(final Bundle msg) {
        boolean mustShowNotification = mustShowNotification();
        if (mustShowNotification) {
            try {
                // Put the message into a notification and post it.
                Context context = getApplicationContext();
                final NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

                final PendingIntent contentIntent = createPendingIntent(msg);
                if (contentIntent != null) {
                    final Notification notification = createNotification(msg, contentIntent);
                    if (notification != null) {
                        showNotification(mNotificationManager, notification);
                    }
                }
            } catch (Exception ex) {
                SwrveLogger.e(TAG, "Error processing ADM push notification", ex);
            }
        } else {
            SwrveLogger.i(TAG, "ADM notification: not processing as mustShowNotification is false.");
        }
    }

    public boolean mustShowNotification() {
        return true;
    }

    private int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }

    public int showNotification(NotificationManager notificationManager, Notification notification) {
        int id = generateTimestampId();
        notificationManager.notify(id, notification);
        return id;
    }

    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        String msgText = msg.getString("text");
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            // Build notification
            NotificationCompat.Builder mBuilder = createNotificationBuilder(msgText, msg);
            mBuilder.setContentIntent(contentIntent);
            return mBuilder.build();
        }

        return null;
    }

    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        Context context = getApplicationContext();
        SwrveAdmNotification notificationHelper = SwrveAdmNotification.getInstance(context);
        boolean materialDesignIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        int iconResource = (materialDesignIcon && notificationHelper.iconMaterialDrawableId >= 0) ? notificationHelper.iconMaterialDrawableId : notificationHelper.iconDrawableId;

        // Build notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(iconResource)
                .setTicker(msgText)
                .setContentTitle(notificationHelper.notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msgText))
                .setContentText(msgText)
                .setAutoCancel(true);

        if (notificationHelper.largeIconDrawable != null) {
            mBuilder.setLargeIcon(notificationHelper.largeIconDrawable);
        }

        if (notificationHelper.accentColor >= 0) {
            mBuilder.setColor(ContextCompat.getColor(context, notificationHelper.accentColor));
        }

        String msgSound = msg.getString("sound");
        if (!SwrveHelper.isNullOrEmpty(msgSound)) {
            Uri soundUri;
            if (msgSound.equalsIgnoreCase("default")) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            } else {
                String packageName = context.getApplicationContext().getPackageName();
                soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/raw/" + msgSound);
            }
            mBuilder.setSound(soundUri);
        }
        return mBuilder;
    }

    public PendingIntent createPendingIntent(Bundle msg) {
        Intent intent = createIntent(msg);
        if (intent != null) {
            Context context = getApplicationContext();
            return PendingIntent.getBroadcast(context, generateTimestampId(), intent,PendingIntent.FLAG_CANCEL_CURRENT);
        }
        return null;
    }

    public Intent createIntent(Bundle msg) {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, SwrvePushEngageReceiver.class);
        intent.putExtra(SwrveAdmConstants.ADM_BUNDLE, msg);
        return intent;
    }

    private void verifyMD5Checksum(final Bundle extras) {
        //TODO
    }

    @Override
    protected void onRegistrationError(final String string) {
        //This is fatal for ADM
        Log.e(TAG, "SwrveAdmMessageHandler:onRegistrationError " + string);
    }

    @Override
    protected void onRegistered(final String registrationId) {
        Log.i(TAG, "registrationId:" + registrationId);
        ISwrveBase sdk = SwrveSDK.getInstance();
        if (sdk != null && sdk instanceof Swrve) {
            ((Swrve) sdk).onRegistrationIdReceived(registrationId);
        } else {
            SwrveLogger.e(TAG, "Could not notify the SDK of a new token. Consider using the shared instance.");
        }
    }

    @Override
    protected void onUnregistered(final String registrationId) {
        Log.i(TAG, "SwrveAdmMessageHandler:onUnregistered");
    }
}

