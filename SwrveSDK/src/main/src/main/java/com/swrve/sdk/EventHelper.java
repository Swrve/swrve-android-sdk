package com.swrve.sdk;

import com.swrve.sdk.localstorage.MemoryCachedLocalStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Used internally to generate JSON batch strings from event data.
 */
final class EventHelper {
    private static final Object BATCH_API_VERSION = "2";

    private synchronized static short getDeviceId(MemoryCachedLocalStorage storage) {
        String id = storage.getSharedCacheEntry("device_id");
        if (id == null || id.length() <= 0) {
            short deviceId = (short) new Random().nextInt(Short.MAX_VALUE);
            storage.setAndFlushSharedEntry("device_id", Short.toString(deviceId));
            return deviceId;
        } else {
            return Short.parseShort(id);
        }
    }

    private synchronized static int getNextSequenceNumber(MemoryCachedLocalStorage storage) {
        String id = storage.getSharedCacheEntry("seqnum");
        int seqnum = 1;
        if (!SwrveHelper.isNullOrEmpty(id)) {
            seqnum = Integer.parseInt(id) + 1;
        }
        storage.setAndFlushSharedEntry("seqnum", Integer.toString(seqnum));
        return seqnum;
    }

    /*
     * Generate JSON to be stored in EventLocalStorage and eventually sent to
     * the batch API to inform Swrve of these events.
     */
    public static String eventAsJSON(String type, Map<String, Object> parameters,
                                     Map<String, String> payload, MemoryCachedLocalStorage storage) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("time", System.currentTimeMillis());
        obj.put("seqnum", getNextSequenceNumber(storage));

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

    /*
     * Generate JSON in the format expected by the batch API to inform Swrve of
     * these events.
     */
    public static String eventsAsBatch(String userId, String appVersion,
                                       String sessionToken, LinkedHashMap<Long, String> events, MemoryCachedLocalStorage storage)
            throws JSONException {
        JSONObject batch = new JSONObject();
        batch.put("user", userId);
        batch.put("session_token", sessionToken);
        batch.put("version", BATCH_API_VERSION);
        batch.put("app_version", appVersion);
        batch.put("device_id", getDeviceId(storage));
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
        String eventName = "";

        if (eventType.equals("session_start")) {
            eventName = "Swrve.session.start";
        } else if (eventType.equals("session_end")) {
            eventName = "Swrve.session.end";
        } else if (eventType.equals("buy_in")) {
            eventName = "Swrve.buy_in";
        } else if (eventType.equals("iap")) {
            eventName = "Swrve.iap";
        } else if (eventType.equals("event")) {
            eventName = (String) eventParameters.get("name");
        } else if (eventType.equals("purchase")) {
            eventName = "Swrve.user_purchase";
        } else if (eventType.equals("currency_given")) {
            eventName = "Swrve.currency_given";
        } else if (eventType.equals("user")) {
            eventName = "Swrve.user_properties_changed";
        }

        return eventName;
    }
}
