package com.swrve.sdk.adm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.amazon.device.messaging.ADMMessageReceiver;
import com.google.gson.reflect.TypeToken;
import com.swrve.sdk.ISwrveBase;
import com.swrve.sdk.Swrve;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrvePushEngageReceiver;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.qa.SwrveQAUser;

import com.google.gson.Gson;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

public class SwrveAdmMessageHandler extends ADMMessageHandlerBase {
    private final static String TAG = "SwrveAdm";
    private final static String AMAZON_RECENT_PUSH_IDS = "recent_push_notification_ids";
    private final static String AMAZON_PREFENCES = "swrve_amazon_pref";
    private final int MAX_ID_CACHE_ITEMS = 16;

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
        final Bundle extras = intent.getExtras();

        //TODO decide if using or not
        //verifyMD5Checksum(extras);

        if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
            SwrveLogger.i(TAG, "Received ADM notification: " + extras.toString());
            processRemoteNotification(extras);
        }
    }

    private void processRemoteNotification(Bundle msg) {
        if (isSwrveRemoteNotification(msg)) {
            //Get tracking key
            Object rawId = msg.get(SwrveAdmConstants.SWRVE_TRACKING_KEY);
            String msgId = (rawId != null) ? rawId.toString() : null;

            //Check for duplicates
            LinkedList<String> recentIds = getRecentNotificationIdCache();
            if (recentIds.contains(msgId)) {
                //Found a duplicate
                SwrveLogger.i(TAG, "ADM notification: but not processing because duplicate: " + msgId);
            } else {
                //No duplicate found. Update the cache.
                updateRecentNotificationIdCache(recentIds, msgId, MAX_ID_CACHE_ITEMS);

                // Notify bound clients
                Iterator<SwrveQAUser> iter = SwrveQAUser.getBindedListeners().iterator();
                while (iter.hasNext()) {
                    SwrveQAUser sdkListener = iter.next();
                    sdkListener.pushNotification(msgId, msg);
                }
                processNotification(msg);
            }
        } else {
            SwrveLogger.i(TAG, "ADM notification: but not processing as it's missing " + SwrveAdmConstants.SWRVE_TRACKING_KEY);
        }
    }

    private LinkedList<String> getRecentNotificationIdCache() {
        Context context = getApplicationContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(AMAZON_PREFENCES, Context.MODE_PRIVATE);
        String jsonString = sharedPreferences.getString(AMAZON_RECENT_PUSH_IDS, "");
        Gson gson = new Gson();
        LinkedList<String> recentIds = gson.fromJson(jsonString, new TypeToken<LinkedList<String>>() {}.getType());
        recentIds = recentIds == null ? new LinkedList<String>() : recentIds;
        return recentIds;
    }

    private void updateRecentNotificationIdCache(LinkedList<String> recentIds, String newId, int maxCacheItems) {
        //Update queue
        recentIds.add(newId);
        //Maintain cache size limit
        while (recentIds.size() > MAX_ID_CACHE_ITEMS) {
            recentIds.remove();
        }

        //Store latest queue to shared preferences
        Context context = getApplicationContext();
        Gson gson = new Gson();
        String recentNotificationsJson = gson.toJson(recentIds);
        SharedPreferences sharedPreferences = context.getSharedPreferences(AMAZON_PREFENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(AMAZON_RECENT_PUSH_IDS, recentNotificationsJson).apply();
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
}

