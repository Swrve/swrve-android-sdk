package com.swrve.sdk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.swrve.sdk.push.SwrvePushDeDuper;

import org.json.JSONObject;

import java.util.Date;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;

public class SwrvePushSDK {

    protected static SwrvePushSDK instance;

    public static synchronized SwrvePushSDK createInstance(Context context) {
        if (instance == null) {
            instance = new SwrvePushSDK(context);
        }
        return instance;
    }

    private final Context context;
    protected SwrveSilentPushListener silentPushListener;
    private SwrvePushService service;
    private SwrveNotificationBuilder notificationBuilder;
    private SwrveCampaignInfluence campaignInfluence = new SwrveCampaignInfluence();

    protected SwrvePushSDK(Context context) {
        this.context = context;
    }

    public static SwrvePushSDK getInstance() {
        return instance;
    }

    public void setSilentPushListener(SwrveSilentPushListener silentPushListener) {
        this.silentPushListener = silentPushListener;
    }

    public void setService(SwrvePushService service) {
        this.service = service;
    }

    public void processRemoteNotification(Bundle msg, boolean checkDupes) {
        if (!SwrveHelper.isSwrvePush(msg)) {
            SwrveLogger.i("Received Push: but not processing as it doesn't contain: %s or %s", SwrveNotificationConstants.SWRVE_TRACKING_KEY, SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY);
            return;
        }

        if (!(checkDupes && new SwrvePushDeDuper(context).isDupe(msg))) {
            service.processNotification(msg);
        }
    }

    public void processNotification(final Bundle msg) {

        // Process silent notifications
        String silentId = SwrveHelper.getSilentPushId(msg);
        if (!SwrveHelper.isNullOrEmpty(silentId)) {
            // Attempt to save influence data for push
            campaignInfluence.saveInfluencedCampaign(context, silentId, msg, getNow());

            if (this.silentPushListener != null) {
                try {
                    // Obtain the _s.SilentPayload key and decode it to pass it to customers
                    String silentPayloadJson = msg.getString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY);
                    JSONObject payload;
                    if (silentPayloadJson != null) {
                        payload = new JSONObject(silentPayloadJson);
                    } else {
                        payload = new JSONObject();
                    }
                    this.silentPushListener.onSilentPush(context, payload);
                } catch (Exception exp) {
                    SwrveLogger.e("Swrve silent push listener launched an exception: ", exp);
                }
            } else {
                SwrveLogger.i("Swrve silent push received but there was no listener assigned.");
            }
        } else {
            // Attempt to save influence data for push
            String pushId = SwrveHelper.getRemotePushId(msg);
            campaignInfluence.saveInfluencedCampaign(context, pushId, msg, getNow());

            // Process visual notification
            notificationBuilder = null; // reset the notificationBuilder

            if (!service.mustShowNotification()) {
                SwrveLogger.i("Not processing as mustShowNotification is false.");
                return;
            }

            try {
                // Put the message into a notification and post it.
                final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                final PendingIntent contentIntent = service.createPendingIntent(msg);
                if (contentIntent == null) {
                    SwrveLogger.e("Error processing push notification. Unable to create intent");
                    return;
                }

                final Notification notification = service.createNotification(msg, contentIntent);
                if (notification == null) {
                    SwrveLogger.e("Error processing push. Unable to create notification.");
                    return;
                }

                service.showNotification(mNotificationManager, notification);
            } catch (Exception ex) {
                SwrveLogger.e("Error processing push.", ex);
            }
        }
    }

    public int showNotification(NotificationManager notificationManager, Notification notification) {
        int notificationId = getSwrveNotificationBuilder().getNotificationId();
        notificationManager.notify(notificationId, notification);
        return notificationId;
    }

    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        String msgText = msg.getString(SwrveNotificationConstants.TEXT_KEY);
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            NotificationCompat.Builder mBuilder = service.createNotificationBuilder(msgText, msg);
            mBuilder.setContentIntent(contentIntent);
            return mBuilder.build();
        }
        return null;
    }

    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        SwrveNotificationBuilder builder = getSwrveNotificationBuilder();
        return builder.build(msgText, msg, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null);
    }

    public PendingIntent createPendingIntent(Bundle msg) {
        Intent intent = service.createIntent(msg);
        if (intent != null) {
            return getSwrveNotificationBuilder().createPendingIntent(msg, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null);
        }
        return null;
    }

    public Intent createIntent(Bundle msg) {
        return getSwrveNotificationBuilder().createIntent(msg, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null);
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
