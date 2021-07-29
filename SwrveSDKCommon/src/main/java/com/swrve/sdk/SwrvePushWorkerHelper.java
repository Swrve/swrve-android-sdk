package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Map;

public class SwrvePushWorkerHelper {

    private final Context context;
    private final Class<? extends ListenableWorker> workerClass;
    protected final Data inputData;
    protected OneTimeWorkRequest workRequest; // exposed for testing
    protected boolean isSwrvePush;

    public SwrvePushWorkerHelper(Context context, Class<? extends ListenableWorker> workerClass, Map<String, String> mapData) {
        this.context = context;
        this.workerClass = workerClass;
        Data.Builder builder = new Data.Builder();
        if (mapData != null) {
            for (String key : mapData.keySet()) {
                builder.putString(key, mapData.get(key));
                checkIsSwrvePush(key, mapData.get(key));
            }
        }
        inputData = builder.build();
    }

    public SwrvePushWorkerHelper(Context context, Class<? extends ListenableWorker> workerClass, Bundle bundle) {
        this.context = context;
        this.workerClass = workerClass;
        Data.Builder builder = new Data.Builder();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object object = bundle.get(key);
                if (object instanceof String) {
                    builder.putString(key, (String) object);
                    checkIsSwrvePush(key, (String) object);
                } else {
                    SwrveLogger.w("SwrveSDK: SwrvePushWorkerHelper found non string type object in bundle..");
                }
            }
        }
        inputData = builder.build();
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
            if (isSwrvePush) {
                workRequest = new OneTimeWorkRequest.Builder(workerClass)
                        .setInputData(inputData)
                        .build();
                enqueueWorkRequest(context, workRequest);
                handled = true;
                SwrveLogger.i("SwrveSDK: Swrve push worker queued with data via SwrvePushWorkerHelper.");
            } else {
                SwrveLogger.i("SwrveSDK: Swrve will not handle this push because it is not a swrve push.");
            }
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: Error trying to queue SwrvePushWorkerHelper.", ex);
        }
        return handled;
    }

    synchronized void enqueueWorkRequest(Context context, OneTimeWorkRequest workRequest) {
        WorkManager.getInstance(context).enqueue(workRequest);
    }
}
