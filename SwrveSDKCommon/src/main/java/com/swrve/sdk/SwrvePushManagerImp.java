package com.swrve.sdk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;

class SwrvePushManagerImp implements SwrvePushManager {

    private ISwrveCommon swrveCommon = SwrveCommon.getInstance();
    private final Context context;
    private SwrveNotificationBuilder notificationBuilder;
    private String authenticatedUserId;

    SwrvePushManagerImp(Context context) {
        this.context = context;
    }

    @Override
    public void processMessage(final Bundle msg) {

        if (swrveCommon == null) {
            SwrveLogger.e("SwrveSDK cannot process push because SwrveCommon is null. Please check integration.");
            return;
        }

        String silentId = SwrveHelper.getSilentPushId(msg);

        if (!SwrveHelper.isNullOrEmpty(silentId)) {
            sendPushDeliveredEvent(msg, false, "");
            processSilent(msg, silentId);
        } else {

            boolean displayed = true;
            String reason = "";

            authenticatedUserId = msg.getString(SwrveNotificationConstants.SWRVE_AUTH_USER_KEY);
            boolean isAuthenticatedPush = isAuthenticatedPush(authenticatedUserId);
            if (isAuthenticatedPush && isDifferentUser(authenticatedUserId)) {
                displayed = false;
                reason = "different_user";
            } else if (isAuthenticatedPush && isTrackingStateStopped()) {
                displayed = false;
                reason = "stopped";
            }
            sendPushDeliveredEvent(msg, displayed, reason);

            if (displayed) {
                String pushId = SwrveHelper.getRemotePushId(msg);
                processNotification(msg, pushId);
            }
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

    private boolean isAuthenticatedPush(String authenticatedUserId) {
        return SwrveHelper.isNotNullOrEmpty(authenticatedUserId);
    }

    private boolean isDifferentUser(String authenticatedUserId) {
        boolean isDifferentUser = false;
        if (!swrveCommon.getUserId().equals(authenticatedUserId)) {
            SwrveLogger.w("Swrve cannot display push notification because its intended for different user.");
            isDifferentUser = true;
        }
        return isDifferentUser;
    }

    private boolean isTrackingStateStopped() {
        boolean isTrackingStateStopped = false;
        if (swrveCommon.isTrackingStateStopped()) {
            SwrveLogger.w("Swrve cannot display push notification because sdk is stopped.");
            isTrackingStateStopped = true;
        }
        return isTrackingStateStopped;
    }

    private void saveInfluencedCampaign(final Bundle msg, String trackingId) {
        SwrveCampaignInfluence campaignInfluence = new SwrveCampaignInfluence();
        campaignInfluence.saveInfluencedCampaign(context, trackingId, msg, getNow());
    }

    protected void sendPushDeliveredEvent(Bundle extras, boolean displayed, String reason) {
        try {
            ArrayList<String> eventsList = EventHelper.getPushDeliveredEvent(extras, getTime(), displayed, reason);
            if (eventsList != null && eventsList.size() > 0) {
                String eventBody = EventHelper.getPushDeliveredBatchEvent(eventsList);
                String endPoint = swrveCommon.getEventsServer() + "/1/batch";
                getCampaignDeliveryManager().sendCampaignDelivery(endPoint, eventBody);
            }
        } catch (Exception e) {
            SwrveLogger.e("Exception in sendPushDeliveredEvent.", e);
        }
    }

    private void processSilent(final Bundle msg, final  String silentId) {
        saveInfluencedCampaign(msg, silentId);

        SwrveSilentPushListener silentPushListener = swrveCommon.getSilentPushListener();

        if (silentPushListener != null) {
            JSONObject silentPayload = getSilentPayload(msg);
            silentPushListener.onSilentPush(context, silentPayload);
        } else {
            SwrveLogger.i("Swrve silent push received but there was no listener assigned or wasn't currently authenticated user");
        }
    }

    protected void processNotification(final Bundle msg, String pushId) {

        try {
            String msgText = msg.getString(SwrveNotificationConstants.TEXT_KEY);
            SwrveNotificationBuilder swrveNotificationBuilder = getSwrveNotificationBuilder();
            NotificationCompat.Builder notificationCompatBuilder = swrveNotificationBuilder.build(msgText, msg, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null);

            PendingIntent contentIntent = swrveNotificationBuilder.createPendingIntent(msg, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null);
            notificationCompatBuilder.setContentIntent(contentIntent);

            int notificationId = swrveNotificationBuilder.getNotificationId();
            Notification notification = applyCustomFilter(notificationCompatBuilder, notificationId, msg, swrveNotificationBuilder.getNotificationDetails());

            if (notification == null) {
                SwrveLogger.d("SwrvePushManager: notification suppressed via custom filter. notificationId: %s", notificationId);
            } else {
                saveInfluencedCampaign(msg, pushId);
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(notificationId, notification);
                SwrveLogger.d("SwrvePushManager: displayed notificationId: %s", notificationId);

                // Save notification id so existing authenticated notifications can be dismissed later if different user identifies
                if (authenticatedUserId != null) {
                    // Notification ids are persisted to db because NotificationManager.getActiveNotifications is only api 23 and current minVersion is below that
                    swrveCommon.saveNotificationAuthenticated(notificationId);
                }
            }

        } catch (Exception ex) {
            SwrveLogger.e("Error processing push.", ex);
        }
    }

    private Notification applyCustomFilter(NotificationCompat.Builder builder, int notificationId, final Bundle msg, SwrveNotificationDetails notificationDetails) {
        Notification notification;
        SwrveNotificationConfig notificationConfig = swrveCommon.getNotificationConfig();
        if (notificationConfig == null ||
                notificationConfig.getNotificationFilter() == null) {
            SwrveLogger.d("SwrveNotificationFilter not configured.");
            notification = builder.build();
        } else {
            SwrveLogger.d("SwrveNotificationFilter configured. Passing builder to custom filter.");
            try {
                String payload = SwrvePushManagerHelper.getPayload(msg);
                if (notificationConfig.getNotificationFilter() != null) {
                    SwrveNotificationFilter filter = notificationConfig.getNotificationFilter();
                    notification = filter.filterNotification(builder, notificationId, notificationDetails, payload);
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

    protected Date getNow() {
        return new Date();
    }

    protected long getTime() {
        return getNow().getTime();
    }

    protected SwrveNotificationBuilder getSwrveNotificationBuilder() {
        if (notificationBuilder == null) {
            notificationBuilder = new SwrveNotificationBuilder(context, swrveCommon.getNotificationConfig());
        }
        return notificationBuilder;
    }

    protected CampaignDeliveryManager getCampaignDeliveryManager() {
        return new CampaignDeliveryManager(context);
    }
}
