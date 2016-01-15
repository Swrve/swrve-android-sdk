package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.localstorage.ILocalStorage;
import com.swrve.sdk.localstorage.MemoryCachedLocalStorage;
import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class SwrveEventsManager {

    public enum EventType {
        NamedEvent,
        LocationImpressionEvent,
        LocationEngagedEvent
    }

    private static final String LOG_TAG = "SwrveSDK";
    private static final Object lock = new Object();

    private final SwrveConfigBase config;
    private final IRESTClient restClient;
    private final String userId;
    private final String appVersion;
    private final String sessionToken;
    private final short deviceId;

    protected SwrveEventsManager(SwrveConfigBase config, IRESTClient restClient, String userId, String appVersion, String sessionToken, short deviceId) {
        this.config = config;
        this.restClient = restClient;
        this.userId = userId;
        this.appVersion = appVersion;
        this.sessionToken = sessionToken;
        this.deviceId = deviceId;
    }

    /*
     * Stores the events passed in from ArrayList and attempts to send only these events. If successful, these events are removed from storage.
     */
    protected int storeAndSendEvents(EventType eventType, ArrayList<?> events, MemoryCachedLocalStorage memoryCachedLocalStorage, SQLiteLocalStorage sqLiteLocalStorage) throws Exception {
        if (events == null || (events != null && events.size() == 0)) {
            return 0;
        }
        synchronized(lock) {
            LinkedHashMap<Long, String> storedEvents = storeEvents(eventType, events, memoryCachedLocalStorage, sqLiteLocalStorage);
            LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>> combinedEvents = new LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>>();
            combinedEvents.put(memoryCachedLocalStorage, storedEvents);
            return sendEvents(combinedEvents);
        }
    }

    private LinkedHashMap<Long, String> storeEvents(EventType eventType, ArrayList<?> events,
                                                    MemoryCachedLocalStorage memoryCachedLocalStorage, SQLiteLocalStorage sqLiteLocalStorage) throws Exception {
        LinkedHashMap<Long, String> storedEvents = new LinkedHashMap<Long, String>();
        if (eventType == EventType.NamedEvent) {
            // Store named events coming from the list
            ArrayList<String> namedEvents = (ArrayList<String>) events;
            for (String event : namedEvents) {
                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("name", event);
                String eventAsJSON = EventHelper.eventAsJSON("event", parameters, null, memoryCachedLocalStorage);
                long id = sqLiteLocalStorage.addEventAndGetId(eventAsJSON);
                storedEvents.put(id, eventAsJSON);
            }
        } else if (eventType == EventType.LocationImpressionEvent || eventType == EventType.LocationEngagedEvent) {
            ArrayList<Integer> messageIds = (ArrayList<Integer>) events;
            for (Integer messageId : messageIds) {
                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("id", messageId);
                String internalEventType = (eventType == EventType.LocationImpressionEvent)? "location_campaign_impression" : "location_campaign_engaged";
                String eventAsJSON = EventHelper.eventAsJSON(internalEventType, parameters, null, memoryCachedLocalStorage);
                long id = sqLiteLocalStorage.addEventAndGetId(eventAsJSON);
                storedEvents.put(id, eventAsJSON);
            }
        }

        return storedEvents;
    }

    /*
     * Attempts to sends events from local storage and deletes them if successful. Number of events sent configured from config.
     */
    protected int sendStoredEvents(MemoryCachedLocalStorage cachedLocalStorage) {
        synchronized(lock) {
            final LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>> combinedEvents = cachedLocalStorage.getCombinedFirstNEvents(config.getMaxEventsPerFlush());
            return sendEvents(combinedEvents);
        }
    }

    private int sendEvents(final LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>> combinedEvents) {
        int eventsSent = 0;
        final LinkedHashMap<Long, String> events = new LinkedHashMap<Long, String>();
        if (!combinedEvents.isEmpty()) {
            SwrveLogger.i(LOG_TAG, "Sending queued events");
            try {
                // Combine all events
                Iterator<ILocalStorage> storageIt = combinedEvents.keySet().iterator();
                while (storageIt.hasNext()) {
                    events.putAll(combinedEvents.get(storageIt.next()));
                }
                eventsSent = events.size();
                String data = EventHelper.eventsAsBatch(events, userId, appVersion, sessionToken, deviceId);
                SwrveLogger.i(LOG_TAG, "Sending " + events.size() + " events to Swrve");
                postBatchRequest(data, new IPostBatchRequestListener() {
                    public void onResponse(boolean shouldDelete) {
                        if (shouldDelete) {
                            // Remove events from where they came from
                            Iterator<ILocalStorage> storageIt = combinedEvents.keySet().iterator();
                            while (storageIt.hasNext()) {
                                ILocalStorage storage = storageIt.next();
                                storage.removeEventsById(combinedEvents.get(storage).keySet());
                            }
                        } else {
                            SwrveLogger.e(LOG_TAG, "Batch of events could not be sent, retrying");
                        }
                    }
                });
            } catch (JSONException je) {
                SwrveLogger.e(LOG_TAG, "Unable to generate event batch, and send events", je);
            }
        }
        return eventsSent;
    }

    private void postBatchRequest(final String postData, final IPostBatchRequestListener listener) {

        restClient.post(config.getEventsUrl() + SwrveBase.BATCH_EVENTS_ACTION, postData, new IRESTResponseListener() {
            @Override
            public void onResponse(RESTResponse response) {
                boolean deleteEvents = true;
                if (SwrveHelper.userErrorResponseCode(response.responseCode)) {
                    SwrveLogger.e(LOG_TAG, "Error sending events to Swrve: " + response.responseBody);
                } else if (SwrveHelper.successResponseCode(response.responseCode)) {
                    SwrveLogger.i(LOG_TAG, "Events sent to Swrve");
                } else if (SwrveHelper.serverErrorResponseCode(response.responseCode)) {
                    deleteEvents = false;
                    SwrveLogger.e(LOG_TAG, "Error sending events to Swrve: " + response.responseBody);
                }

                // Resend if we got a server error (5XX)
                listener.onResponse(deleteEvents);
            }

            @Override
            public void onException(Exception ex) {
                SwrveLogger.e(LOG_TAG, "Error posting batch of events. postData:" + postData, ex);
            }
        });
    }
}
