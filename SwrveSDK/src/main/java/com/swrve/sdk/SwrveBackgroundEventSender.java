package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import java.util.ArrayList;

public class SwrveBackgroundEventSender {
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_EVENTS = "swrve_wakeful_events";

    private final Swrve swrve;
    private final Context context;
    private String userId;

    public SwrveBackgroundEventSender(Swrve swrve, Context context) {
        this.swrve = swrve;
        this.context = context;
    }

    public int handleSendEvents(Bundle extras) throws Exception {

        initUserId(extras);

        ArrayList<String> eventsExtras = extras.getStringArrayList(EXTRA_EVENTS);
        if (eventsExtras != null && eventsExtras.size() > 0) {
            return handleSendEvents(eventsExtras);
        } else {
            SwrveLogger.e("SwrveBackgroundEventSender: Unknown intent received (extras: %s).", extras);
        }
        return 0;
    }

    private void initUserId(Bundle extras) {
        boolean hasUserId = extras.containsKey(EXTRA_USER_ID);
        if (hasUserId) {
            userId = extras.getString(EXTRA_USER_ID);
        }
        if (SwrveHelper.isNullOrEmpty(userId)) {
            userId = SwrveSDK.getUserId(); // fallback to current logged in user
        }
    }

    private int handleSendEvents(ArrayList<String> events) throws Exception {
        int eventsSent = 0;
        SQLiteLocalStorage sqLiteLocalStorage = new SQLiteLocalStorage(context, swrve.config.getDbName(), swrve.config.getMaxSqliteDbSize());
        SwrveMultiLayerLocalStorage multiLayerLocalStorage = new SwrveMultiLayerLocalStorage(sqLiteLocalStorage);
        if (SwrveHelper.isNotNullOrEmpty(userId)) {
            SwrveEventsManager swrveEventsManager = getSendEventsManager(swrve, userId, multiLayerLocalStorage);
            eventsSent = swrveEventsManager.storeAndSendEvents(events, sqLiteLocalStorage); // always choose the SQLiteLocalStorage to store events from the background
            SwrveLogger.i("SwrveBackgroundEventSender: eventsSent: " + eventsSent);
        } else {
            SwrveLogger.i("SwrveBackgroundEventSender: no user to save events log events against.");
        }
        return eventsSent;
    }

    private SwrveEventsManager getSendEventsManager(Swrve swrve, String userId, SwrveMultiLayerLocalStorage multiLayerLocalStorage) {
        String deviceId = SwrveLocalStorageUtil.getDeviceId(multiLayerLocalStorage);
        String sessionToken = SwrveHelper.generateSessionToken(swrve.apiKey, swrve.appId, userId);
        return new SwrveEventsManagerImp(context, swrve.config, swrve.restClient, userId, swrve.appVersion, sessionToken, deviceId);
    }
}
