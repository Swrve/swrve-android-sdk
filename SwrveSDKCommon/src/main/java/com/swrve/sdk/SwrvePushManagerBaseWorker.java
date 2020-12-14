package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Map;

public abstract class SwrvePushManagerBaseWorker extends Worker {

    public SwrvePushManagerBaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Result workResult = Result.success();
        try {
            SwrveLogger.i("SwrveSDK: SwrvePushWorker started.");
            SwrvePushManager pushManager = getSwrvePushManager();
            Bundle bundle = convertToBundle(getInputData().getKeyValueMap());
            pushManager.processMessage(bundle);
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: SwrvePushWorker exception.", ex);
            workResult = Result.failure();
        }
        return workResult;
    }

    public abstract SwrvePushManager getSwrvePushManager();

    private Bundle convertToBundle(Map<String, Object> map) {
        Bundle bundle = new Bundle();
        if (map != null) {
            for (String key : map.keySet()) {
                if (map.get(key) instanceof String) {
                    bundle.putString(key, (String) map.get(key));
                }
            }
        }
        return bundle;
    }
}
