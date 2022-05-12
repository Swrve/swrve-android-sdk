package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.BATCH_EVENT_KEY_APP_VERSION;
import static com.swrve.sdk.ISwrveCommon.BATCH_EVENT_KEY_DATA;
import static com.swrve.sdk.ISwrveCommon.BATCH_EVENT_KEY_SESSION_TOKEN;
import static com.swrve.sdk.ISwrveCommon.BATCH_EVENT_KEY_UNIQUE_DEVICE_ID;
import static com.swrve.sdk.ISwrveCommon.BATCH_EVENT_KEY_USER;
import static com.swrve.sdk.ISwrveCommon.BATCH_EVENT_KEY_VERSION;
import static com.swrve.sdk.ISwrveCommon.EVENT_ID_KEY;
import static com.swrve.sdk.ISwrveCommon.EVENT_PAYLOAD_KEY;
import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN;
import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_DELIVERED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_ENGAGED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_ID_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_GEO;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CONTEXT_ID_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_DISPLAYED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_REASON;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_SILENT;

import android.content.Context;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Used internally to generate JSON batch strings from event data.
 */
final class EventHelper {
    private static final Object BATCH_API_VERSION = "3";

    public static String eventAsJSON(String type, Map<String, Object> parameters, int seqnum, long time) throws JSONException {
        return eventAsJSON(type, parameters, null, seqnum, time);
    }

    public static ArrayList<String> createGenericEvent(String id, String campaignType, String actionType, String contextId, String campaignId, Map<String, String> payload, int seqnum) throws JSONException {
        return createGenericEvent(System.currentTimeMillis(), id, campaignType, actionType, contextId, campaignId, payload, seqnum);
    }

    public static ArrayList<String> createGenericEvent(long time, String id, String campaignType, String actionType, String contextId, String campaignId, Map<String, ?> payload, int seqnum) throws JSONException {
        ArrayList<String> events = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(EVENT_ID_KEY, id);
        parameters.put(GENERIC_EVENT_CAMPAIGN_TYPE_KEY, campaignType);
        parameters.put(GENERIC_EVENT_ACTION_TYPE_KEY, actionType);
        if (SwrveHelper.isNotNullOrEmpty(contextId)) {
            parameters.put(GENERIC_EVENT_CONTEXT_ID_KEY, contextId);
        }
        if (SwrveHelper.isNotNullOrEmpty(campaignId)) {
            parameters.put(GENERIC_EVENT_CAMPAIGN_ID_KEY, campaignId);
        }
        String eventAsJSON = EventHelper.eventAsJSON(EVENT_TYPE_GENERIC_CAMPAIGN, parameters, payload, seqnum, time);
        events.add(eventAsJSON);
        return events;
    }

    public static List<String> createSessionStartEvent(long time, int seqNum) {
        List<String> events = new ArrayList<>();
        try {
            String eventString = EventHelper.eventAsJSON("session_start", null, null, seqNum, time);
            events.add(eventString);
        } catch (Exception e) {
            SwrveLogger.e("Exception building session start event", e);
        }
        return events;
    }

