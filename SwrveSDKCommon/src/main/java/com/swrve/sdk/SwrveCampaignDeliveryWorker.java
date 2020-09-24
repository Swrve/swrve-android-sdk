package com.swrve.sdk;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SwrveCampaignDeliveryWorker extends Worker {

    public SwrveCampaignDeliveryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        ListenableWorker.Result workResult = ListenableWorker.Result.failure();
        try {
            SwrveLogger.i("SwrveSDK: SwrveCampaignDeliveryWorker started.");
            CampaignDeliveryManager manager = new CampaignDeliveryManager(getApplicationContext());
            workResult = manager.post(getInputData(), getRunAttemptCount());
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: SwrveCampaignDeliveryWorker exception.", ex);
        }
        return workResult;
    }
}
