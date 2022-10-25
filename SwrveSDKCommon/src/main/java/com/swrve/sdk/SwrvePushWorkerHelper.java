package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_MSG_ID;
import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.SWRVE_TRACKING_KEY;
import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY;

import android.content.Context;
import android.os.Bundle;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.HashMap;
import java.util.Map;

public class SwrvePushWorkerHelper {

    private final Context context;
    private final Class<? extends ListenableWorker> workerClass;
    private Map<String, String> mapData;
    private Bundle bundle;
    private OneTimeWorkRequest workRequest;
    private boolean isSwrvePush;

    public SwrvePushWorkerHelper(Context context, Class<? extends ListenableWorker> workerClass, Map<String, String> mapData) {
        this.context = context;
        this.workerClass = workerClass;
        this.mapData = mapData;
    }

    public SwrvePushWorkerHelper(Context context, Class<? extends ListenableWorker> workerClass, Bundle bundle) {
        this.context = context;
        this.workerClass = workerClass;
        this.bundle = bundle;
    }

    private void checkIsSwrvePush(String key, String value) {
        if (SwrveHelper.isNotNullOrEmpty(key) && SwrveHelper.isNotNullOrEmpty(value)) {
            if (key.equalsIgnoreCase(SwrveNotificationConstants.SWRVE_TRACKING_KEY) || key.equalsIgnoreCase(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY)) {
                isSwrvePush = true;
            }
        }
    }

    public boolean handle() {
        SwrveLogger.i("SwrveSDK: Attempt to handle push message via SwrvePushWorkerHelper.");
        boolean handled = false;
        try {
            // call resolveData first because it will set global variables such as isSwrvePush
            Data inputData = resolveData();
            if (!isSwrvePush) {
                SwrveLogger.i("SwrveSDK: Swrve will not handle this push because it is not a swrve push.");
            } else if (SwrvePushSidDeDuper.isDupe(context, mapData)) {
                SwrveLogger.i("SwrveSDK Received Push: but not processing as _sid has been processed before.");
            } else {
                String uniqueWorkName = getUniqueWorkName(inputData);
                workRequest = new OneTimeWorkRequest.Builder(workerClass)
                        .setInputData(inputData)
                        .build();
                enqueueUniqueWorkRequest(context, uniqueWorkName, workRequest);
                handled = true;
                SwrveLogger.i("SwrveSDK: Swrve push worker queued with data via SwrvePushWorkerHelper.");
            }
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: Error trying to queue SwrvePushWorkerHelper.", ex);
        }
        return handled;
    }

    protected Data resolveData() {
        Data.Builder builder = new Data.Builder();
        if (mapData != null) {
            for (String key : mapData.keySet()) {
                builder.putString(key, mapData.get(key));
                checkIsSwrvePush(key, mapData.get(key));
            }
        } else if (bundle != null) {
            mapData = new HashMap<>();
            for (String key : bundle.keySet()) {
                Object object = bundle.get(key);
                if (object instanceof String) {
                    builder.putString(key, (String) object);
                    checkIsSwrvePush(key, (String) object);
                    mapData.put(key, (String) object);
                } else {
                    SwrveLogger.w("SwrveSDK: SwrvePushWorkerHelper found non string type object in bundle..");
                }
            }
        }
        return builder.build();
    }

    private String getUniqueWorkName(Data inputData) {
        String uniqueWorkName = inputData.getString(SWRVE_UNIQUE_MESSAGE_ID_KEY);
        if (SwrveHelper.isNullOrEmpty(uniqueWorkName) && inputData.hasKeyWithValueOfType(GENERIC_EVENT_PAYLOAD_MSG_ID, String.class)) {
            uniqueWorkName = inputData.getString(GENERIC_EVENT_PAYLOAD_MSG_ID);
        }
        if (SwrveHelper.isNullOrEmpty(uniqueWorkName)) {
            uniqueWorkName = inputData.getString(SWRVE_TRACKING_KEY);
        }
        uniqueWorkName = "SwrvePushWorkerHelper_" + uniqueWorkName; // add prefix
        return uniqueWorkName;
    }

    protected void enqueueUniqueWorkRequest(Context context, String uniqueWorkName, OneTimeWorkRequest workRequest) {
        WorkManager.getInstance(context).enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.KEEP, workRequest);
    }
}
