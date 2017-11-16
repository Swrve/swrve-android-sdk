package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.localstorage.LocalStorage;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONException;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class SwrveEventsManagerImp implements SwrveEventsManager {

    private final SwrveConfigBase config;
    private final IRESTClient restClient;
    private final String userId;
    private final String appVersion;
    private final String sessionToken;
    private final short deviceId;

    protected SwrveEventsManagerImp(SwrveConfigBase config, IRESTClient restClient, String userId, String appVersion, String sessionToken, short deviceId) {
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
    @Override
    public int storeAndSendEvents(List<String> eventsJson, LocalStorage localStorage) throws Exception {
        if (eventsJson == null || (eventsJson != null && eventsJson.size() == 0)) {
            return 0;
        }
        synchronized(SwrveMultiLayerLocalStorage.EVENT_LOCK) {
            LinkedHashMap<Long, String> storedEvents = storeEvents(eventsJson, localStorage);
            LinkedHashMap<LocalStorage, LinkedHashMap<Long, String>> combinedEvents = new LinkedHashMap<>();
            combinedEvents.put(localStorage, storedEvents);
            return sendEvents(combinedEvents);
        }
    }

    private LinkedHashMap<Long, String> storeEvents(List<String> eventsJson, LocalStorage localStorage) throws Exception {
        LinkedHashMap<Long, String> storedEvents = new LinkedHashMap<>();
        // Store named events coming from the list
        for (String eventAsJSON : eventsJson) {
            long id = localStorage.addEvent(userId, eventAsJSON);
            storedEvents.put(id, eventAsJSON);
        }
        return storedEvents;
    }

    /*
     * Attempts to sends events from local storage and deletes them if successful. Number of events sent configured from config.
     */
    @Override
    public int sendStoredEvents(SwrveMultiLayerLocalStorage multiLayerLocalStorage) {
        synchronized(SwrveMultiLayerLocalStorage.EVENT_LOCK) {
            final LinkedHashMap<LocalStorage, LinkedHashMap<Long, String>> combinedEvents = multiLayerLocalStorage.getCombinedFirstNEvents(config.getMaxEventsPerFlush(), userId);
            return sendEvents(combinedEvents);
        }
    }

    /*
     * Sends events stored from different LocalStorage (eg: some events stored in memory, some in sqlite)
     * @param combinedEvents A collection of event ids to send. The local storage from where to get the event is contained
     * @return total number of events that will be sent (if possible)
     */
    private int sendEvents(final LinkedHashMap<LocalStorage, LinkedHashMap<Long, String>> combinedEvents) {
        int eventsSent = 0;
        final LinkedHashMap<Long, String> events = new LinkedHashMap<>();
        if (!combinedEvents.isEmpty()) {
            SwrveLogger.i("Sending queued events");
            try {
                // Combine all events
                Iterator<LocalStorage> storageIt = combinedEvents.keySet().iterator();
                while (storageIt.hasNext()) {
                    events.putAll(combinedEvents.get(storageIt.next()));
                }
                eventsSent = events.size();
                String data = EventHelper.eventsAsBatch(events, userId, appVersion, sessionToken, deviceId);
                SwrveLogger.i("Sending %s events to Swrve", events.size());
                postBatchRequest(data, new IPostBatchRequestListener() {
                    public void onResponse(boolean shouldDelete) {
                        if (shouldDelete) {
                            // Remove events from where they came from
                            Iterator<LocalStorage> storageIt = combinedEvents.keySet().iterator();
                            while (storageIt.hasNext()) {
                                LocalStorage storage = storageIt.next();
                                storage.removeEvents(userId, combinedEvents.get(storage).keySet());
                            }
                        } else {
                            SwrveLogger.e("Batch of events could not be sent, retrying");
                        }
                    }
                });
            } catch (JSONException je) {
                SwrveLogger.e("Unable to generate event batch, and send events", je);
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
                    SwrveLogger.e("Error sending events to Swrve. responseCode: %s\tresponseBody:%s", response.responseCode, response.responseBody);
                } else if (SwrveHelper.successResponseCode(response.responseCode)) {
                    SwrveLogger.i("Events sent to Swrve");
                } else if (SwrveHelper.serverErrorResponseCode(response.responseCode)) {
                    deleteEvents = false;
                    SwrveLogger.e("Error sending events to Swrve. Wil retry. responseCode: %s\tresponseBody:%s", response.responseCode, response.responseBody);
                }

                // Resend if we got a server error (5XX)
                listener.onResponse(deleteEvents);
            }

            @Override
            public void onException(Exception ex) {
                SwrveLogger.e("Error posting batch of events. postData:%s", ex, postData);
            }
        });
    }
}
