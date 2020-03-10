package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;
import androidx.legacy.content.WakefulBroadcastReceiver;

@SuppressWarnings("deprecation")
public class SwrvePushServiceDefaultReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, SwrvePushServiceDefaultJobIntentService.class);
        service.putExtras(intent.getExtras());
        startWakefulService(context, service);
    }
}
