package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.EVENT_PAYLOAD_DEEPLINK;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_BUTTON_TEXT;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_PLATFORM;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_TRACKING_DATA;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;

import com.swrve.sdk.notifications.model.SwrveNotificationButton;

import org.json.JSONObject;

import java.util.Date;
import java.util.Map;

class SwrveNotificationEngage {

    public static final String DO_NOT_OPEN_INTENT = "do_not_open_intent";
    private final Context context;
    private Intent intent;
    private Bundle extras; // the root bundle extras
    private Bundle pushBundle; // the push bundle
    private String pushId;
    private String campaignType;
    private Map<String, String> payload;

    SwrveNotificationEngage(Context context) {
        this.context = context;
    }

    void processIntent(Intent intent) {

        if (intent == null || intent.getExtras() == null || intent.getExtras().isEmpty()) {
            return;
        }

        try {
            this.intent = intent;
            extras = intent.getExtras();
            pushBundle = extras.getBundle(SwrveNotificationConstants.PUSH_BUNDLE);
            if (pushBundle == null) {
                return;
            }
            Object rawId = pushBundle.get(SwrveNotificationConstants.SWRVE_TRACKING_KEY);
            pushId = (rawId != null) ? rawId.toString() : null;
            if (SwrveHelper.isNullOrEmpty(pushId)) {
                return;
            }

            new SwrveCampaignInfluence().removeInfluenceCampaign(context, pushId); // Clear the influence data for this push

            campaignType = getCampaignType();
            payload = getBasicPayload();

            String contextId = extras.getString(SwrveNotificationConstants.CONTEXT_ID_KEY);
            if (SwrveHelper.isNotNullOrEmpty(contextId)) {
                handleButtonEngagement(contextId);
            } else {
                handleNotificationEngagement();
            }
        } catch (Exception e) {
            SwrveLogger.e("SwrveNotificationEngage.processIntent", e);
        }

        executeCustomNotificationListener(pushBundle);
    }

    private String getCampaignType() {
        return extras.getString(SwrveNotificationConstants.CAMPAIGN_TYPE);
    }

    // This is the basic payload. Deeplink might be added later
    private Map<String, String> getBasicPayload() {
        Bundle eventPayloadExtra = extras.getBundle(SwrveNotificationConstants.EVENT_PAYLOAD); // some flows, such as geo, pass in event payloads
        Map<String, String> basicPayload = SwrveHelper.getBundleAsMap(eventPayloadExtra);
        if (pushBundle.containsKey(SwrveNotificationConstants.TRACKING_DATA_KEY)) {
            basicPayload.put(GENERIC_EVENT_PAYLOAD_TRACKING_DATA, pushBundle.getString(SwrveNotificationConstants.TRACKING_DATA_KEY));
        }
        if (pushBundle.containsKey(SwrveNotificationConstants.PLATFORM_KEY)) {
            basicPayload.put(GENERIC_EVENT_PAYLOAD_PLATFORM, pushBundle.getString(SwrveNotificationConstants.PLATFORM_KEY));
        }
        return basicPayload;
    }

    private void handleButtonEngagement(String contextId) throws Exception {
        SwrveLogger.d("SwrveSDK: Handle button engagement pushId: %s, with contextId: %s", pushId, contextId);

        String url = extras.getString(SwrveNotificationConstants.PUSH_ACTION_URL_KEY);
        if (SwrveHelper.isNotNullOrEmpty(url)) {
            payload.put(EVENT_PAYLOAD_DEEPLINK, url);
        }
        EventHelper.sendEngagedEvent(context, campaignType, pushId, payload);

        getNotificationMediaManager(context).deleteGifUri(intent); // If notification had a gif, delete it now

        String buttonText = extras.getString(SwrveNotificationConstants.BUTTON_TEXT_KEY);
        payload.put(GENERIC_EVENT_PAYLOAD_BUTTON_TEXT, buttonText);
        EventHelper.sendButtonClickEvent(context, campaignType, pushId, contextId, payload);

        // Button has been pressed, now close the notification
        int notificationId = extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notificationId);

        SwrveNotificationButton.ActionType buttonActionType = (SwrveNotificationButton.ActionType) extras.get(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY);
        if (buttonActionType == SwrveNotificationButton.ActionType.OPEN_CAMPAIGN) {
            setNotificationSwrveCampaignId(url); // Open campaign functionality is not supported yet in the BE - this is future proofing
        }

        if (extras.containsKey(DO_NOT_OPEN_INTENT) && extras.getBoolean(DO_NOT_OPEN_INTENT)) {
            return; // if not using the engagement proxy, or intent is not an external deeplink, then the target intent/activity will already be opened
        }

