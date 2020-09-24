package com.swrve.sdk;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTClient;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.swrve.sdk.ISwrveCommon.BATCH_EVENT_KEY_DATA;
import static com.swrve.sdk.ISwrveCommon.EVENT_PAYLOAD_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_RUN_NUMBER;

class CampaignDeliveryManager {

    protected static final int REST_CLIENT_TIMEOUT_MILLIS = 30000;
    protected static final int MAX_ATTEMPTS = 3;
    protected static final String KEY_END_POINT = "END_POINT";
    protected static final String KEY_BODY = "BODY";

    private final Context context;

    CampaignDeliveryManager(Context context) {
        this.context = context;
    }

    protected void sendCampaignDelivery(String endpoint, String body) {
        try {
            OneTimeWorkRequest workRequest = getRestWorkRequest(endpoint, body);
            enqueueWorkRequest(context, workRequest);
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: Error trying to queue campaign delivery event.", ex);
        }
    }

    protected OneTimeWorkRequest getRestWorkRequest(final String endpoint, final String body) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Data inputData = new Data.Builder()
                .putString(KEY_END_POINT, endpoint)
                .putString(KEY_BODY, body)
                .build();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SwrveCampaignDeliveryWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build();
        return workRequest;
    }

    // separate method for testing
    protected synchronized void enqueueWorkRequest(Context context, OneTimeWorkRequest workRequest) {
        WorkManager.getInstance(context).enqueue(workRequest);
    }

    protected ListenableWorker.Result post(Data data, int runAttempt) {

        if (runAttempt >= MAX_ATTEMPTS) {
            // Should never actually enter this if block. Only here for safety.
            SwrveLogger.e("SwrveSDK: SwrveCampaignDelivery error. Exit. Attempts to resend campaign delivery has maxed out %s times", MAX_ATTEMPTS);
            return ListenableWorker.Result.failure();
        }

        String endpoint = data.getString(KEY_END_POINT);
        String body = data.getString(KEY_BODY);
        if (SwrveHelper.isNullOrEmpty(endpoint) || SwrveHelper.isNullOrEmpty(body)) {
            SwrveLogger.e("SwrveSDK: SwrveCampaignDelivery error. Exit. Invalid endpoint:%s body:%s", endpoint, body);
            return ListenableWorker.Result.failure();
        }

        int runNumber = runAttempt + 1; // runAttempt is initially zero from worker so increment for checks/logging.
        if(runNumber > 1) {
            body = addRunNumberToPayload(body, runNumber);
        }
        IRESTClient restClient = getRestClient(REST_CLIENT_TIMEOUT_MILLIS);
        SwrveLogger.v("SwrveSDK: runNumber %s, sending campaign delivery post request with body:\n %s", runNumber, body);
        RESTResponseListener restResponseListener = (RESTResponseListener) getRestResponseListener(runNumber, body);
        restClient.post(endpoint, body, restResponseListener);

        return restResponseListener.result;
    }

    private String addRunNumberToPayload(String batchEvent, int runNumber) {
        try {
            String eventJson = EventHelper.extractEventFromBatch(batchEvent);
            JSONObject eventJSONObject = new JSONObject(eventJson);
            if (eventJSONObject.has(EVENT_PAYLOAD_KEY)) { // the campaign delivery event should always have an existing payload
                JSONObject payloadJSONObject = eventJSONObject.getJSONObject(EVENT_PAYLOAD_KEY);
                payloadJSONObject.put(GENERIC_EVENT_PAYLOAD_RUN_NUMBER, runNumber);

                JSONArray eventsJSONArray = new JSONArray();
                eventsJSONArray.put(eventJSONObject);

                JSONObject batchEventJSONObject = new JSONObject(batchEvent);
                batchEventJSONObject.put(BATCH_EVENT_KEY_DATA, eventsJSONArray);

                batchEvent = batchEventJSONObject.toString();
            }

        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK: Exception in addRunNumberToPayload", e);
        }
        return batchEvent;
    }

    protected IRESTClient getRestClient(int timeout) {
        IRESTClient restClient = new RESTClient(timeout);
        return restClient;
    }

    protected IRESTResponseListener getRestResponseListener(int runNumber, String batchEvent) {
        return new RESTResponseListener(runNumber, batchEvent);
    }

    class RESTResponseListener implements IRESTResponseListener {

        final int runNumber;
        final String batchEvent;

        ListenableWorker.Result result = ListenableWorker.Result.failure(); // default to failure

        RESTResponseListener(int runNumber, String batchEvent) {
            this.runNumber = runNumber;
            this.batchEvent = batchEvent;
        }

        @Override
        public void onResponse(RESTResponse response) {
            if (SwrveHelper.successResponseCode(response.responseCode)) {
                SwrveLogger.i("SwrveSDK:PushDelivery event sent to Swrve");
                result = ListenableWorker.Result.success();
                sendQaEvent(batchEvent);
            } else {
                SwrveLogger.e("SwrveSDK:Error sending PushDelivery event to Swrve. responseCode: %s\tresponseBody:%s", response.responseCode, response.responseBody);
                if (SwrveHelper.userErrorResponseCode(response.responseCode)) {
                    result = ListenableWorker.Result.failure();
                } else if (SwrveHelper.serverErrorResponseCode(response.responseCode)) {
                    if (runNumber >= MAX_ATTEMPTS) {
                        SwrveLogger.e("SwrveSDK: Attempts to resend campaign delivery has maxed out %s times. No more retries.", MAX_ATTEMPTS);
                        result = ListenableWorker.Result.failure();
                        saveEvent(batchEvent, runNumber + 1); // increment runNumber
                    } else {
                        SwrveLogger.i("SwrveSDK: Will retry sending campaign delivery. runNumber:%s", runNumber);
                        result = ListenableWorker.Result.retry();
                    }
                }
            }
        }

        @Override
        public void onException(Exception ex) {
            SwrveLogger.e("SwrveSDK: Error sending post request for campaign delivery event.", ex);
        }
    }

    protected void sendQaEvent(String batchEvent) {
        try {
            List events = new ArrayList();
            events.add(EventHelper.extractEventFromBatch(batchEvent));
            QaUser.wrappedEvents(events);
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK: Exception sending QA campaign delivery wrapped event.", e);
        }
    }

    protected void saveEvent(String batchEvent, int runNumber) {
        try {
            ISwrveCommon swrveCommon = SwrveCommon.getInstance();
            batchEvent = addRunNumberToPayload(batchEvent, runNumber); // add the run number so we know its sent with regular events when app is opened.
            String eventJson = EventHelper.extractEventFromBatch(batchEvent);
            swrveCommon.saveEvent(eventJson);
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK: Exception saving campaign delivery event to storage.", e);
        }
    }
}
