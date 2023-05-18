package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_BUTTON_TEXT;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;

import com.swrve.sdk.notifications.model.SwrveNotification;
import com.swrve.sdk.notifications.model.SwrveNotificationButton;

import org.json.JSONObject;

import java.util.Date;
import java.util.Map;

class SwrveNotificationEngage {

    private final Context context;

    SwrveNotificationEngage(Context context) {
        this.context = context;
    }

    void processIntent(Intent intent) {

        if (intent == null || intent.getExtras() == null || intent.getExtras().isEmpty()) {
            return;
        }

        try {
            Bundle extras = intent.getExtras();
            Bundle pushBundle = extras.getBundle(SwrveNotificationConstants.PUSH_BUNDLE);
            if (pushBundle == null) {
                return;
            }
            Object rawId = pushBundle.get(SwrveNotificationConstants.SWRVE_TRACKING_KEY);
            String msgId = (rawId != null) ? rawId.toString() : null;
            if (SwrveHelper.isNullOrEmpty(msgId)) {
                return;
            }

            new SwrveCampaignInfluence().removeInfluenceCampaign(context, msgId); // Clear the influence data for this push

            // Cannot send any event if the SDK is not started
            String contextId = extras.getString(SwrveNotificationConstants.CONTEXT_ID_KEY);
            if (SwrveHelper.isNotNullOrEmpty(contextId)) {
                sendButtonEngagedEvent(extras, pushBundle, msgId, contextId);
            } else {
                setNotificationSwrveCampaignIdFromPayload(pushBundle);
                sendEngagedEvent(extras, pushBundle, msgId);
            }

            executeCustomNotificationListener(pushBundle);

        } catch (Exception e) {
            SwrveLogger.e("SwrveNotificationEngage.processIntent", e);
        }
    }

