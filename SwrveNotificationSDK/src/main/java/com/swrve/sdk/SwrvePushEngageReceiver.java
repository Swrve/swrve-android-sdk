package com.swrve.sdk;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SwrvePushEngageReceiver extends BroadcastReceiver {
    private static final String TAG = "SwrveNotification";

    private Context context;
    private ISwrveCommon swrveCommon;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            this.context = context;
            swrveCommon = SwrveCommon.getInstance();
            processIntent(intent);
        } catch (Exception ex) {
            SwrveLogger.e(TAG, "SwrvePushEngageReceiver. Error processing intent. Intent:" + intent, ex);
        }
    }

    private void processIntent(Intent intent) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty()) {
                Bundle msg = extras.getBundle(SwrveNotificationConstants.NOTIFICATION_BUNDLE);
                //if (msg != null && SwrveSDK.getConfig().isPushEnabled()) {
                if (msg != null && SwrveNotificationImp.getInstance().isPushEnabled()) {
                    // Obtain push id
                    Object rawId = msg.get(SwrveNotificationConstants.SWRVE_TRACKING_KEY);
                    String msgId = (rawId != null) ? rawId.toString() : null;
                    if (!SwrveHelper.isNullOrEmpty(msgId)) {
                        String eventName = "Swrve.Messages.Push-" + msgId + ".engaged";
                        SwrveLogger.d(TAG, "Notification engaged, sending event:" + eventName);
                        sendEngagedEvent(eventName);
                        if (msg.containsKey(SwrveNotificationConstants.DEEPLINK_KEY)) {
                            openDeeplink(msg);
                        } else {
                            openActivity(msg);
                        }

                        SwrveNotificationImp imp = SwrveNotificationImp.getInstance();
                        if (imp != null) {
                            imp.onPushEngaged(msg);
                        }
                    }
                }
            }
        }
    }

    private void sendEngagedEvent(String name) {
        String eventString = "";
        try {
            ArrayList<String> events = new ArrayList<>();
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", name);
            eventString = EventHelper.eventAsJSON("event", parameters, swrveCommon.getNextSequenceNumber());
            events.add(eventString);
            swrveCommon.sendEventsWakefully(context, events);
        } catch (JSONException e) {
            SwrveLogger.e(TAG, "SwrvePushEngageReceiver. Could not send the engaged event:" + eventString, e);
        }
    }

    private void openDeeplink(Bundle msg) {
        String uri = msg.getString(SwrveNotificationConstants.DEEPLINK_KEY);
        SwrveLogger.d(TAG, "Found Notification deeplink. Will attempt to open:" + uri);
        Bundle msgBundleCopy = new Bundle(msg); // make copy of extras and remove any that have been handled
        msgBundleCopy.remove(SwrveNotificationConstants.SWRVE_TRACKING_KEY);
        msgBundleCopy.remove(SwrveNotificationConstants.DEEPLINK_KEY);
        SwrveIntentHelper.openDeepLink(context, uri, msgBundleCopy);
    }

    private void openActivity(Bundle msg) {
        Intent intent = null;
        try {
            intent = getActivityIntent(msg);
            PendingIntent pi = PendingIntent.getActivity(context, generateTimestampId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            pi.send();
        } catch (PendingIntent.CanceledException e) {
            SwrveLogger.e(TAG, "SwrvePushEngageReceiver. Could open activity with intent:" + intent, e);
        }
    }

    private Intent getActivityIntent(Bundle msg) {
        Intent intent = null;
        Class<?> clazz = SwrveNotification.getInstance(context).getActivityClass();
        if (clazz != null) {
            intent = new Intent(context, clazz);
            intent.putExtra(SwrveNotificationConstants.NOTIFICATION_BUNDLE, msg);
            intent.setAction("openActivity");
        }
        return intent;
    }

    private int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }
}

