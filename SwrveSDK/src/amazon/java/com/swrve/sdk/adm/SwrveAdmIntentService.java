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

public class SwrveAdmIntentService extends ADMMessageHandlerBase {
    private final static String TAG = "SwrveAdm";
    private final static String AMAZON_RECENT_PUSH_IDS = "recent_push_ids";
    private final static String AMAZON_PREFERENCES = "swrve_amazon_pref";
    private final int MAX_ID_CACHE_ITEMS = 16;

    //SwrveMessageReceiver listens for messages from ADM
    public static class SwrveAdmMessageReceiver extends ADMMessageReceiver {
        public SwrveAdmMessageReceiver() {
            super(SwrveAdmIntentService.class);
        }
    }

    public SwrveAdmIntentService() {
        super(SwrveAdmIntentService.class.getName());
    }

    public SwrveAdmIntentService(final String className) {
        super(className);
    }

    @Override
    protected void onMessage(final Intent intent) {
        if (intent == null) {
            SwrveLogger.e(TAG, "Unexpected null intent");
            return;
        }

        final Bundle extras = intent.getExtras();
        if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
            SwrveLogger.i(TAG, "Received ADM notification: " + extras.toString());
            processRemoteNotification(extras);
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

    protected boolean isSwrveRemoteNotification(final Bundle msg) {
        Object rawId = msg.get(SwrveAdmConstants.SWRVE_TRACKING_KEY);
        String msgId = (rawId != null) ? rawId.toString() : null;
        return !SwrveHelper.isNullOrEmpty(msgId);
    }

    private void processRemoteNotification(Bundle msg) {
        if (!isSwrveRemoteNotification(msg)) {
            SwrveLogger.i(TAG, "ADM notification: but not processing as it doesn't contain: " + SwrveAdmConstants.SWRVE_TRACKING_KEY);
            return;
        }

        //Get the context
        Context context = getApplicationContext();

        //Deduplicate notification
        //Get tracking key
        Object rawId = msg.get(SwrveAdmConstants.SWRVE_TRACKING_KEY);
        String msgId = (rawId != null) ? rawId.toString() : null;

        final String timestamp = msg.getString(SwrveAdmConstants.TIMESTAMP_KEY);
        if (SwrveHelper.isNullOrEmpty(timestamp)) {
            SwrveLogger.e(TAG, "ADM notification: but not processing as it's missing " + SwrveAdmConstants.TIMESTAMP_KEY);
            return;
        }

        //Check for duplicates. This is a necessary part of using ADM which might clone
        //a message as part of attempting to deliver it. We de-dupe by
        //checking against the tracking id and timestamp. (Multiple pushes with the same
        //tracking id are possible in some scenarios from Swrve).
        //Id is concatenation of tracking key and timestamp "$_p:$_s.t"
        String curId = msgId + ":" + timestamp;
        LinkedList<String> recentIds = getRecentNotificationIdCache(context);
        if (recentIds.contains(curId)) {
            //Found a duplicate
            SwrveLogger.i(TAG, "ADM notification: but not processing because duplicate Id: " + curId);
            return;
        }

        //No duplicate found. Update the cache.
        updateRecentNotificationIdCache(recentIds, curId, MAX_ID_CACHE_ITEMS, context);

        // Notify bound clients
        Iterator<SwrveQAUser> iter = SwrveQAUser.getBindedListeners().iterator();
        while (iter.hasNext()) {
            SwrveQAUser sdkListener = iter.next();
            sdkListener.pushNotification(msgId, msg);
        }

        //Process
        processNotification(msg, context);
    }

    private LinkedList<String> getRecentNotificationIdCache(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(AMAZON_PREFERENCES, Context.MODE_PRIVATE);
        String jsonString = sharedPreferences.getString(AMAZON_RECENT_PUSH_IDS, "");
        Gson gson = new Gson();
        LinkedList<String> recentIds = gson.fromJson(jsonString, new TypeToken<LinkedList<String>>() {}.getType());
        recentIds = recentIds == null ? new LinkedList<String>() : recentIds;
        return recentIds;
    }

    private void updateRecentNotificationIdCache(LinkedList<String> recentIds, String newId, int maxCacheItems, Context context) {
        //Update queue
        recentIds.add(newId);
        //Maintain cache size limit
        while (recentIds.size() > MAX_ID_CACHE_ITEMS) {
            recentIds.remove();
        }

        //Store latest queue to shared preferences
        Gson gson = new Gson();
        String recentNotificationsJson = gson.toJson(recentIds);
        SharedPreferences sharedPreferences = context.getSharedPreferences(AMAZON_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(AMAZON_RECENT_PUSH_IDS, recentNotificationsJson).apply();
    }

    private void processNotification(final Bundle msg, Context context) {
        try {
            // Put the message into a notification and post it.
            final NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

            final PendingIntent contentIntent = createPendingIntent(msg, context);
            if (contentIntent == null) {
                SwrveLogger.e(TAG, "Error processing ADM push notification. Unable to create intent");
                return;
            }
            
            final Notification notification = createNotification(msg, contentIntent, context);
            if (notification == null) {
                SwrveLogger.e(TAG, "Error processing ADM push notification. Unable to create notification.");
                return;
            }

            //Time to show notification
            showNotification(mNotificationManager, notification);
        } catch (Exception ex) {
            SwrveLogger.e(TAG, "Error processing ADM push notification", ex);
        }
    }

    private int showNotification(NotificationManager notificationManager, Notification notification) {
        int id = generateTimestampId();
        notificationManager.notify(id, notification);
        return id;
    }

    private int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }

    private Notification createNotification(Bundle msg, PendingIntent contentIntent, Context context) {
        String msgText = msg.getString("text");
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            // Build notification
            NotificationCompat.Builder mBuilder = createNotificationBuilder(msgText, msg, context);
            mBuilder.setContentIntent(contentIntent);
            return mBuilder.build();
        }
        return null;
    }

    private NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg, Context context) {
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

    private PendingIntent createPendingIntent(Bundle msg, Context context) {
        Intent intent = createIntent(msg, context);
        if (intent != null) {
            return PendingIntent.getBroadcast(context, generateTimestampId(), intent,PendingIntent.FLAG_CANCEL_CURRENT);
        }
        return null;
    }

    private Intent createIntent(Bundle msg, Context context) {
        Intent intent = new Intent(context, SwrvePushEngageReceiver.class);
        intent.putExtra(SwrveAdmConstants.ADM_BUNDLE, msg);
        return intent;
    }
}

