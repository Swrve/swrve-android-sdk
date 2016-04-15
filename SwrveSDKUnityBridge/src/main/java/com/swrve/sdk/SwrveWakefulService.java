package com.swrve.sdk;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.swrve.sdk.EventHelper;
import com.swrve.sdk.IPostBatchRequestListener;
import com.swrve.sdk.ISwrveCommon;
import com.swrve.sdk.SwrveCommon;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.localstorage.ILocalStorage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTClient;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class SwrveWakefulService extends IntentService {

    private static final String LOG_TAG = "SwrveWakeful";
    public static final String EXTRA_EVENTS = "swrve_wakeful_events";

    private ISwrveCommon swrveCommon = SwrveCommon.getInstance();

    public SwrveWakefulService() {
        super("SwrveWakefulService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ArrayList<String> eventsExtras = intent.getExtras().getStringArrayList(EXTRA_EVENTS);
        if (eventsExtras != null && eventsExtras.size() > 0) {
            sendEvents(eventsExtras);
        } else {
            SwrveLogger.e(LOG_TAG, "SwrveWakefulService: Unknown intent received.");
        }

        SwrveWakefulReceiver.completeWakefulIntent(intent);
    }

    void sendEvents(ArrayList<String> events) {
        SwrveLogger.i(LOG_TAG, "Sending queued events");
        try {
            LinkedHashMap<Long, String> eventsMap = new LinkedHashMap<>();
            for(int i = 0; i < events.size(); i++) {
                eventsMap.put((long)i, events.get(i));
            }
            RESTClient rc = new RESTClient(swrveCommon.getHttpTimeout());
            String postData = EventHelper.eventsAsBatch(eventsMap, swrveCommon.getUserId(), swrveCommon.getAppVersion(), swrveCommon.getSessionKey(), swrveCommon.getDeviceId());
            IPostBatchRequestListener pbrl = new IPostBatchRequestListener() {
                public void onResponse(boolean shouldDelete) {
                    SwrveLogger.d(LOG_TAG, "sendEventsAsBatch response, should delete: " + shouldDelete);
                }
            };
            postBatchRequest(rc, postData, pbrl);
        } catch (JSONException je) {
            SwrveLogger.e(LOG_TAG, "Unable to generate event batch, and send events", je);
        }
    }

    private void postBatchRequest(RESTClient restClient, final String postData, final IPostBatchRequestListener listener) {
        restClient.post(SwrveCommon.getInstance().getBatchURL(), postData, new IRESTResponseListener() {
            @Override
            public void onResponse(RESTResponse response) {
                boolean deleteEvents = true;
                if (SwrveHelper.userErrorResponseCode(response.responseCode)) {
                    SwrveLogger.e(LOG_TAG, "Error sending events to Swrve: " + response.responseBody);
                } else if (SwrveHelper.successResponseCode(response.responseCode)) {
                    SwrveLogger.i(LOG_TAG, "Events sent to Swrve");
                } else if (SwrveHelper.serverErrorResponseCode(response.responseCode)) {
                    deleteEvents = false;
                    SwrveLogger.e(LOG_TAG, "Error sending events to Swrve: " + response.responseBody);
                }

                // Resend if we got a server error (5XX)
                listener.onResponse(deleteEvents);
            }

            @Override
            public void onException(Exception ex) {
                SwrveLogger.e(LOG_TAG, "Error posting batch of events. postData:" + postData, ex);
            }
        });
    }
}
