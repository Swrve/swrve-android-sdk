package com.swrve.sdk;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.swrve.sdk.ISwrveCommon.EVENT_ID_KEY;
import static com.swrve.sdk.ISwrveCommon.EVENT_PAYLOAD_KEY;
import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_ENGAGED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_ID_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_GEO;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CONTEXT_ID_KEY;
import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN;

/**
 * Used internally to generate JSON batch strings from event data.
 */
final class EventHelper {
    private static final Object BATCH_API_VERSION = "3";

    public static String eventAsJSON(String type, Map<String, Object> parameters, int seqnum, long time) throws JSONException {
        return eventAsJSON(type, parameters, null, seqnum, time);
    }

    public static ArrayList<String> createGenericEvent(String id, String campaignType, String actionType, String contextId, String campaignId, Map<String, String> payload, int seqnum) throws JSONException {
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
        String eventAsJSON = EventHelper.eventAsJSON(EVENT_TYPE_GENERIC_CAMPAIGN, parameters, payload, seqnum, System.currentTimeMillis());
        events.add(eventAsJSON);
        return events;
    }

    /*
     * Generate JSON to be stored in EventLocalStorage and eventually sent to
     * the batch API to inform Swrve of these events.
     */
    public static String eventAsJSON(String type, Map<String, Object> parameters, Map<String, String> payload, int seqnum, long time) throws JSONException {
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

    public static String qaLogEventAsJSON(int seqnum, long time, String logSource, String logType, String logDetails) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("seqnum", seqnum);
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
        batch.put("user", userId);
        batch.put("session_token", sessionToken);
        batch.put("version", BATCH_API_VERSION);
        batch.put("app_version", appVersion);
        batch.put("unique_device_id", deviceId);
        batch.put("data", orderedMapToJSONArray(events));
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
}
