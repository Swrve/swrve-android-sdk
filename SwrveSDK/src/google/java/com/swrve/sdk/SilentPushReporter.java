package com.swrve.sdk;

import android.content.Context;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SilentPushReporter {
    protected static final String TAG = "SilentPushReporter";

    public void sendReceivedEvent(Context context, String trackingId) {
        ArrayList<String> impressionEvents = new ArrayList<String>();
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id", trackingId);
        try {
            impressionEvents.add(EventHelper.eventAsJSON("silent_push_campaign_received", parameters, null, null));
        } catch (JSONException e) {
            SwrveLogger.e(TAG, "SilentPushReporter. Could not send the silent push received event", e);
        }
        if (SwrveCommon.getInstance() != null) {
            SwrveCommon.getInstance().sendEventsWakefully(context, impressionEvents);
        }
    }
}
