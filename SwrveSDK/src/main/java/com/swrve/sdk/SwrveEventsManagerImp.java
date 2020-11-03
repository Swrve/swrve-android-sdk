  package com.swrve.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.localstorage.LocalStorage;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;
import com.swrve.sdk.rest.RESTResponseLog;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

public class SwrveEventsManagerImp implements SwrveEventsManager {

    private static final Object RESPONSE_LOG_LOCK = new Object();
    protected static final String PREF_EVENT_SEND_RESPONSE_LOG = "EVENT_SEND_RESPONSE_LOG";
    protected static boolean shouldSendResponseLogs = true; // static and true by default but set to false once logs are cleared.

    private final Context context;
    private final SwrveConfigBase config;
    private final IRESTClient restClient;
    private final String userId;
    private final String appVersion;
    private final String sessionToken;
    private final String deviceId;

    protected SwrveEventsManagerImp(Context context, SwrveConfigBase config, IRESTClient restClient, String userId, String appVersion, String sessionToken, String deviceId) {
        this.context = context;
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
        synchronized (SwrveMultiLayerLocalStorage.EVENT_LOCK) {
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
        synchronized (SwrveMultiLayerLocalStorage.EVENT_LOCK) {
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
        int eventsToSend = 0;
        final LinkedHashMap<Long, String> events = new LinkedHashMap<>();
        if (!combinedEvents.isEmpty()) {
            SwrveLogger.i("Sending queued events");
            try {
                // Combine all events
                LocalStorage storageForSendingResponseEvent = null;
                for (LocalStorage storage : combinedEvents.keySet()) {
                    events.putAll(combinedEvents.get(storage));
                    storageForSendingResponseEvent = storage;
                }
                eventsToSend = events.size();
                String data = EventHelper.eventsAsBatch(events, userId, appVersion, sessionToken, deviceId);
                SwrveLogger.i("Sending %s events to Swrve", events.size());
                postBatchRequest(storageForSendingResponseEvent, data, eventsToSend, new IPostBatchRequestListener() {
                    public void onResponse(boolean shouldDelete) {
                        if (shouldDelete) {
                            // Remove events from where they came from
                            for (LocalStorage storage : combinedEvents.keySet()) {
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
        return eventsToSend;
    }

    private void postBatchRequest(final LocalStorage localStorage, final String postData, final int numOfEvents, final IPostBatchRequestListener listener) {

        restClient.post(config.getEventsUrl() + SwrveBase.BATCH_EVENTS_ACTION, postData, new IRESTResponseListener() {
            @Override
            public void onResponse(RESTResponse response) {
                boolean deleteEvents = true;
                if (SwrveHelper.userErrorResponseCode(response.responseCode)) {
                    logResponse(response, numOfEvents);
                    SwrveLogger.e("Error sending events to Swrve. responseCode: %s\tresponseBody:%s", response.responseCode, response.responseBody);
                } else if (SwrveHelper.successResponseCode(response.responseCode)) {
                    sendResponseLogs(localStorage);
                    SwrveLogger.i("Events sent to Swrve");
                } else if (SwrveHelper.serverErrorResponseCode(response.responseCode)) {
                    deleteEvents = false;
                    SwrveLogger.e("Error sending events to Swrve. Wil retry. responseCode: %s\tresponseBody:%s", response.responseCode, response.responseBody);
                } else {
                    logResponse(response, numOfEvents);
                    SwrveLogger.e("Error sending events to Swrve. responseCode: %s\tresponseBody:%s", response.responseCode, response.responseBody);
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

    // Persist an aggregated log of response codes to be sent after the next 200 response is received.
    // At time of implementation logging <200, & 300-499 response codes.
    protected void logResponse(RESTResponse response, int eventsCount) {

        String responseCode = String.valueOf(response.responseCode);
        Gson gson = new Gson();

        synchronized (RESPONSE_LOG_LOCK) {

            SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_EVENT_SEND_RESPONSE_LOG, MODE_PRIVATE);

            RESTResponseLog log;
            String savedResponseJson = sharedPreferences.getString(responseCode, null);
            if (savedResponseJson == null) {
                log = new RESTResponseLog(response.responseCode, eventsCount, 1, getTime(), response.responseBody, response.responseHeaders);
            } else {
                log = gson.fromJson(savedResponseJson, new TypeToken<RESTResponseLog>() {
                }.getType());
                int newEventsCount = log.eventsCount + eventsCount;
                int newRequestCount = log.requestCount + 1;
                log = new RESTResponseLog(response.responseCode, newEventsCount, newRequestCount, getTime(), response.responseBody, response.responseHeaders);
            }

            String responseLogJson = gson.toJson(log);
            sharedPreferences.edit().putString(responseCode, responseLogJson).apply();

            shouldSendResponseLogs = true;
        }
    }

    protected void sendResponseLogs(LocalStorage localStorage) {

        if (!shouldSendResponseLogs) {
            return;
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_EVENT_SEND_RESPONSE_LOG, MODE_PRIVATE);
        Map<String, ?> responseLogsMap;
        synchronized (RESPONSE_LOG_LOCK) {
            responseLogsMap = sharedPreferences.getAll();
            sharedPreferences.edit().clear().apply();
            shouldSendResponseLogs = false;
        }

        try {
            List<String> eventsJson = new ArrayList<>();
            Gson gson = new Gson();

            for (Map.Entry<String, ?> entry : responseLogsMap.entrySet()) {
                String responseLogJson = (String) entry.getValue();
                RESTResponseLog responseLog = gson.fromJson(responseLogJson, new TypeToken<RESTResponseLog>() {
                }.getType());
                Map<String, String> payload = new HashMap();
                payload.put("code", String.valueOf(responseLog.code));
                payload.put("events_count", String.valueOf(responseLog.eventsCount));
                payload.put("request_count", String.valueOf(responseLog.requestCount));
                payload.put("body", responseLog.body);
                payload.put("headers", responseLog.headers);
                payload.put("time", String.valueOf(responseLog.time));

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("name", "Swrve.RestResponseLog");
                int seqNum = getNextSequenceNumber();
                String eventString = EventHelper.eventAsJSON("event", parameters, payload, seqNum, getTime());
                eventsJson.add(eventString);
            }

            if (localStorage != null) {
                storeAndSendEvents(eventsJson, localStorage);
            }

        } catch (Exception ex) {
            SwrveLogger.e("Error sending rest response logs.", ex);
        }
    }

    protected int getNextSequenceNumber() {
        return SwrveCommon.getInstance() == null ? 0 : SwrveCommon.getInstance().getNextSequenceNumber();
    }

    protected long getTime() {
        return System.currentTimeMillis();
    }
}
