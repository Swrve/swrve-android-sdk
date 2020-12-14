package com.swrve.sdk;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SwrveBackgroundEventSenderWorker extends Worker {

    public SwrveBackgroundEventSenderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Result workResult = Result.success();
        try {
            SwrveLogger.i("SwrveSDK: SwrveBackgroundEventSenderWorker started.");
            final Swrve swrve = (Swrve) SwrveSDKBase.getInstance();
            SwrveBackgroundEventSender sender = new SwrveBackgroundEventSender(swrve, getApplicationContext());
            sender.handleSendEvents(getInputData());
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: SwrveBackgroundEventSenderWorker exception.", ex);
            workResult = Result.failure();
        }
        return workResult;
    }
}
