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
    public static final String EXTRA_LOCATIONS_IMPRESSION_IDS = "swrve_wakeful_location_impression_ids";
    public static final String EXTRA_LOCATIONS_ENGAGED_IDS = "swrve_wakeful_location_engaged_ids";

    private Swrve swrve = (Swrve) SwrveSDKBase.getInstance();

    public SwrveWakefulService() {
        super("SwrveWakefulService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            boolean unkownIntentContent = true;
            ArrayList<String> eventsExtras = intent.getExtras().getStringArrayList(EXTRA_EVENTS);
            if (eventsExtras != null) {
                unkownIntentContent = false;
                handleSendEvents(SwrveEventsManager.EventType.NamedEvent, eventsExtras);
            }

            ArrayList<Integer> locationImpressionsExtras = intent.getExtras().getIntegerArrayList(EXTRA_LOCATIONS_IMPRESSION_IDS);
            if (locationImpressionsExtras != null) {
                unkownIntentContent = false;
                handleSendEvents(SwrveEventsManager.EventType.LocationImpressionEvent, locationImpressionsExtras);
            }

            ArrayList<Integer> locationEngagedExtras = intent.getExtras().getIntegerArrayList(EXTRA_LOCATIONS_ENGAGED_IDS);
            if (locationEngagedExtras != null) {
                unkownIntentContent = false;
                handleSendEvents(SwrveEventsManager.EventType.LocationEngagedEvent, locationEngagedExtras);
            }

            if (unkownIntentContent) {
                SwrveLogger.e(LOG_TAG, "SwrveWakefulService: Unknown intent received.");
            }
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "SwrveWakefulService exception:", ex);
        } finally {
            SwrveWakefulReceiver.completeWakefulIntent(intent);
        }
    }

    protected int handleSendEvents(SwrveEventsManager.EventType eventType, ArrayList<?> data) throws Exception {
        int eventsSent = 0;
        MemoryCachedLocalStorage memoryCachedLocalStorage = null;
        SQLiteLocalStorage sqLiteLocalStorage = null;
        try {
            sqLiteLocalStorage = new SQLiteLocalStorage(getApplicationContext(), swrve.config.getDbName(), swrve.config.getMaxSqliteDbSize());
            memoryCachedLocalStorage = new MemoryCachedLocalStorage(sqLiteLocalStorage, null);

            SwrveEventsManager swrveEventsManager = getSendEventsManager(memoryCachedLocalStorage);

            if (eventType == SwrveEventsManager.EventType.NamedEvent) {
                eventsSent = swrveEventsManager.storeAndSendEvents(eventType, data, memoryCachedLocalStorage, sqLiteLocalStorage);
            } else if (eventType == SwrveEventsManager.EventType.LocationImpressionEvent) {
                eventsSent = swrveEventsManager.storeAndSendEvents(eventType, data, memoryCachedLocalStorage, sqLiteLocalStorage);
            } else if (eventType == SwrveEventsManager.EventType.LocationEngagedEvent) {
                eventsSent = swrveEventsManager.storeAndSendEvents(eventType, data, memoryCachedLocalStorage, sqLiteLocalStorage);
            }
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
