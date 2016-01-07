package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.localstorage.ILocalStorage;
import com.swrve.sdk.localstorage.MemoryCachedLocalStorage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONException;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class QueuedEventsManager {

    protected static final String LOG_TAG = "SwrveSDK";

    private final SwrveConfigBase config;
    private final String userId;
    private final String appVersion;
    private final String sessionToken;
    private final IRESTClient restClient;

    protected QueuedEventsManager(SwrveConfigBase config, IRESTClient restClient, String userId, String appVersion, String sessionToken){
        this.config = config;
        this.restClient = restClient;
        this.userId = userId;
        this.appVersion = appVersion;
        this.sessionToken = sessionToken;
    }

    protected int sendQueuedEvents(MemoryCachedLocalStorage cachedLocalStorage) {
        int eventsSent = 0;
        // Get batch of events and send them
        final LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>> combinedEvents = cachedLocalStorage.getCombinedFirstNEvents(config.getMaxEventsPerFlush());
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
                String data = com.swrve.sdk.EventHelper.eventsAsBatch(userId, appVersion, sessionToken, events, cachedLocalStorage);
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
                SwrveLogger.e(LOG_TAG, "Unable to generate event batch", je);
            }
        }
        return eventsSent;
    }

    protected void postBatchRequest(String postData, final IPostBatchRequestListener listener) {

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
            public void onException(Exception exp) {
            }
        });
    }
}
