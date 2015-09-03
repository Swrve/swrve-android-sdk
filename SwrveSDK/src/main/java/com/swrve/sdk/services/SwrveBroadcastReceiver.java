package com.swrve.sdk.services;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.RequiresPermission;
import android.support.v4.content.WakefulBroadcastReceiver;

public class SwrveBroadcastReceiver extends WakefulBroadcastReceiver {

    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, SwrveIntentService.class);
        service.putExtras(intent);
        startWakefulService(context, service);
    }
}