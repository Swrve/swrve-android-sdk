package com.swrve.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SwrveEngageEventSender extends BroadcastReceiver {

    private Context context;

    public static void sendPushEngagedEvent(Context context, String pushId) {
        Intent intent = new Intent(context, SwrveEngageEventSender.class);
        intent.putExtra(SwrvePushConstants.SWRVE_TRACKING_KEY, pushId);
        context.sendBroadcast(intent);
    }

    public static void sendPushButtonEngagedEvent(Context context, String pushId, String actionId, String actionText) {
        Intent intent = new Intent(context, SwrveEngageEventSender.class);
        intent.putExtra(SwrvePushConstants.SWRVE_TRACKING_KEY, pushId);
        intent.putExtra(SwrvePushConstants.PUSH_ACTION_KEY, actionId);
        intent.putExtra(SwrvePushConstants.PUSH_ACTION_TEXT, actionText);
        context.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            this.context = context;
            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty()) {
                Object rawId = extras.get(SwrvePushConstants.SWRVE_TRACKING_KEY);
                String msgId = (rawId != null) ? rawId.toString() : null;
                if (!SwrveHelper.isNullOrEmpty(msgId)) {
                    String actionId = extras.getString(SwrvePushConstants.PUSH_ACTION_KEY);
                    if (SwrveHelper.isNotNullOrEmpty(actionId)) {
                        SwrveLogger.d("SwrveEngageEventSender: Sending push button_click for push id:%s and actionId:%s", msgId, actionId);
                        String actionText = extras.getString(SwrvePushConstants.PUSH_ACTION_TEXT);
                        if (SwrveHelper.isNotNullOrEmpty(actionText)) {
                            sendButtonClickEvent(msgId, actionId, actionText);
                        }
                    }

                    String eventName = "Swrve.Messages.Push-" + msgId + ".engaged";
                    SwrveLogger.d("SwrveEngageEventSender: Sending engaged event: %s", eventName);
                    sendEngagedEvent(eventName);
                }
            }
        } catch (Exception ex) {
            SwrveLogger.e("SwrveEngageEventSender. Error sending engaged event. Intent: %s", ex, intent.toString());
        }
    }

    private void sendEngagedEvent(String name) throws JSONException {
        ISwrveCommon swrve = SwrveCommon.getInstance();
        ArrayList<String> events = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", name);
        String eventString = EventHelper.eventAsJSON("event", parameters, swrve.getNextSequenceNumber(), System.currentTimeMillis());
        events.add(eventString);
        swrve.sendEventsInBackground(context, swrve.getUserId(), events);
    }

    // Also invoked by Unity
    private void sendButtonClickEvent(String msgId, String actionId, String actionText) throws JSONException {
        SwrveCommon.checkInstanceCreated(); // throws RuntimeException

        ISwrveCommon swrve = SwrveCommon.getInstance();
        if (swrve != null) {
            ArrayList<String> events = new ArrayList<>();
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("id", msgId);
            parameters.put("campaignType", "push");
            parameters.put("actionType", "button_click");
            parameters.put("contextId", actionId);
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("buttonText", actionText);
            String eventAsJSON = EventHelper.eventAsJSON("generic_campaign_event", parameters, payload, swrve.getNextSequenceNumber(), System.currentTimeMillis());
            events.add(eventAsJSON);
            swrve.sendEventsInBackground(context, swrve.getUserId(), events);
        } else {
            SwrveLogger.e("No SwrveSDK instance present");
        }
    }
}
