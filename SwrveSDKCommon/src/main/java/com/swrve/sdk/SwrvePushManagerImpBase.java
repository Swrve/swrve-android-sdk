package com.swrve.sdk;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_CHANNEL_ID;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_CHANNEL_PARENT_ID;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_MSG_ID;
import static com.swrve.sdk.ISwrveCommon.SWRVE_NOTIFICATIONS_ENABLED;
import static com.swrve.sdk.ISwrveCommon.SWRVE_PERMISSION_NOTIFICATION;
import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.SWRVE_TRACKING_KEY;
import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
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

    protected String notificationChannelId;
    protected String channelGroupId;

    public SwrvePushManagerImpBase(Context context) {
        SwrveCommon.checkInstanceCreated();
        this.context = context;
        this.swrveCommon = SwrveCommon.getInstance();
    }

    public abstract void processSilent(final Bundle msg, final String silentId);

    public abstract void processNotification(final Bundle msg, String pushId);

    public abstract SwrveNotificationFilter getNotificationFilter();

    protected abstract SwrveNotificationBuilder getSwrveNotificationBuilder();

    protected void process(final Bundle msg) {

        String silentId = SwrveHelper.getSilentPushId(msg);
        if (!SwrveHelper.isNullOrEmpty(silentId)) {
            sendPushDeliveredEvent(msg, false, "");
            saveCampaignInfluence(msg, silentId);
            processSilent(msg, silentId);
        } else {
            boolean displayed = true;
            String reason = "";
            notificationChannelId = null;
            channelGroupId = null;
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
            } else if (isNotificationChannelDisabled(msg)) {
                displayed = false;
                reason = "channel_disabled";
                msg.putString(GENERIC_EVENT_PAYLOAD_CHANNEL_ID, notificationChannelId);
                msg.putString(GENERIC_EVENT_PAYLOAD_CHANNEL_PARENT_ID, channelGroupId);
            }
            sendPushDeliveredEvent(msg, displayed, reason);

            if (displayed) {
                String pushId = SwrveHelper.getRemotePushId(msg);
                processNotification(msg, pushId);
            }
        }
    }

    @TargetApi(value = 28)
    private boolean isNotificationChannelDisabled(final Bundle msg) {
        if (getOSBuildVersion() < Build.VERSION_CODES.O) {
            return false;
        }

        SwrveNotificationBuilder swrveNotificationBuilder = getSwrveNotificationBuilder();
        if (swrveNotificationBuilder != null) {
            swrveNotificationBuilder.setMessage(msg);
            notificationChannelId = swrveNotificationBuilder.getNotificationChannelId();
            try {
                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationChannelId != null) {
                    NotificationChannel channel = mNotificationManager.getNotificationChannel(notificationChannelId);
                    if (channel != null) {
                        boolean isMuted = channel.getImportance() == NotificationManager.IMPORTANCE_NONE;
                        if (!isMuted && getOSBuildVersion() >= Build.VERSION_CODES.P) {
                            channelGroupId = channel.getGroup();
                            if (channelGroupId != null) {
                                NotificationChannelGroup group = mNotificationManager.getNotificationChannelGroup(channel.getGroup());
                                isMuted = group.isBlocked();
                            }
                        }
                        return isMuted;
                    }
                }
            } catch (Exception e) {
                SwrveLogger.e("Exception in isNotificationDisabled.", e);
            }
        }

        return false;
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
                String uniqueWorkName = getUniqueWorkName(extras);
                String eventBody = EventHelper.getPushDeliveredBatchEvent(eventsList);
                String endPoint = swrveCommon.getEventsServer() + "/1/batch";
                getCampaignDeliveryManager().sendCampaignDelivery(uniqueWorkName, endPoint, eventBody);
            }
        } catch (Exception e) {
            SwrveLogger.e("Exception in sendPushDeliveredEvent.", e);
        }
    }

    private String getUniqueWorkName(Bundle extras) {
        String uniqueWorkName = extras.getString(SWRVE_UNIQUE_MESSAGE_ID_KEY);
        if (SwrveHelper.isNullOrEmpty(uniqueWorkName) && extras.containsKey(GENERIC_EVENT_PAYLOAD_MSG_ID)) {
            uniqueWorkName = extras.getString(GENERIC_EVENT_PAYLOAD_MSG_ID);
        }
        if (SwrveHelper.isNullOrEmpty(uniqueWorkName)) {
            uniqueWorkName = extras.getString(SWRVE_TRACKING_KEY);
        }
        uniqueWorkName = "CampaignDeliveryWork_" + uniqueWorkName; // add prefix
        return uniqueWorkName;
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
