package com.swrve.sdk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;

class SwrvePushManagerImp extends SwrvePushManagerImpBase implements SwrvePushManager {

    private SwrveNotificationBuilder notificationBuilder;

    SwrvePushManagerImp(Context context) {
        super(context);
    }

    @Override
    public void processMessage(final Bundle msg) {
        process(msg);
    }

    @Override
    public void processSilent(final Bundle msg, final String silentId) {
        SwrveSilentPushListener silentPushListener = swrveCommon.getSilentPushListener();
        if (silentPushListener != null) {
            JSONObject silentPayload = getSilentPayload(msg);
            silentPushListener.onSilentPush(context, silentPayload);
        } else {
            SwrveLogger.i("Swrve silent push received but there was no listener assigned or wasn't currently authenticated user");
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

    @Override
    public void processNotification(final Bundle msg, String pushId) {

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
                saveCampaignInfluence(msg, pushId);
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(notificationId, notification);
                SwrveLogger.d("SwrvePushManager: displayed notificationId: %s", notificationId);

                // Save notification id so existing authenticated notifications can be dismissed later if different user identifies
                if (isAuthenticatedNotification(msg)) {
                    // Notification ids are persisted to db because NotificationManager.getActiveNotifications is only api 23 and current minVersion is below that
                    swrveCommon.saveNotificationAuthenticated(notificationId);
                }
            }

        } catch (Exception ex) {
            SwrveLogger.e("Error processing push.", ex);
        }
    }

    @Override
    public SwrveNotificationFilter getNotificationFilter() {
        SwrveNotificationFilter filter = null;
        SwrveNotificationConfig notificationConfig = swrveCommon.getNotificationConfig();
        if (notificationConfig != null && notificationConfig.getNotificationFilter() != null) {
            SwrveLogger.d("SwrveNotificationFilter configured. Passing builder to custom filter.");
            filter = notificationConfig.getNotificationFilter();
        } else {
            SwrveLogger.d("SwrveNotificationFilter not configured.");
        }
        return filter;
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
