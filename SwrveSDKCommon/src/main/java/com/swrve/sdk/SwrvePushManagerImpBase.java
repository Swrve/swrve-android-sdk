package com.swrve.sdk;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static com.swrve.sdk.ISwrveCommon.SWRVE_NOTIFICATIONS_ENABLED;
import static com.swrve.sdk.ISwrveCommon.SWRVE_PERMISSION_NOTIFICATION;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public abstract class SwrvePushManagerImpBase {

    protected final Context context;
    protected final ISwrveCommon swrveCommon;

    public SwrvePushManagerImpBase(Context context) {
        SwrveCommon.checkInstanceCreated();
        this.context = context;
        this.swrveCommon = SwrveCommon.getInstance();
    }

    public abstract void processSilent(final Bundle msg, final String silentId);

    public abstract void processNotification(final Bundle msg, String pushId);

    public abstract SwrveNotificationFilter getNotificationFilter();

    protected void process(final Bundle msg) {

        String silentId = SwrveHelper.getSilentPushId(msg);
        if (!SwrveHelper.isNullOrEmpty(silentId)) {
            sendPushDeliveredEvent(msg, false, "");
            saveCampaignInfluence(msg, silentId);
            processSilent(msg, silentId);
        } else {
            boolean displayed = true;
            String reason = "";
            boolean isAuthenticatedPush = isAuthenticatedNotification(msg);
            if (isAuthenticatedPush && isDifferentUserForAuthenticatedPush(msg)) {
                displayed = false;
                reason = "different_user";
            } else if (isAuthenticatedPush && isTrackingStateStopped()) {
                displayed = false;
                reason = "stopped";
            } else if (!hasNotificationPermission()) {
                displayed = false;
                reason = "permission_denied";
                sendDeviceUpdateWithDeniedNotificationPermission();
            }
            sendPushDeliveredEvent(msg, displayed, reason);

            if (displayed) {
                String pushId = SwrveHelper.getRemotePushId(msg);
                processNotification(msg, pushId);
            }
        }
    }

    protected boolean isAuthenticatedNotification(Bundle msg) {
        String targetUser = msg.getString(SwrveNotificationConstants.SWRVE_AUTH_USER_KEY);
        return SwrveHelper.isNotNullOrEmpty(targetUser);
    }

    private boolean isDifferentUserForAuthenticatedPush(Bundle msg) {
        String authenticatedUserId = msg.getString(SwrveNotificationConstants.SWRVE_AUTH_USER_KEY);
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

    protected boolean hasNotificationPermission() {
        boolean hasNotificationPermission;
        if (getOSBuildVersion() >= 33) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED;
        } else {
            hasNotificationPermission = NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
        return hasNotificationPermission;
    }

    // exposed for testing
    protected int getOSBuildVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }

    public void sendPushDeliveredEvent(Bundle extras, boolean displayed, String reason) {
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

    public void sendDeviceUpdateWithDeniedNotificationPermission() {
        try {
            JSONObject deviceUpdate = new JSONObject();
            if (getOSBuildVersion() >= 33) {
                deviceUpdate.put(SWRVE_PERMISSION_NOTIFICATION, SwrveHelper.getPermissionString(PERMISSION_DENIED));
            }
            deviceUpdate.put(SWRVE_NOTIFICATIONS_ENABLED, NotificationManagerCompat.from(context).areNotificationsEnabled());
            EventHelper.sendUninitiatedDeviceUpdateEvent(context, swrveCommon.getUserId(), deviceUpdate);
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK exception in sendDeviceUpdateWithDeniedNotificationPermission", e);
        }
    }

    protected Date getNow() {
        return new Date();
    }

    protected long getTime() {
        return getNow().getTime();
    }

    protected CampaignDeliveryManager getCampaignDeliveryManager() {
        return new CampaignDeliveryManager(context);
    }

    protected void saveCampaignInfluence(final Bundle msg, String trackingId) {
        SwrveCampaignInfluence campaignInfluence = new SwrveCampaignInfluence();
        campaignInfluence.saveInfluencedCampaign(context, trackingId, msg, getNow());
    }

    protected Notification applyCustomFilter(NotificationCompat.Builder builder, int notificationId, final Bundle msg, SwrveNotificationDetails notificationDetails) {
        Notification notification;
        try {
            String payload = SwrvePushManagerHelper.getPayload(msg);
            SwrveNotificationFilter filter = getNotificationFilter();
            if (filter != null) {
                notification = filter.filterNotification(builder, notificationId, notificationDetails, payload);
            } else {
                notification = builder.build();
            }
        } catch (Exception ex) {
            SwrveLogger.e("Error calling the custom notification filter.", ex);
            notification = builder.build();
        }
        return notification;
    }
}
