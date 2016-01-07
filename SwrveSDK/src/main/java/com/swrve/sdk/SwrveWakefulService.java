package com.swrve.sdk;

import android.app.IntentService;
import android.content.Intent;

import com.swrve.sdk.localstorage.ILocalStorage;
import com.swrve.sdk.localstorage.MemoryCachedLocalStorage;
import com.swrve.sdk.rest.RESTClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SwrveWakefulService extends IntentService {

    private static final String LOG_TAG = "SwrveWakeful";
    public static final String EXTRA_SEND_EVENTS = "swrve_wakeful_events";

    public SwrveWakefulService() {
        super("SwrveWakefulService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            if (hasValidExtra(intent)) {
                handleSendEvents(intent);
            } else {
                SwrveLogger.e(LOG_TAG, "SwrveWakefulService: Unknown intent received.");
            }
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "SwrveWakefulService exception:", ex);
        } finally {
            SwrveWakefulReceiver.completeWakefulIntent(intent);
        }
    }

    private boolean hasValidExtra(Intent intent) {
        if (intent.hasExtra(EXTRA_SEND_EVENTS) ) {
            if(intent.getExtras().getStringArrayList(EXTRA_SEND_EVENTS) != null) {
                ArrayList<String> events = intent.getExtras().getStringArrayList(EXTRA_SEND_EVENTS);
                if(events.size() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    protected int handleSendEvents(Intent intent) throws Exception {
        int eventsSent = 0;
        ArrayList<String> events = intent.getExtras().getStringArrayList(EXTRA_SEND_EVENTS);
        Swrve swrve = (Swrve) SwrveSDKBase.getInstance();
        MemoryCachedLocalStorage memoryCachedLocalStorage = null;
        ILocalStorage localStorage = null;
        try {
            memoryCachedLocalStorage = swrve.createCachedLocalStorage();
            localStorage = swrve.createLocalStorage();
            memoryCachedLocalStorage.setSecondaryStorage(localStorage);

            storeEvents(events, memoryCachedLocalStorage);

            String sessionToken = SwrveHelper.generateSessionToken(swrve.apiKey, swrve.appId, swrve.userId);
            RESTClient restClient = new RESTClient(swrve.config.getHttpTimeout());
            QueuedEventsManager queuedEventsManager = new QueuedEventsManager(swrve.config, restClient, swrve.userId, swrve.appVersion, sessionToken);
            eventsSent = queuedEventsManager.sendQueuedEvents(memoryCachedLocalStorage);
        } finally {
            if (memoryCachedLocalStorage != null) memoryCachedLocalStorage.close();
            if (localStorage != null) localStorage.close();
        }
        SwrveLogger.i(LOG_TAG, "SwrveWakefulService:eventsSent:" + eventsSent);
        return eventsSent;
    }

    /**
     * Store the event first before trying to send it later.
     */
    private void storeEvents(ArrayList<String> events, MemoryCachedLocalStorage cachedLocalStorage) throws Exception {
        for (String event : events) {
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", event);
            String eventString = EventHelper.eventAsJSON("event", parameters, null, cachedLocalStorage);
            cachedLocalStorage.addEvent(eventString);
        }
    }
}
