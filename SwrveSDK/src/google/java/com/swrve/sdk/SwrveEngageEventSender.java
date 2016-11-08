package com.swrve.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.swrve.sdk.gcm.SwrveGcmConstants;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SwrveEngageEventSender extends BroadcastReceiver {

    private static final String LOG_TAG = "SwrveGcm";
    private Context context;

    /**
     * Called to process the engaged event and send to Swrve
     *
     * @param context android context
     * @param pushId  The push id for engagement
     */
    public static void send(Context context, String pushId) {
        Intent intent = new Intent(context, SwrveEngageEventSender.class);
        intent.putExtra(SwrveGcmConstants.SWRVE_TRACKING_KEY, pushId);
        context.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            this.context = context;
            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty()) {
                Object rawId = extras.get(SwrveGcmConstants.SWRVE_TRACKING_KEY);
                String msgId = (rawId != null) ? rawId.toString() : null;
                if (!SwrveHelper.isNullOrEmpty(msgId)) {
                    String eventName = "Swrve.Messages.Push-" + msgId + ".engaged";
                    SwrveLogger.d(LOG_TAG, "SwrveEngageEventSender: Sending engaged event:" + eventName);
                    sendEngagedEvent(eventName);
                }
            }
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "SwrveEngageEventSender. Error sending engaged event. Intent:" + intent, ex);
        }
    }

    private void sendEngagedEvent(String name) throws JSONException {
        Swrve swrve = (Swrve) SwrveSDK.getInstance();
        ArrayList<String> events = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", name);
        String eventString = EventHelper.eventAsJSON("event", parameters, swrve.getNextSequenceNumber());
        events.add(eventString);
        swrve.sendEventsWakefully(context, events);
    }

}
