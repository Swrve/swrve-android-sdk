package com.swrve.sdk;

import android.app.IntentService;
import android.content.Intent;

import com.swrve.sdk.localstorage.MemoryCachedLocalStorage;
import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.RESTClient;

import java.util.ArrayList;

public class SwrveWakefulService extends IntentService {

    private static final String LOG_TAG = "SwrveWakeful";
    public static final String EXTRA_EVENTS = "swrve_wakeful_events";

    private Swrve swrve = (Swrve) SwrveSDKBase.getInstance();

    public SwrveWakefulService() {
        super("SwrveWakefulService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            ArrayList<String> eventsExtras = intent.getExtras().getStringArrayList(EXTRA_EVENTS);
            if (eventsExtras != null && eventsExtras.size() > 0) {
                handleSendEvents(eventsExtras);
            } else {
                SwrveLogger.e(LOG_TAG, "SwrveWakefulService: Unknown intent received (extras: " + intent.getExtras() + ").");
            }
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "SwrveWakefulService exception (intent: " + intent + ") :", ex);
        } finally {
            SwrveWakefulReceiver.completeWakefulIntent(intent);
        }
    }

    protected int handleSendEvents(ArrayList<String> eventsJson) throws Exception {
        int eventsSent = 0;
        MemoryCachedLocalStorage memoryCachedLocalStorage = null;
        SQLiteLocalStorage sqLiteLocalStorage = null;
        try {
            sqLiteLocalStorage = new SQLiteLocalStorage(getApplicationContext(), swrve.config.getDbName(), swrve.config.getMaxSqliteDbSize());
            memoryCachedLocalStorage = new MemoryCachedLocalStorage(sqLiteLocalStorage, null);

            SwrveEventsManager swrveEventsManager = getSendEventsManager(memoryCachedLocalStorage);
            eventsSent = swrveEventsManager.storeAndSendEvents(eventsJson, memoryCachedLocalStorage, sqLiteLocalStorage);
        } finally {
            if (sqLiteLocalStorage != null) sqLiteLocalStorage.close();
            if (memoryCachedLocalStorage != null) memoryCachedLocalStorage.close();
        }
        SwrveLogger.i(LOG_TAG, "SwrveWakefulService:eventsSent:" + eventsSent);
        return eventsSent;
    }

    private SwrveEventsManager getSendEventsManager(MemoryCachedLocalStorage memoryCachedLocalStorage){
        IRESTClient restClient = new RESTClient(swrve.config.getHttpTimeout());
        short deviceId = EventHelper.getDeviceId(memoryCachedLocalStorage);
        String sessionToken = SwrveHelper.generateSessionToken(swrve.apiKey, swrve.appId, swrve.userId);
        return new SwrveEventsManager(swrve.config, restClient, swrve.userId, swrve.appVersion, sessionToken, deviceId);
    }
}
