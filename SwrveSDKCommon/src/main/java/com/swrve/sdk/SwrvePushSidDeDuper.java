package com.swrve.sdk;

import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY;
import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.SWRVE_UNIQUE_MESSAGE_ID_MAX_CACHE_KEY;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.LinkedList;
import java.util.Map;

/**
 * Internal swrve class to deduplicate regular/silent push based upon the _sid value in the push payload.
 * Thread safety responsibility is on the calling class. For Firebase this will be called from FirebaseMessagingService
 * which is an IntentService so only one will execute at a time.
 */
class SwrvePushSidDeDuper {

    protected final static String PREFERENCES = "swrve_sids";
    protected final static String SIDS = "_sids";
    protected static final int DEFAULT_SID_CACHE_SIZE = 100;

    private final Context context;
    private int maxCacheSize = DEFAULT_SID_CACHE_SIZE;
    private String sid;

    static boolean isDupe(Context context, Map<String, String> data) {
        SwrvePushSidDeDuper notificationDeDuper = new SwrvePushSidDeDuper(context, data);
        return notificationDeDuper.isDupe();
    }

    SwrvePushSidDeDuper(Context context, Map<String, String> map) {
        this.context = context;
        if (map != null && map.containsKey(SWRVE_UNIQUE_MESSAGE_ID_KEY)) {
            this.sid = map.get(SWRVE_UNIQUE_MESSAGE_ID_KEY);
        }
        if (map != null && map.containsKey(SWRVE_UNIQUE_MESSAGE_ID_MAX_CACHE_KEY)) {
            // The maxCacheSize from server isn't actually implemented so default is used.
            String maxCacheSizeString = map.get(SWRVE_UNIQUE_MESSAGE_ID_MAX_CACHE_KEY);
            this.maxCacheSize = Integer.valueOf(maxCacheSizeString);
        }
    }

    boolean isDupe() {
        if (SwrveHelper.isNullOrEmpty(sid)) {
            return false;
        }
        boolean isDupe = false;
        LinkedList<String> sidsFromCache = getSidsFromCache();
        if (sidsFromCache.contains(sid)) {
            SwrveLogger.w("SwrveSDK: SwrvePushSidDeDuper - duplicate _sid id: %s", sid);
            isDupe = true;
        } else {
            addSidToCache(sidsFromCache);
        }
        return isDupe;
    }

    protected LinkedList<String> getSidsFromCache() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String jsonString = sharedPreferences.getString(SIDS, "");
        Gson gson = new Gson();
        LinkedList<String> recentIds = gson.fromJson(jsonString, new TypeToken<LinkedList<String>>() {
        }.getType());
        recentIds = recentIds == null ? new LinkedList<>() : recentIds;
        return recentIds;
    }

    private void addSidToCache(LinkedList<String> sidsFromCache) {
        sidsFromCache.add(sid);

        while (sidsFromCache.size() > maxCacheSize) {
            sidsFromCache.remove(); //Maintain cache size limit by removing from the head
        }

        Gson gson = new Gson();
        String sidsJson = gson.toJson(sidsFromCache);
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(SIDS, sidsJson).apply();
    }
}
