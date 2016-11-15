package com.swrve.sdk;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.swrve.sdk.adm.SwrveAdmConstants;
import com.swrve.sdk.adm.SwrveAdmNotification;

import java.util.Date;

public class SwrvePushEngageReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "SwrveAdm";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Swrve swrve = (Swrve) SwrveSDK.getInstance();
            processIntent(intent, context, swrve);
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "SwrvePushEngageReceiver. Error processing intent. Intent:" + intent, ex);
        }
    }

    private void processIntent(Intent intent, Context context, Swrve swrve) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty()) {
                Bundle msg = extras.getBundle(SwrveAdmConstants.ADM_BUNDLE);
                if (msg != null) {
                    // Obtain push id
                    Object rawId = msg.get(SwrveAdmConstants.SWRVE_TRACKING_KEY);
                    String msgId = (rawId != null) ? rawId.toString() : null;
                    if (!SwrveHelper.isNullOrEmpty(msgId)) {
                        SwrveLogger.d(LOG_TAG, "Found ADM engaged event:" + msgId);
                        SwrveSDK.sendPushEngagedEvent(context, msgId);

                        if (msg.containsKey(SwrveAdmConstants.DEEPLINK_KEY)) {
                            openDeeplink(msg, context);
                        } else {
                            openActivity(msg, context);
                        }

                        if (swrve.pushNotificationListener != null) {
                            swrve.pushNotificationListener.onPushNotification(msg);
                        }
                    }
                }
            }
        }
    }

    private void openDeeplink(Bundle msg, Context context) {
        String uri = msg.getString(SwrveAdmConstants.DEEPLINK_KEY);
        SwrveLogger.d(LOG_TAG, "Found ADM deeplink. Will attempt to open:" + uri);
        Bundle msgBundleCopy = new Bundle(msg); // make copy of extras and remove any that have been handled
        msgBundleCopy.remove(SwrveAdmConstants.SWRVE_TRACKING_KEY);
        msgBundleCopy.remove(SwrveAdmConstants.DEEPLINK_KEY);
        SwrveIntentHelper.openDeepLink(context, uri, msgBundleCopy);
    }

    private void openActivity(Bundle msg, Context context) {
        Intent intent = null;
        try {
            intent = getActivityIntent(msg, context);
            PendingIntent pi = PendingIntent.getActivity(context, generateTimestampId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            pi.send();
        } catch (PendingIntent.CanceledException e) {
            SwrveLogger.e(LOG_TAG, "SwrvePushEngageReceiver. Could open activity with intent:" + intent, e);
        }
    }

    private Intent getActivityIntent(Bundle msg, Context context) {
        Intent intent = null;
        Class<?> clazz = SwrveAdmNotification.getInstance(context).getActivityClass();
        if (clazz != null) {
            intent = new Intent(context, clazz);
            intent.putExtra(SwrveAdmConstants.ADM_BUNDLE, msg);
            intent.setAction("openActivity");
        }
        return intent;
    }

    private int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }
}