    /*
     * Generate JSON to be stored in EventLocalStorage and eventually sent to
     * the batch API to inform Swrve of these events.
     */
    public static String eventAsJSON(String type, Map<String, Object> parameters, Map<String, ?> payload, int seqnum, long time) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(EVENT_TYPE_KEY, type);
        obj.put("time", time);
        if (seqnum > 0) {
            obj.put("seqnum", seqnum);
        }
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                obj.put(entry.getKey(), entry.getValue());
            }
        }

        if (payload != null && payload.size() > 0) {
            obj.put(EVENT_PAYLOAD_KEY, new JSONObject(payload));
        }
        return obj.toString();
    }

    public static String qaLogEventAsJSON(long time, String logSource, String logType, String logDetails) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("time", time);
        obj.put("type", "qa_log_event");
        obj.put("log_source", logSource);
        obj.put("log_type", logType);
        obj.put("log_details", new JSONObject(logDetails));
        return obj.toString();
    }

    /*
     * Generate JSON in the format expected by the batch API to inform Swrve of
     * these events.
     */
    public static String eventsAsBatch(LinkedHashMap<Long, String> events, String userId, String appVersion, String sessionToken, String deviceId) throws JSONException {
        JSONObject batch = new JSONObject();
        batch.put(BATCH_EVENT_KEY_USER, userId);
        batch.put(BATCH_EVENT_KEY_SESSION_TOKEN, sessionToken);
        batch.put(BATCH_EVENT_KEY_VERSION, BATCH_API_VERSION);
        batch.put(BATCH_EVENT_KEY_APP_VERSION, appVersion);
        batch.put(BATCH_EVENT_KEY_UNIQUE_DEVICE_ID, deviceId);
        batch.put(BATCH_EVENT_KEY_DATA, orderedMapToJSONArray(events));
        return batch.toString();
    }

    /*
     * Parse TreeMap of JSON string objects into an array of JSONObjects,
     * ordered by Map key value. Using TreeMap ensures the keys are ordered.
     */
    private static JSONArray orderedMapToJSONArray(LinkedHashMap<Long, String> map)
            throws JSONException {
        JSONArray obj = new JSONArray();

        for (LinkedHashMap.Entry<Long, String> entry : map.entrySet()) {
            String value = entry.getValue();
            obj.put(new JSONObject(value));
        }
        return obj;
    }

    /*
     * Return the event name used for triggers based on the event parameters
     */
    public static String getEventName(String eventType, Map<String, Object> eventParameters) {
        switch (eventType) {
            case "session_start":
                return "Swrve.session.start";
            case "session_end":
                return "Swrve.session.end";
            case "buy_in":
                return "Swrve.buy_in";
            case "iap":
                return "Swrve.iap";
            case "event":
                return (String) eventParameters.get("name");
            case "purchase":
                return "Swrve.user_purchase";
            case "currency_given":
                return "Swrve.currency_given";
            case "user":
                return "Swrve.user_properties_changed";
        }
        return "";
    }

    protected static void sendEngagedEvent(Context context, String campaignType, String id, Map<String, String> payload) {
        try {
            ISwrveCommon swrve = SwrveCommon.getInstance();
            ArrayList<String> events = new ArrayList<>();
            int seqNum = swrve.getNextSequenceNumber();
            if (GENERIC_EVENT_CAMPAIGN_TYPE_GEO.equalsIgnoreCase(campaignType)) {
                SwrveLogger.d("Sending generic engaged event.");
                String contextId = "";
                String campaignId = "";
                events = EventHelper.createGenericEvent(id, GENERIC_EVENT_CAMPAIGN_TYPE_GEO, GENERIC_EVENT_ACTION_TYPE_ENGAGED, contextId, campaignId, payload, seqNum);
            } else if (GENERIC_EVENT_CAMPAIGN_TYPE_PUSH.equalsIgnoreCase(campaignType)) {
                String eventName = "Swrve.Messages.Push-" + id + ".engaged";
                SwrveLogger.d("Sending engaged event: %s", eventName);
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("name", eventName);
                String eventString = EventHelper.eventAsJSON("event", parameters, payload, seqNum, System.currentTimeMillis());
                events.add(eventString);
            }
            swrve.sendEventsInBackground(context, swrve.getUserId(), events);
        } catch (Exception e) {
            SwrveLogger.e("Exception trying to send engaged event.", e);
        }
    }

    protected static void sendButtonClickEvent(Context context, String campaignType, String id, String contextId, Map<String, String> payload) {
        try {
            ISwrveCommon swrve = SwrveCommon.getInstance();
            int seqNum = swrve.getNextSequenceNumber();
            String campaignId = null;
            ArrayList<String> events = EventHelper.createGenericEvent(id, campaignType, GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK, contextId, campaignId, payload, seqNum);
            SwrveLogger.d("Sending button_click for id:%s contextId:%s campaignType:%s", id, contextId, campaignType);
            swrve.sendEventsInBackground(context, swrve.getUserId(), events);
        } catch (Exception e) {
            SwrveLogger.e("Exception trying to send button click event.", e);
        }
    }

    public static String getPushDeliveredBatchEvent(ArrayList<String> eventsList) throws Exception {
        final String userId = SwrveCommon.getInstance().getUserId();
        final String appVersion = SwrveCommon.getInstance().getAppVersion();
        final String sessionKey = SwrveCommon.getInstance().getSessionKey();
        final String deviceId = SwrveCommon.getInstance().getDeviceId();

        LinkedHashMap<Long, String> batchEvents = new LinkedHashMap<>();
        batchEvents.put(-1l, eventsList.get(0)); // id doesn't matter here so use -1
        return EventHelper.eventsAsBatch(batchEvents, userId, appVersion, sessionKey, deviceId);
    }

    public static ArrayList<String> getPushDeliveredEvent(Bundle extras, long time, boolean displayed, String reason) throws Exception {

        if (extras == null || !SwrveHelper.isSwrvePush(extras)) {
            return new ArrayList<>();
        }

        String id = extras.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY);

        Map<String, String> payload = new HashMap<>();
        if (SwrveHelper.isNullOrEmpty(id)) {
            id = extras.getString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY);
            payload.put(GENERIC_EVENT_PAYLOAD_SILENT, String.valueOf(true));
        } else {
            payload.put(GENERIC_EVENT_PAYLOAD_SILENT, String.valueOf(false));
        }
        payload.put(GENERIC_EVENT_PAYLOAD_DISPLAYED, String.valueOf(displayed));
        if (SwrveHelper.isNotNullOrEmpty(reason)) {
            payload.put(GENERIC_EVENT_PAYLOAD_REASON, reason);
        }

        int seqNum = SwrveCommon.getInstance().getNextSequenceNumber();
        ArrayList<String> events = EventHelper.createGenericEvent(time, id, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, GENERIC_EVENT_ACTION_TYPE_DELIVERED, null, null, payload, seqNum);
        return events;
    }

    public static String extractEventFromBatch(String batchEvent) throws Exception {
        String event = "";
        JSONObject batchEventJSONObject = new JSONObject(batchEvent);
        if (batchEventJSONObject.has(BATCH_EVENT_KEY_DATA)) {
            JSONArray eventsJSONArray = batchEventJSONObject.getJSONArray(BATCH_EVENT_KEY_DATA);
            if (eventsJSONArray != null && eventsJSONArray.length() > 0) {
                event = eventsJSONArray.getString(0); // could be more than one event but just returning the first one.
            }
        }
        return event;
    }

    // Send device update (such as token) in the background without affecting DAU/sessions (user_initiated = false)
    protected static void sendUninitiatedDeviceUpdateEvent(Context context, String userId, JSONObject attributes) {
        try {
            ISwrveCommon swrveCommon = SwrveCommon.getInstance();

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("attributes", attributes);
            parameters.put("user_initiated", "false"); // important this is false, hence the name of the method send "Uninitiated"

            int seqnum = swrveCommon.getNextSequenceNumber();
            String event = EventHelper.eventAsJSON("device_update", parameters, null, seqnum, System.currentTimeMillis());
            ArrayList<String> events = new ArrayList<>();
            events.add(event);
            swrveCommon.sendEventsInBackground(context, userId, events);
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK couldn't send uninitiated device_update event.", e);
        }
    }
}
