package com.swrve.sdk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import org.json.JSONObject;

import java.util.Date;
import java.util.Set;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static com.swrve.sdk.SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY;
import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.PUSH_INTERNAL_KEYS;

class SwrvePushServiceManager {

    private final Context context;
    private SwrveNotificationBuilder notificationBuilder;
    private String authenticatedUserId;

    SwrvePushServiceManager(Context context) {
        this.context = context;
    }

    void processMessage(final Bundle msg) {
        String silentId = SwrveHelper.getSilentPushId(msg);
        if (!SwrveHelper.isNullOrEmpty(silentId)) {
            processSilent(msg,silentId);
        } else {
            String pushId = SwrveHelper.getRemotePushId(msg);
            processNotification(msg, pushId);
        }
    }

    private JSONObject getSilentPayload(Bundle msg) {
        JSONObject silentPayload = new JSONObject();
        try {
            // Obtain the _s.SilentPayload key and decode it to pass it to customers
            String silentPayloadJson = msg.getString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY);
            if (silentPayloadJson != null) {
                silentPayload = new JSONObject(silentPayloadJson);
            }
        } catch (Exception exp) {
            SwrveLogger.e("Swrve silent push listener launched an exception: ", exp);
        }

        return silentPayload;
    }

    private boolean isTargetUser(Bundle msg) {
        boolean isTargetUser = true;
        authenticatedUserId = msg.getString(SwrveNotificationConstants.SWRVE_AUTH_USER_KEY);
        if (authenticatedUserId != null) {
            ISwrveCommon swrveCommon = SwrveCommon.getInstance();
            if (swrveCommon != null && !swrveCommon.getUserId().equals(authenticatedUserId)) {
                isTargetUser = false;
            }
        }
        return isTargetUser;
    }

    private void saveInfluencedCampaign(final Bundle msg, String trackingId) {
        SwrveCampaignInfluence campaignInfluence = new SwrveCampaignInfluence();
        campaignInfluence.saveInfluencedCampaign(context, trackingId, msg, getNow());
    }

    private void processSilent(final Bundle msg, final  String silentId) {
        saveInfluencedCampaign(msg, silentId);

        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        SwrveSilentPushListener silentPushListener = swrveCommon.getSilentPushListener();

        if (silentPushListener != null) {
            JSONObject silentPayload = getSilentPayload(msg);
            silentPushListener.onSilentPush(context, silentPayload);
        } else {
            SwrveLogger.i("Swrve silent push received but there was no listener assigned or wasn't currently authenticated user");
        }
    }

    private void processNotification(final Bundle msg, String pushId) {

        if (!isTargetUser(msg)) {
            SwrveLogger.w("Swrve cannot process push because its intended for different user.");
            return;
        }

        try {
            String msgText = msg.getString(SwrveNotificationConstants.TEXT_KEY);
            SwrveNotificationBuilder swrveNotificationBuilder = getSwrveNotificationBuilder();
            NotificationCompat.Builder notificationCompatBuilder = swrveNotificationBuilder.build(msgText, msg, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null);

            PendingIntent contentIntent = swrveNotificationBuilder.createPendingIntent(msg, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null);
            notificationCompatBuilder.setContentIntent(contentIntent);

            int notificationId = swrveNotificationBuilder.getNotificationId();
            Notification notification = applyCustomFilter(notificationCompatBuilder, notificationId, msg, swrveNotificationBuilder.getNotificationDetails());

            if (notification == null) {
                SwrveLogger.d("SwrvePushServiceManager: notification suppressed via custom filter. notificationId: %s", notificationId);
            } else {
                saveInfluencedCampaign(msg, pushId);
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(notificationId, notification);
                SwrveLogger.d("SwrvePushServiceManager: displayed notificationId: %s", notificationId);

                // Save notification id so existing authenticated notifications can be dismissed later if different user identifies
                if (authenticatedUserId != null) {
                    // Notification ids are persisted to db because NotificationManager.getActiveNotifications is only api 23 and current minVersion is below that
                    ISwrveCommon swrveCommon = SwrveCommon.getInstance();
                    swrveCommon.saveNotificationAuthenticated(notificationId);
                }
            }

        } catch (Exception ex) {
            SwrveLogger.e("Error processing push.", ex);
        }

        QaUser.pushNotification(pushId, msg);
    }

    private Notification applyCustomFilter(NotificationCompat.Builder builder, int notificationId, final Bundle msg, SwrveNotificationDetails notificationDetails) {
        Notification notification;
        SwrveNotificationConfig notificationConfig = SwrveCommon.getInstance().getNotificationConfig();
        if (notificationConfig == null ||
                (notificationConfig.getNotificationCustomFilter() == null && notificationConfig.getNotificationFilter() == null)) {
            SwrveLogger.d("SwrveNotificationFilter not configured.");
            notification = builder.build();
        } else {
            SwrveLogger.d("SwrveNotificationFilter configured. Passing builder to custom filter.");
            try {
                String payload = getPayload(msg);
                if (notificationConfig.getNotificationFilter() != null) {
                    SwrveNotificationFilter filter = notificationConfig.getNotificationFilter();
                    notification = filter.filterNotification(builder, notificationId, notificationDetails, payload);
                } else if (notificationConfig.getNotificationCustomFilter() != null) {
                    SwrveNotificationCustomFilter customFilter = notificationConfig.getNotificationCustomFilter();
                    notification = customFilter.filterNotification(builder, notificationId, payload);
                } else {
                    notification = builder.build();
                }
            } catch (Exception ex) {
                SwrveLogger.e("Error calling the custom notification filter.", ex);
                notification = builder.build();
            }
        }
        return notification;
    }

    protected String getPayload(final Bundle msg) {
        String payload = "";
        String jsonPayload = msg.getString(SWRVE_NESTED_JSON_PAYLOAD_KEY);
        // Try and clean the bundle keys so that only the custom properties (and no internal ones) are added to the custom filter json param
        try {
            JSONObject jsonObject = (jsonPayload != null) ? new JSONObject(jsonPayload) : new JSONObject();
            Set<String> msgKeySet = msg.keySet();
            msgKeySet.removeAll(PUSH_INTERNAL_KEYS);

            for (String key : msgKeySet) {
                // Do not overwrite key if already present
                if (!jsonObject.has(key)) {
                    jsonObject.put(key, msg.get(key));
                }
            }
            payload = jsonObject.toString();
        } catch (Exception ex) {
            SwrveLogger.e("Error getting json payload.", ex);
        }
        return payload;
    }

    protected Date getNow() {
        return new Date();
    }

    protected SwrveNotificationBuilder getSwrveNotificationBuilder() {
        if (notificationBuilder == null) {
            ISwrveCommon swrveCommon = SwrveCommon.getInstance();
            notificationBuilder = new SwrveNotificationBuilder(context, swrveCommon.getNotificationConfig());
        }
        return notificationBuilder;
    }
}
