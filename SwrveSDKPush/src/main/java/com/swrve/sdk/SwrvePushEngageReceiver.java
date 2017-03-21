package com.swrve.sdk;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Date;

public class SwrvePushEngageReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "SwrvePush";

    private Context context;
    private SwrvePushSDK pushSDK;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            this.context = context;
            this.pushSDK = SwrvePushSDK.getInstance();
            if(pushSDK == null) {
                SwrveLogger.e(LOG_TAG, "SwrvePushSDK is null");
            } else {
                processIntent(intent);
            }
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "SwrvePushEngageReceiver. Error processing intent. Intent:" + intent, ex);
        }
    }

    private void processIntent(Intent intent) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty()) {
                Bundle msg = extras.getBundle(SwrvePushConstants.PUSH_BUNDLE);
                if (msg != null) {
                    // Obtain push id
                    Object rawId = msg.get(SwrvePushConstants.SWRVE_TRACKING_KEY);
                    String msgId = (rawId != null) ? rawId.toString() : null;
                    if (!SwrveHelper.isNullOrEmpty(msgId)) {
                        SwrveLogger.d(LOG_TAG, "Found engaged event:" + msgId);
                        SwrveEngageEventSender.sendPushEngagedEvent(context, msgId);

                        if(msg.containsKey(SwrvePushConstants.DEEPLINK_KEY)) {
                            openDeeplink(msg);
                        } else {
                            openActivity(msg);
                        }

                        if (pushSDK.getPushNotificationListener() != null) {
                            pushSDK.getPushNotificationListener().onPushNotification(msg);
                        }
                    }
                }
            }
        }
    }

    private void openDeeplink(Bundle msg) {
        String uri = msg.getString(SwrvePushConstants.DEEPLINK_KEY);
        SwrveLogger.d(LOG_TAG, "Found push deeplink. Will attempt to open:" + uri);
        Bundle msgBundleCopy = new Bundle(msg); // make copy of extras and remove any that have been handled
        msgBundleCopy.remove(SwrvePushConstants.SWRVE_TRACKING_KEY);
        msgBundleCopy.remove(SwrvePushConstants.DEEPLINK_KEY);
        SwrveIntentHelper.openDeepLink(context, uri, msgBundleCopy);
    }

    private void openActivity(Bundle msg) {
        Intent intent = null;
        try {
            intent = getActivityIntent(msg);
            PendingIntent pi = PendingIntent.getActivity(context, generateTimestampId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            pi.send();
        } catch (PendingIntent.CanceledException e) {
            SwrveLogger.e(LOG_TAG, "SwrvePushEngageReceiver. Could open activity with intent:" + intent, e);
        }
    }

    private Intent getActivityIntent(Bundle msg) {
        Intent intent = null;
        Class<?> clazz = SwrvePushNotificationConfig.getInstance(context).getActivityClass();
        if (clazz != null) {
            intent = new Intent(context, clazz);
            intent.putExtra(SwrvePushConstants.PUSH_BUNDLE, msg);
            intent.setAction("openActivity");
        }
        return intent;
    }

    private int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }
}
