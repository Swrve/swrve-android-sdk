package com.swrve.sdk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.LinkedList;

public class SwrveAdmHandler {
    protected static final String TAG = "SwrveAdm";
    private Context context;
    private final static String AMAZON_RECENT_PUSH_IDS = "recent_push_notification_ids";
    private final static String AMAZON_PREFENCES = "swrve_amazon_push_pref";
    private final int MAX_ID_CACHE_ITEMS = 16;

    protected SwrveAdmHandler(Context context) {
        this.context = context;
    }

    public boolean onMessageReceived(Bundle data) {
        boolean handled = false;
        if (data != null && !data.isEmpty()) {  // has effect of unparcelling Bundle
            SwrveLogger.i(TAG, "Received ADM notification: " + data.toString());
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

            //Check for duplicates
            LinkedList<String> recentIds = getRecentNotificationIdCache();
            if (recentIds.contains(msgId)) {
                //Found a duplicate
                SwrveLogger.i(TAG, "ADM notification: but not processing because duplicate: " + msgId);
            } else {
                //No duplicate found. Update the cache.
                updateRecentNotificationIdCache(recentIds, msgId, MAX_ID_CACHE_ITEMS);

                //Make and show a notification
                processNotification(msg);

                //Inform swrve notification
                SwrvePushSDK pushSDK = SwrvePushSDK.getInstance();
                if (pushSDK != null) {
                    pushSDK.onMessage(msgId, msg);
                } else {
                    SwrveLogger.e(TAG, "Unable to send msg to pushSDK from ADM Handler.");
                }

                handled = true;
            }
        } else {
            SwrveLogger.i(TAG, "ADM notification: but not processing as it's missing " + SwrvePushConstants.SWRVE_TRACKING_KEY);
        }
        return handled;
    }

    private LinkedList<String> getRecentNotificationIdCache() {
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
        Gson gson = new Gson();
        String recentNotificationsJson = gson.toJson(recentIds);
        SharedPreferences sharedPreferences = context.getSharedPreferences(AMAZON_PREFENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(AMAZON_RECENT_PUSH_IDS, recentNotificationsJson).apply();
    }

    public void processNotification(final Bundle msg) {
        try {
            // Put the message into a notification and post it.
            final NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

            final PendingIntent contentIntent = createPendingIntent(msg);
            if (contentIntent != null) {
                final Notification notification = createNotification(msg, contentIntent);
                if (notification != null) {
                    showNotification(mNotificationManager, notification);
                }
            }
        } catch (Exception ex) {
            SwrveLogger.e(TAG, "Error processing GCM push notification", ex);
        }
    }

    public int showNotification(NotificationManager notificationManager, Notification notification) {
        int id = SwrvePushHelper.generateTimestampId();
        notificationManager.notify(id, notification);
        return id;
    }

    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        return SwrvePushHelper.createNotificationBuilder(context, msgText, msg);
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

    public PendingIntent createPendingIntent(Bundle msg) {
        Intent intent = createIntent(msg);
        if (intent != null) {
            return PendingIntent.getBroadcast(context, SwrvePushHelper.generateTimestampId(), intent,PendingIntent.FLAG_CANCEL_CURRENT);
        }
        return null;
    }

    public Intent createIntent(Bundle msg) {
        return SwrvePushHelper.createPushEngagedIntent(context, msg);
    }

}