    private void sendButtonEngagedEvent(Bundle extras, Bundle pushBundle, String id, String contextId) throws Exception {
        SwrveLogger.d("SwrveSDK: Found engaged event: %s, with contextId: %s", id, contextId);
        String campaignType = extras.getString(SwrveNotificationConstants.CAMPAIGN_TYPE);
        Bundle eventPayload = extras.getBundle(SwrveNotificationConstants.EVENT_PAYLOAD);
        Map<String, String> eventPayloadMap = SwrveHelper.getBundleAsMap(eventPayload);

        // send engaged event
        EventHelper.sendEngagedEvent(context, campaignType, id, eventPayloadMap);

        // send button click event
        String buttonText = extras.getString(SwrveNotificationConstants.BUTTON_TEXT_KEY);
        eventPayloadMap.put(GENERIC_EVENT_PAYLOAD_BUTTON_TEXT, buttonText);
        EventHelper.sendButtonClickEvent(context, campaignType, id, contextId, eventPayloadMap);

        SwrveNotificationButton.ActionType type = (SwrveNotificationButton.ActionType) extras.get(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY);
        switch (type) {
            case OPEN_URL:
                openDeeplink(pushBundle, extras.getString(SwrveNotificationConstants.PUSH_ACTION_URL_KEY));
                break;
            case OPEN_APP:
                openActivity(pushBundle);
                break;
            case OPEN_CAMPAIGN:
                setNotificationSwrveCampaignIdFromButtonAction(extras.getString(SwrveNotificationConstants.PUSH_ACTION_URL_KEY));
                openActivity(pushBundle);
                break;
            case DISMISS:
                break;
        }
        int notificationId = extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID);
        closeNotification(notificationId); // Button has been pressed, now close the notification
    }

    private void sendEngagedEvent(Bundle extras, Bundle pushBundle, String msgId) throws Exception {
        SwrveLogger.d("SwrveSDK: Found engaged event: %s", msgId);
        String campaignType = extras.getString(SwrveNotificationConstants.CAMPAIGN_TYPE);
        Bundle eventPayloadBundle = extras.getBundle(SwrveNotificationConstants.EVENT_PAYLOAD);
        Map<String, String> eventPayloadMap = SwrveHelper.getBundleAsMap(eventPayloadBundle);
        EventHelper.sendEngagedEvent(context, campaignType, msgId, eventPayloadMap);
        if (pushBundle.containsKey(SwrveNotificationConstants.DEEPLINK_KEY)) {
            openDeeplink(pushBundle, pushBundle.getString(SwrveNotificationConstants.DEEPLINK_KEY));
        } else {
            openActivity(pushBundle);
        }
    }

    private void setNotificationSwrveCampaignIdFromPayload(Bundle pushBundle) {
        ISwrveCommon swrveCommon = SwrveCommon.getInstance();

        String swrvePushPayload = pushBundle.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY);
        if (SwrveHelper.isNotNullOrEmpty(swrvePushPayload)) {
            SwrveNotification swrveNotification = SwrveNotification.fromJson(swrvePushPayload);
            if (swrveNotification != null) {
                if (swrveNotification.getCampaign() != null) {
                    String swrveCampaignId = swrveNotification.getCampaign().getId();
                    swrveCommon.setNotificationSwrveCampaignId(swrveCampaignId);
                }
            }
        }
    }

    private void setNotificationSwrveCampaignIdFromButtonAction(String campaignId) {
        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        if (SwrveHelper.isNotNullOrEmpty(campaignId)) {
            swrveCommon.setNotificationSwrveCampaignId(campaignId);
        }
    }

    private void executeCustomNotificationListener(Bundle msg) {
        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        SwrvePushNotificationListener listener = swrveCommon.getNotificationListener();
        if (listener != null) {
            JSONObject payload = SwrveHelper.convertPayloadToJSONObject(msg);
            listener.onPushNotification(payload);
        }
    }

    protected void closeNotification(int notificationID) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notificationID);
    }

    private void openDeeplink(Bundle msg, String uri) {
        Bundle msgBundleCopy = new Bundle(msg); // make copy of extras and remove any that have been handled
        msgBundleCopy.remove(SwrveNotificationConstants.SWRVE_TRACKING_KEY);
        msgBundleCopy.remove(SwrveNotificationConstants.DEEPLINK_KEY);
        SwrveIntentHelper.openDeepLink(context, uri, msgBundleCopy);
        closeNotificationBar();
    }

    private void openActivity(Bundle msg) throws PendingIntent.CanceledException {
        Intent intent = getActivityIntent(msg);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, generateTimestampId(), intent, flags);
        pendingIntent.send();
        closeNotificationBar();
    }

    private Intent getActivityIntent(Bundle msg) {
        Intent intent = null;
        Class<?> clazz = getActivityClass();
        if (clazz != null) {
            intent = new Intent(context, clazz);
            intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, msg);
            intent.setAction("openActivity");
        }
        return intent;
    }

    @SuppressLint("MissingPermission")
    private void closeNotificationBar() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // When performing a button action on notifications, you need to close the notification drawer explicitly
            // note, this requires permission android.permission.BROADCAST_CLOSE_SYSTEM_DIALOGS if on 31+
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
        }
    }

    private int generateTimestampId() {
        return (int) (new Date().getTime() % Integer.MAX_VALUE);
    }

    private Class<?> getActivityClass() {
        Class<?> clazz = null;
        SwrveNotificationConfig notificationConfig = SwrveCommon.getInstance().getNotificationConfig();
        if (notificationConfig != null && notificationConfig.getActivityClass() != null) {
            clazz = notificationConfig.getActivityClass();
        } else {
            // If no class configured then use the default launcher activity
            try {
                String activity = null;
                PackageManager packageManager = context.getPackageManager();
                ResolveInfo resolveInfo = packageManager.resolveActivity(packageManager.getLaunchIntentForPackage(context.getPackageName()), PackageManager.MATCH_DEFAULT_ONLY);
                if (resolveInfo != null) {
                    activity = resolveInfo.activityInfo.name;
                    if (activity.startsWith(".")) {
                        activity = context.getPackageName() + activity; // Append application package as it starts with .
                    }
                }
                if (SwrveHelper.isNotNullOrEmpty(activity)) {
                    if (activity.startsWith(".")) {
                        activity = context.getPackageName() + activity; // Append application package as it starts with .
                    }
                    clazz = Class.forName(activity);
                }
            } catch (Exception e) {
                SwrveLogger.e("Exception getting activity class to start when notification is engaged.", e);
            }
        }
        return clazz;
    }
}
