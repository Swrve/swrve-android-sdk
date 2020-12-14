package com.swrve.sdk;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import java.util.Arrays;
import java.util.List;

public class SwrveBackgroundEventSender {

    protected static final String DATA_KEY_USER_ID = "userId";
    protected static final String DATA_KEY_EVENTS = "events";

    private final SwrveBase swrve;
    private final Context context;
    private String userId;

    protected OneTimeWorkRequest workRequest; // exposed for testing

    public SwrveBackgroundEventSender(SwrveBase swrve, Context context) {
        this.swrve = swrve;
        this.context = context;
    }

    protected void send(String userId, List<String> events) {
        try {
            workRequest = getOneTimeWorkRequest(userId, events);
            enqueueWorkRequest(workRequest);
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: Error trying to queue events to be sent in the background worker.", ex);
        }
    }

    protected OneTimeWorkRequest getOneTimeWorkRequest(String userId, List<String> events) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Data inputData = new Data.Builder()
                .putString(DATA_KEY_USER_ID, userId)
                .putStringArray(DATA_KEY_EVENTS, events.toArray(new String[events.size()]))
                .build();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SwrveBackgroundEventSenderWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)

                .build();
        return workRequest;
    }

    // separate method for testing
    protected synchronized void enqueueWorkRequest(OneTimeWorkRequest workRequest) {
        WorkManager.getInstance(context).enqueue(workRequest);
    }

    protected int handleSendEvents(Data data) throws Exception {

        this.userId = data.getString(DATA_KEY_USER_ID);
        if (SwrveHelper.isNullOrEmpty(userId)) {
            this.userId = SwrveSDK.getUserId(); // fallback to current logged in user
        }
        String[] events = data.getStringArray(DATA_KEY_EVENTS);

        int eventsSent = 0;
        if (events != null && events.length > 0) {
            eventsSent = handleSendEvents(Arrays.asList(events));
        }

        return eventsSent;
    }

    private int handleSendEvents(List<String> events) throws Exception {
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

    private SwrveEventsManager getSendEventsManager(SwrveBase swrve, String userId, SwrveMultiLayerLocalStorage multiLayerLocalStorage) {
        String deviceId = SwrveLocalStorageUtil.getDeviceId(multiLayerLocalStorage);
        String sessionToken = SwrveHelper.generateSessionToken(swrve.apiKey, swrve.appId, userId);
        return new SwrveEventsManagerImp(context, swrve.config, swrve.restClient, userId, swrve.appVersion, sessionToken, deviceId);
    }
}
