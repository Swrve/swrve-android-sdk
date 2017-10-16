package com.swrve.sdk;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.swrve.sdk.model.PushPayloadButton;

import java.util.Date;

public class SwrvePushEngageReceiver extends BroadcastReceiver {

    private Context context;
    private SwrvePushSDK pushSDK;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            this.context = context;
            this.pushSDK = SwrvePushSDK.getInstance();
            if(pushSDK == null) {
                SwrveLogger.e("SwrvePushSDK is null");
            } else {
                processIntent(intent);
            }
        } catch (Exception ex) {
            SwrveLogger.e("SwrvePushEngageReceiver. Error processing intent. Intent: %s", intent.toString(), ex);
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
                        // Clear the influence data for this push
                        SwrvePushSDK.removeInfluenceCampaign(context, msgId);

                        String actionKey = extras.getString(SwrvePushConstants.PUSH_ACTION_KEY);
                        if (SwrveHelper.isNotNullOrEmpty(actionKey)) {
                            SwrveLogger.d("Found engaged event: %s, with actionId: %s", (Object)msgId, actionKey);
                            // Send events and resolve the button action
                            SwrveEngageEventSender.sendPushButtonEngagedEvent(context, msgId, actionKey, extras.getString(SwrvePushConstants.PUSH_ACTION_TEXT));
                            PushPayloadButton.ActionType type = (PushPayloadButton.ActionType) extras.get(SwrvePushConstants.PUSH_ACTION_TYPE_KEY);
                            switch (type) {
                                case OPEN_URL:
                                    openDeeplink(msg, extras.getString(SwrvePushConstants.PUSH_ACTION_URL_KEY));
                                    break;
                                case OPEN_APP:
                                    openActivity(msg);
                                    break;
                                case DISMISS:
                                    //Every action closes the notification
                                    break;
                            }

                            // Button has been pressed, now close the notification
                            closeNotification(extras.getInt(SwrvePushConstants.PUSH_NOTIFICATION_ID));
                        } else {
                            SwrveLogger.d("Found engaged event: %s", (Object)msgId);
                            SwrveEngageEventSender.sendPushEngagedEvent(context, msgId);
                            if(msg.containsKey(SwrvePushConstants.DEEPLINK_KEY)) {
                                openDeeplink(msg, msg.getString(SwrvePushConstants.DEEPLINK_KEY));
                            } else {
                                openActivity(msg);
                            }
                        }

                        if (pushSDK.getPushNotificationListener() != null) {
                            pushSDK.getPushNotificationListener().onPushNotification(msg);
                        }
                    }
                }
            }
        }
    }

    protected void closeNotification(int notificationID) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notificationID);
    }

    private void openDeeplink(Bundle msg, String uri) {
        SwrveLogger.d("Found push deeplink. Will attempt to open: %s", (Object)uri);
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

            // Close the notification bar
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
        } catch (PendingIntent.CanceledException e) {
            SwrveLogger.e("SwrvePushEngageReceiver. Could open activity with intent: %s", intent.toString(), e);
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
