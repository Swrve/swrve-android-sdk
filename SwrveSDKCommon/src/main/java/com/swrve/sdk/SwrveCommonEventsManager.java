package com.swrve.sdk;

import com.swrve.sdk.localstorage.ILocalStorage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONException;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class SwrveCommonEventsManager {

    protected static final String LOG_TAG = "SwrveSDK";
    protected static final Object lock = new Object();

    protected final ISwrveCommon swrveCommon;
    private final IRESTClient restClient;

    protected SwrveCommonEventsManager(IRESTClient restClient) {
        this.swrveCommon = SwrveCommon.getSwrveCommon();
        if (swrveCommon == null) {
            SwrveLogger.e(LOG_TAG, "You have not called SwrveSDK.createInstance in your Application class. SwrveEventsManager will not be able to operate properly.");
        }

        this.restClient = restClient;
    }

    protected int sendEvents(final LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>> combinedEvents) {
        int eventsSent = 0;
        final LinkedHashMap<Long, String> events = new LinkedHashMap<Long, String>();
        if (!combinedEvents.isEmpty()) {
            SwrveLogger.i(LOG_TAG, "Sending queued events");

            // Combine all events
            Iterator<ILocalStorage> storageIt = combinedEvents.keySet().iterator();
            while (storageIt.hasNext()) {
                events.putAll(combinedEvents.get(storageIt.next()));
            }
            eventsSent = events.size();

            sendEventsAsBatch(events, new IPostBatchRequestListener() {
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
        }
        return eventsSent;
    }

    protected void sendEventsAsBatch(LinkedHashMap<Long, String> events, final IPostBatchRequestListener listener) {
        try {
            String data = EventHelper.eventsAsBatch(
                events,
                swrveCommon.getUserId(),
                swrveCommon.getAppVersion(),
                swrveCommon.getSessionKey(),
                swrveCommon.getDeviceId()
            );
            SwrveLogger.i(LOG_TAG, "Sending " + events.size() + " events to Swrve");
            postBatchRequest(data, listener);
        } catch (JSONException je) {
            SwrveLogger.e(LOG_TAG, "Unable to generate event batch, and send events", je);
        }
    }

    protected void postBatchRequest(final String postData, final IPostBatchRequestListener listener) {

        restClient.post(swrveCommon.getBatchURL(), postData, new IRESTResponseListener() {
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
