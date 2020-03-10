package com.swrve.sdk;

import android.os.Bundle;
import org.json.JSONObject;
import java.util.Set;

import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.PUSH_INTERNAL_KEYS;
import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY;

public class SwrvePushServiceHelper {

    public static String getPayload(final Bundle msg) {
        String payload = "";
        String jsonPayload = msg.getString(SWRVE_NESTED_JSON_PAYLOAD_KEY);
        // Try and clean the bundle keys so that only the custom properties (and no internal ones) are added to the custom filter json param
        try {
            JSONObject jsonObject = (jsonPayload != null) ? new JSONObject(jsonPayload) : new JSONObject();

            // Even with the msg as "final Bundle msg" we do need to clone it or it will remove keys from msg as well.
            Set<String> msgKeySet = ((Bundle)msg.clone()).keySet();
            msgKeySet.removeAll(PUSH_INTERNAL_KEYS);

            for (String key : msgKeySet) {
                // Do not overwrite key if already present
                if (!jsonObject.has(key)) {
                    jsonObject.put(key, msg.get(key));
                }
            }
            payload = jsonObject.toString();
        } catch (Exception ex) {
            SwrveLogger.e("Error getting json payload.", ex);
        }
        return payload;
    }
}
