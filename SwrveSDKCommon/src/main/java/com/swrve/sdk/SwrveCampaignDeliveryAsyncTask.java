package com.swrve.sdk;

import android.os.AsyncTask;
import android.os.Bundle;

import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTClient;
import com.swrve.sdk.rest.RESTResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_DELIVERED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;

public class SwrveCampaignDeliveryAsyncTask extends AsyncTask<Object, Void, Void> {

    private final ISwrveCommon swrveCommon = SwrveCommon.getInstance();
    final Bundle extras;

    public SwrveCampaignDeliveryAsyncTask(Bundle extras) {
        this.extras = extras;
    }

    @Override
    protected Void doInBackground(Object[] params) {
        try {
            String endPoint = swrveCommon.getEventsServer() + "/1/batch";
            String batchEvent = getBatchEvent();
            if (SwrveHelper.isNullOrEmpty(batchEvent))  {
                SwrveLogger.e("Error invalid batchEvent");
                return null;
            }
            this.sendPushDelivery(endPoint, batchEvent);
        } catch (Exception ex) {
            SwrveLogger.e(" exception", ex);
        }
        return null;
    }

    protected void sendPushDelivery(final String endPoint, final String batchEvent) {
        IRESTClient restClient = new RESTClient(15000);
        restClient.post(endPoint, batchEvent, new IRESTResponseListener() {
            @Override
            public void onResponse(RESTResponse response) {
                if (SwrveHelper.userErrorResponseCode(response.responseCode)) {
                    SwrveLogger.e("Error sending PushDelivery event to Swrve. responseCode: %s\tresponseBody:%s", response.responseCode, response.responseBody);
                } else if (SwrveHelper.successResponseCode(response.responseCode)) {
                    SwrveLogger.i("PushDelivery event sent to Swrve");
                } else if (SwrveHelper.serverErrorResponseCode(response.responseCode)) {
                    SwrveLogger.e("Error sending PushDelivery event to Swrve");
                }
            }

            @Override
            public void onException(Exception exp) {
                SwrveLogger.e("Error sending push delivery event %s", exp, batchEvent);
            }
        });
    }

    private String getBatchEvent() throws Exception {
        final String userId = swrveCommon.getUserId();
        final String appVersion = swrveCommon.getAppVersion();
        final String sessionKey = swrveCommon.getSessionKey();
        final String deviceId = swrveCommon.getDeviceId();

        LinkedHashMap<Long, String> events = new LinkedHashMap<>();
        String event = getEventData();
        events.put(-1l, event); // id doesn't matter here so use -1
        return EventHelper.eventsAsBatch(events, userId, appVersion, sessionKey, deviceId);
    }

    protected String getEventData() throws Exception {
        String id = extras.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY);
        Map<String, String> payload = new HashMap<>();
        if(SwrveHelper.isNullOrEmpty(id)){
            id = extras.getString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY);
            payload.put("silent", String.valueOf(true));
        } else {
            payload.put("silent", String.valueOf(false));
        }

        if (SwrveHelper.isNullOrEmpty(id)) {
            // we do need a valid pushID to proceed.
            return null;
        }

        int seqNum = swrveCommon.getNextSequenceNumber();
        ArrayList<String> events = EventHelper.createGenericEvent(id, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, GENERIC_EVENT_ACTION_TYPE_DELIVERED, null, null, payload, seqNum);
        return events.get(0);
    }
}