        SwrveNotificationButton.ActionType type = (SwrveNotificationButton.ActionType) extras.get(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY);
        switch (type) {
            case OPEN_URL:
                openDeeplink(pushBundle, url);
                break;
            case OPEN_APP:
                openActivity(pushBundle);
                break;
            case OPEN_CAMPAIGN:
                openActivity(pushBundle);
                break;
            case DISMISS:
                break;
        }
    }

    private void handleNotificationEngagement() throws Exception {
        SwrveLogger.d("SwrveSDK: Handle notification engagement pushId: %s", pushId);

        String campaignId = extras.getString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY);
        setNotificationSwrveCampaignId(campaignId);

        String deepLink = pushBundle.getString(SwrveNotificationConstants.DEEPLINK_KEY);
        if (SwrveHelper.isNotNullOrEmpty(deepLink)) {
            payload.put(EVENT_PAYLOAD_DEEPLINK, deepLink);
        }
        EventHelper.sendEngagedEvent(context, campaignType, pushId, payload);

        getNotificationMediaManager(context).deleteGifUri(intent); // If notification had a gif, delete it now

        if (extras.containsKey(DO_NOT_OPEN_INTENT) && extras.getBoolean(DO_NOT_OPEN_INTENT)) {
            return; // if not using the engagement proxy, or intent is not an external deeplink, then the target intent/activity will already be opened
        }

        if (SwrveHelper.isNotNullOrEmpty(deepLink)) {
            openDeeplink(pushBundle, deepLink);
        } else {
            openActivity(pushBundle);
        }
    }

    private void setNotificationSwrveCampaignId(String campaignId) {
        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        if (SwrveHelper.isNotNullOrEmpty(campaignId)) {
            swrveCommon.setNotificationSwrveCampaignId(campaignId); // set the campaign to open from the notification
        }
    }

    private void executeCustomNotificationListener(Bundle msg) {
        try {
            ISwrveCommon swrveCommon = SwrveCommon.getInstance();
            SwrvePushNotificationListener listener = swrveCommon.getNotificationListener();
            if (listener != null) {
                JSONObject payload = SwrveHelper.convertPayloadToJSONObject(msg);
                listener.onPushNotification(payload);
            }
        } catch (Exception e) {
            SwrveLogger.e("SwrveNotificationEngage.executeCustomNotificationListener Error executing CustomNotificationListener", e);
        }
    }

    protected void openDeeplink(Bundle msg, String deeplink) {
        Intent intent = getDeeplinkIntent(msg, deeplink);
        if (SwrveCommon.getInstance() != null && SwrveCommon.getInstance().getSwrveDeeplinkListener() != null) {
            SwrveLogger.d("SwrveSDK: Passing to SwrveDeeplinkListener to open deeplink: %s", deeplink);
            SwrveCommon.getInstance().getSwrveDeeplinkListener().handleDeeplink(context, deeplink, extras);
        } else {
            SwrveLogger.d("SwrveSDK: Opening deeplink: %s", deeplink);
            context.startActivity(intent);
        }
    }

    static Intent getDeeplinkIntent(Bundle msg, String deeplink) {
        Intent intent;
        SwrveNotificationConfig notificationConfig = SwrveCommon.getInstance().getNotificationConfig();
        if (notificationConfig != null && notificationConfig.getNotificationIntentListener() != null) {
            intent = notificationConfig.getNotificationIntentListener().onNotificationEngage(msg, deeplink);
        } else {
            intent = SwrveIntentHelper.getDeepLinkIntent(deeplink, msg);
        }
        return intent;
    }

    protected void openActivity(Bundle msg) throws PendingIntent.CanceledException {
        Intent intent = getActivityIntent(context, msg);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(context, generateTimestampId(), intent, flags);
        pendingIntent.send();
    }

    static Intent getActivityIntent(Context context, Bundle msg) {
        Intent intent = null;
        SwrveNotificationConfig notificationConfig = SwrveCommon.getInstance().getNotificationConfig();
        if (notificationConfig != null && notificationConfig.getNotificationIntentListener() != null) {
            intent = notificationConfig.getNotificationIntentListener().onNotificationEngage(msg, null);
        } else {
            Class<?> clazz = getActivityClass(context);
            if (clazz != null) {
                intent = new Intent(context, clazz);
                intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, msg);
                intent.setAction("openActivity");
                intent.addFlags(SwrveIntentHelper.getDefaultIntentFlags());
            }
        }
        return intent;
    }

    private int generateTimestampId() {
        return (int) (new Date().getTime() % Integer.MAX_VALUE);
    }

    static Class<?> getActivityClass(Context context) {
        Class<?> clazz = null;
        SwrveNotificationConfig notificationConfig = SwrveCommon.getInstance().getNotificationConfig();
        if (notificationConfig != null && notificationConfig.getActivityClass() != null) {
            clazz = notificationConfig.getActivityClass();
        } else {
            // If no class configured then use the default launcher activity
            clazz = getDefaultActivityClass(context);
        }
        return clazz;
    }

    static Class<?> getDefaultActivityClass(Context context) {
        Class<?> clazz = null;
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
        return clazz;
    }

    protected NotificationMediaManager getNotificationMediaManager(Context context) {
        return new NotificationMediaManager(context);
    }
}
