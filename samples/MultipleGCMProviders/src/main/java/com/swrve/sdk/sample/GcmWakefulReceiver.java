package com.swrve.sdk.sample;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class GcmWakefulReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, GcmWakefulIntentService.class);
        service.putExtras(intent.getExtras());
        startWakefulService(context, service);
    }
}
