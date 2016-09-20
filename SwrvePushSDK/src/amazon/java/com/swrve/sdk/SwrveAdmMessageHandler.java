package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.amazon.device.messaging.ADMMessageReceiver;
import com.google.gson.reflect.TypeToken;

import com.google.gson.Gson;

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
    protected void onRegistered(final String registrationId) {
        SwrveLogger.i(TAG, "ADM Registered. RegistrationId: " + registrationId);
        SwrvePushSDKImp.getInstance().onRegistered(registrationId);
    }

    @Override
    protected void onMessage(final Intent intent) {
        final Bundle extras = intent.getExtras();

        if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
            SwrveLogger.i(TAG, "Received ADM notification: " + extras.toString());
            processRemoteNotification(extras);
        }
    }

    private void processRemoteNotification(Bundle msg) {
        if (isSwrveRemoteNotification(msg)) {
            //Get tracking key
            Object rawId = msg.get(SwrvePushSDKConstants.SWRVE_TRACKING_KEY);
            String msgId = (rawId != null) ? rawId.toString() : null;

            //Check for duplicates
            LinkedList<String> recentIds = getRecentNotificationIdCache();
            if (recentIds.contains(msgId)) {
                //Found a duplicate
                SwrveLogger.i(TAG, "ADM notification: but not processing because duplicate: " + msgId);
            } else {
                //No duplicate found. Update the cache.
                updateRecentNotificationIdCache(recentIds, msgId, MAX_ID_CACHE_ITEMS);

                //Inform swrve notification
                SwrvePushSDKImp.getInstance().onMessage(msgId, msg);
            }
        } else {
            SwrveLogger.i(TAG, "ADM notification: but not processing as it's missing " + SwrvePushSDKConstants.SWRVE_TRACKING_KEY);
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
        Object rawId = msg.get(SwrvePushSDKConstants.SWRVE_TRACKING_KEY);
        String msgId = (rawId != null) ? rawId.toString() : null;
        return !SwrveHelper.isNullOrEmpty(msgId);
    }

    @Override
    protected void onRegistrationError(final String string) {
        //This is considered fatal for ADM
        SwrveLogger.e(TAG, "ADM Registration Error. Error string: " + string);
    }

    @Override
    protected void onUnregistered(final String registrationId) {
        SwrveLogger.i(TAG, "ADM Unregistered. RegistrationId: " + registrationId);
    }
}
