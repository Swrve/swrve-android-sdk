package com.swrve.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Used internally to generate JSON batch strings from event data.
 */
final class EventHelper {
    private static final Object BATCH_API_VERSION = "2";

    public static String eventAsJSON(String type, Map<String, Object> parameters, int seqnum, long time) throws JSONException {
        return eventAsJSON(type, parameters, null, seqnum, time);
    }

    public static ArrayList<String> createGenericEvent(String id, String campaignType, String actionType, String contextId,String campaignId, Map<String, String> payload) throws JSONException {

        ISwrveCommon swrve = SwrveCommon.getInstance();
        if (swrve != null) {

            ArrayList<String> events = new ArrayList<>();
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("id", id);
            parameters.put("campaignType", campaignType);
            parameters.put("actionType", actionType);
            parameters.put("contextId", contextId);
            parameters.put("campaignId", campaignId);
            String eventAsJSON = EventHelper.eventAsJSON("generic_campaign_event", parameters, payload, swrve.getNextSequenceNumber(), System.currentTimeMillis());
            events.add(eventAsJSON);

            return events;
        }

        return null;
    }

    /*
     * Generate JSON to be stored in EventLocalStorage and eventually sent to
     * the batch API to inform Swrve of these events.
     */
    public static String eventAsJSON(String type, Map<String, Object> parameters, Map<String, String> payload, int seqnum, long time) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("time", time);
        if (seqnum > 0) {
            obj.put("seqnum", seqnum);
        }
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                obj.put(entry.getKey(), entry.getValue());
            }
        }

        if (payload != null) {
            obj.put("payload", new JSONObject(payload));
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
    public static String eventsAsBatch(LinkedHashMap<Long, String> events, String userId, String appVersion, String sessionToken, short deviceId) throws JSONException {
        JSONObject batch = new JSONObject();
        batch.put("user", userId);
        batch.put("session_token", sessionToken);
        batch.put("version", BATCH_API_VERSION);
        batch.put("app_version", appVersion);
        batch.put("device_id", deviceId);
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
}
