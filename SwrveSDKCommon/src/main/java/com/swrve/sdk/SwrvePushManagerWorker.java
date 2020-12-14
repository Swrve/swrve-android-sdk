package com.swrve.sdk;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

public class SwrvePushManagerWorker extends SwrvePushManagerBaseWorker {

    public SwrvePushManagerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public SwrvePushManager getSwrvePushManager() {
        return new SwrvePushManagerImp(getApplicationContext());
    }
}
