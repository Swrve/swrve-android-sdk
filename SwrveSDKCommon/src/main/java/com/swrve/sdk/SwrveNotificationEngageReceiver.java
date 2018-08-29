package com.swrve.sdk;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import com.swrve.sdk.notifications.model.SwrveNotification;
import com.swrve.sdk.notifications.model.SwrveNotificationButton;

import org.json.JSONObject;

import java.util.Date;
import java.util.Map;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_BUTTON_TEXT;

public class SwrveNotificationEngageReceiver extends BroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            this.context = context;
            processIntent(intent);
        } catch (Exception ex) {
            SwrveLogger.e("SwrveNotificationEngageReceiver. Error processing intent. Intent: %s", ex, intent.toString());
        }
    }

    private void processIntent(Intent intent) throws Exception {
        if (intent == null) {
            return;
        }
        Bundle extras = intent.getExtras();
        if (extras == null || extras.isEmpty()) {
            return;
        }
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

        String contextId = extras.getString(SwrveNotificationConstants.CONTEXT_ID_KEY);
        if (SwrveHelper.isNotNullOrEmpty(contextId)) {
            sendButtonEngagedEvent(extras, pushBundle, msgId, contextId);
        } else {
            setNotificationSwrveCampaignIdFromPayload(pushBundle);
            sendEngagedEvent(extras, pushBundle, msgId);
        }

        executeCustomNotificationListener(pushBundle);
    }

    private void sendButtonEngagedEvent(Bundle extras, Bundle pushBundle, String id, String contextId) throws Exception {
        SwrveLogger.d("Found engaged event: %s, with contextId: %s", id, contextId);
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
        closeNotification(extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID)); // Button has been pressed, now close the notification
    }

    private void sendEngagedEvent(Bundle extras, Bundle pushBundle, String msgId) throws Exception {
        SwrveLogger.d("Found engaged event: %s", msgId);
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

    private void setNotificationSwrveCampaignIdFromPayload (Bundle pushBundle) {
        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        String swrvePushPayload = pushBundle.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY);
        if (SwrveHelper.isNotNullOrEmpty(swrvePushPayload)) {
            SwrveNotification swrveNotification = SwrveNotification.fromJson(swrvePushPayload);
            if (swrveNotification != null) {
                if (swrveNotification.getCampaign() != null) {
                    String swrveCampaignId =  swrveNotification.getCampaign().getId();
                    swrveCommon.setNotificationSwrveCampaignId(swrveCampaignId);
                }
            }
        }
    }

    private void setNotificationSwrveCampaignIdFromButtonAction (String campaignId) {
        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        if (SwrveHelper.isNotNullOrEmpty(campaignId)) {
            swrveCommon.setNotificationSwrveCampaignId(campaignId);
        }
    }

    private void executeCustomNotificationListener(Bundle msg) {
        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        SwrvePushNotificationListener listener = swrveCommon.getNotificationListener();
        if (listener != null) {
            JSONObject payload = convertPushPayloadToJSONObject(msg);
            listener.onPushNotification(payload);
        }
    }

    private JSONObject convertPushPayloadToJSONObject(Bundle bundle) {
        // Convert Bundle root keys to JSONObject and add the _s.JsonPayload ones
        JSONObject payload = new JSONObject();
        if (bundle.containsKey(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY)) {
            String pushPayload = bundle.getString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY);
            try {
                payload = new JSONObject(pushPayload);

                for (String key : bundle.keySet()) {
                    if (!key.equals(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY)) {
                        payload.put(key, bundle.get(key));
                    }
                }
            } catch (Exception ex) {
                SwrveLogger.e("SwrveNotificationEngageReceiver. Could not convert SwrveNotification To JSONObject: %s", ex, pushPayload);
            }
        }
        return payload;
    }

    protected void closeNotification(int notificationID) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notificationID);
    }

    private void openDeeplink(Bundle msg, String uri) {
        SwrveLogger.d("Found push deeplink. Will attempt to open: %s", uri);
        Bundle msgBundleCopy = new Bundle(msg); // make copy of extras and remove any that have been handled
        msgBundleCopy.remove(SwrveNotificationConstants.SWRVE_TRACKING_KEY);
        msgBundleCopy.remove(SwrveNotificationConstants.DEEPLINK_KEY);
        SwrveIntentHelper.openDeepLink(context, uri, msgBundleCopy);
        closeNotificationBar();
    }

    private void openActivity(Bundle msg) throws PendingIntent.CanceledException {
        Intent intent = getActivityIntent(msg);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, generateTimestampId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
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

    private void closeNotificationBar() {
        // When performing a button action on notifications, you need to close the notification drawer explicitly
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);
    }

    private int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }

    private Class<?> getActivityClass() {
        Class<?> clazz = null;
        SwrveNotificationConfig notificationConfig = SwrveCommon.getInstance().getNotificationConfig();
        if (notificationConfig != null && notificationConfig.getActivityClass() != null) {
            clazz = notificationConfig.getActivityClass();
        } else {
            try {
                String activity = getActivityClassDeprecated();
                if (SwrveHelper.isNullOrEmpty(activity)) {
                    PackageManager packageManager = context.getPackageManager();
                    ResolveInfo resolveInfo = packageManager.resolveActivity(packageManager.getLaunchIntentForPackage(context.getPackageName()), PackageManager.MATCH_DEFAULT_ONLY);
                    if (resolveInfo != null) {
                        activity = resolveInfo.activityInfo.name;
                        if (activity.startsWith(".")) {
                            activity = context.getPackageName() + activity; // Append application package as it starts with .
                        }
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

    @Deprecated
    private String getActivityClassDeprecated() throws Exception {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo app = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        Bundle metaData = app.metaData;
        String activity = metaData.getString(SwrveNotificationConstants.SWRVE_PUSH_ACTIVITY_METADATA);
        return activity;
    }
}
