package com.swrve.sdk;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.swrve.sdk.model.PushPayloadButton;
import com.swrve.sdk.model.PushPayload;
import com.swrve.sdk.push.SwrvePushDeDuper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.content.ContentValues.TAG;

public class SwrvePushSDK {
    public static final String INFLUENCED_PREFS = "swrve.influenced_data";

    protected static SwrvePushSDK instance;
    private NotificationChannel defaultNotificationChannel;

    public static synchronized SwrvePushSDK createInstance(Context context) {
        if (instance == null) {
            instance = new SwrvePushSDK(context);
        }
        return instance;
    }

    private final Context context;
    private ISwrvePushNotificationListener pushNotificationListener;
    private SwrveSilentPushListener silentPushListener;
    private SwrvePushService service;
    private int notificationId;

    protected SwrvePushSDK(Context context) {
        this.context = context;
    }

    public static SwrvePushSDK getInstance() {
        return instance;
    }

    public ISwrvePushNotificationListener getPushNotificationListener() {
        return pushNotificationListener;
    }

    public void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener) {
        this.pushNotificationListener = pushNotificationListener;
    }

    public void setSilentPushListener(SwrveSilentPushListener silentPushListener) {
        this.silentPushListener = silentPushListener;
    }

    public void setService(SwrvePushService service) {
        this.service = service;
    }

    public void processRemoteNotification(Bundle msg, boolean checkDupes) {
        if (!isSwrveRemoteNotification(msg)) {
            SwrveLogger.i("Received Push: but not processing as it doesn't contain: %s or %s", SwrvePushConstants.SWRVE_TRACKING_KEY, SwrvePushConstants.SWRVE_SILENT_TRACKING_KEY);
            return;
        }

        if (!(checkDupes && new SwrvePushDeDuper(context).isDupe(msg))) {
            service.processNotification(msg);
        }
    }

    // Obtains the normal push or silent push tracking id
    public static String getPushId(final Bundle msg) {
        Object rawId = msg.get(SwrvePushConstants.SWRVE_TRACKING_KEY);
        String id = (rawId != null) ? rawId.toString() : null;
        if (id == null) {
            return getSilentPushId(msg);
        }
        return id;
    }

    // Obtains the silent push tracking id
    // Used by Unity
    public static String getSilentPushId(final Bundle msg) {
        Object rawId = msg.get(SwrvePushConstants.SWRVE_SILENT_TRACKING_KEY);
        return (rawId != null) ? rawId.toString() : null;
    }

    // Used by Unity
    public static boolean isSwrveRemoteNotification(final Bundle msg) {
        return !SwrveHelper.isNullOrEmpty(getPushId(msg));
    }

    public void processNotification(final Bundle msg) {
        // Attempt to save influence data for Push Notification
        if (msg.containsKey(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY)) {
            String influencePushID = getPushId(msg);
            if(SwrveHelper.isNotNullOrEmpty(influencePushID)) {
                // Save the date and push id for tracking influenced users
                saveInfluencedCampaign(context, influencePushID, msg.getString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY), getNow());
            }
        }
        // Process silent notifications
        String silentId = getSilentPushId(msg);
        if (!SwrveHelper.isNullOrEmpty(silentId)) {
            if (this.silentPushListener != null) {
                try {
                    // Obtain the _s.SilentPayload key and decode it to pass it to customers
                    String silentPayloadJson = msg.getString(SwrvePushConstants.SILENT_PAYLOAD_KEY);
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
            // Process visual notification
            if (!service.mustShowNotification()) {
                SwrveLogger.i("Not processing as mustShowNotification is false.");
                return;
            }

            try {
                // Put the message into a notification and post it.
                final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                // Set the NotificationId
                notificationId = createNotificationId(msg);

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

                // Time to show notification
                service.showNotification(mNotificationManager, notification);
            } catch (Exception ex) {
                SwrveLogger.e("Error processing push.", ex);
            }
        }
    }

    // Called by Unity
    public static List<InfluenceData> readSavedInfluencedData(SharedPreferences prefs) {
        Set<String> keys = prefs.getAll().keySet();
        ArrayList<InfluenceData> influencedData = new ArrayList<InfluenceData>();
        for (String trackingId : keys) {
            long maxInfluenceMillis = prefs.getLong(trackingId, 0);
            if (maxInfluenceMillis > 0) {
                influencedData.add(new InfluenceData(trackingId, maxInfluenceMillis));
            }
        }
        return influencedData;
    }

    // Used by Unity
    public static void saveInfluencedCampaign(Context context, String trackingId, String influencedWindowMinsStr, Date date) {
        int influencedWindowMins = Integer.parseInt(influencedWindowMinsStr);
        SharedPreferences sharedPreferences = context.getSharedPreferences(INFLUENCED_PREFS, Context.MODE_PRIVATE);

        // Calculate the max time when this push will be considered influenced
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MINUTE, influencedWindowMins);
        Date influencedDate = cal.getTime();

        InfluenceData newInfluenceData = new InfluenceData(trackingId, influencedDate.getTime());
        // Add the new push influenced data to the list
        List<InfluenceData> influencedData = readSavedInfluencedData(sharedPreferences);
        influencedData.add(newInfluenceData);

        // Save the list
        SharedPreferences.Editor edit = sharedPreferences.edit();
        for (InfluenceData influenceData : influencedData) {
            edit.putLong(influenceData.trackingId, influenceData.maxInfluencedMillis);
        }
        edit.commit();
    }

    public static void removeInfluenceCampaign(Context context, String trackingId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(INFLUENCED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.remove(trackingId).commit();
    }

    void processInfluenceData(ISwrveCommon sdk) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(INFLUENCED_PREFS, Context.MODE_PRIVATE);
        List<InfluenceData> influencedArray = readSavedInfluencedData(sharedPreferences);
        if (!influencedArray.isEmpty()) {
            ArrayList<String> influencedEvents = new ArrayList<String>();
            long nowMillis = getNow().getTime();
            for (InfluenceData influenceData : influencedArray) {
                try {
                    long deltaMillis = influenceData.maxInfluencedMillis - nowMillis;
                    if (deltaMillis >= 0 && influenceData.maxInfluencedMillis > 0) {
                        // We are still inside the influence window
                        Map<String, Object> parameters = new HashMap<String, Object>();
                        parameters.put("id", influenceData.getIntTrackingId());
                        parameters.put("campaignType", "push");
                        parameters.put("actionType", "influenced");
                        Map<String, String> payload = new HashMap<String, String>();
                        // Add delta time in minutes
                        payload.put("delta", String.valueOf(deltaMillis / (1000 * 60)));

                        String eventAsJSON = EventHelper.eventAsJSON("generic_campaign_event", parameters, payload, sdk.getNextSequenceNumber());
                        influencedEvents.add(eventAsJSON);
                    }
                } catch (JSONException e) {
                    SwrveLogger.e(TAG, "Could not obtain push influenced data:", e);
                }
            }
            if (!influencedEvents.isEmpty()) {
                sdk.sendEventsWakefully(context, influencedEvents);
            }

            // Remove the influence data
            sharedPreferences.edit().clear().commit();
        }
    }

    public int showNotification(NotificationManager notificationManager, Notification notification) {
        notificationManager.notify(notificationId, notification);
        return notificationId;
    }

    private int createNotificationId(Bundle msg) {
        // checks for the existence of an update id in the payload and sets it
        String swrvePayloadJSON = msg.getString(SwrvePushConstants.SWRVE_PAYLOAD_KEY);
        if(SwrveHelper.isNotNullOrEmpty(swrvePayloadJSON)){
            PushPayload pushPayload = PushPayload.fromJson(swrvePayloadJSON);
            if(pushPayload.getNotificationId() > 0){
                return pushPayload.getNotificationId();
            }
        }
        return generateTimestampId();
    }

    private int generateTimestampId() {
        return (int) (getNow().getTime() % Integer.MAX_VALUE);
    }

    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        String msgText = msg.getString("text");
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            NotificationCompat.Builder mBuilder = service.createNotificationBuilder(msgText, msg);
            mBuilder.setContentIntent(contentIntent);
            return mBuilder.build();
        }
        return null;
    }
    
    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        SwrvePushNotificationConfig notificationHelper = SwrvePushNotificationConfig.getInstance(context);
        return notificationHelper.createNotificationBuilder(context, msgText, msg, notificationId);
    }

    public PendingIntent createPendingIntent(Bundle msg) {
        Intent intent = service.createIntent(msg);
        if (intent != null) {
            return PendingIntent.getBroadcast(context, generateTimestampId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
        return null;
    }

    public Intent createIntent(Bundle msg) {
        Intent intent = new Intent(context, SwrvePushEngageReceiver.class);
        intent.putExtra(SwrvePushConstants.PUSH_BUNDLE, msg);
        intent.putExtra(SwrvePushConstants.PUSH_NOTIFICATION_ID, notificationId);
        return intent;
    }

    protected Date getNow() {
        return new Date();
    }

    public void setDefaultNotificationChannel(NotificationChannel defaultNotificationChannel) {
        this.defaultNotificationChannel = defaultNotificationChannel;
    }

    public NotificationChannel getDefaultNotificationChannel() {
        return defaultNotificationChannel;
    }

    public static class InfluenceData {
        String trackingId;
        long maxInfluencedMillis;

        public InfluenceData(String trackingId, long maxInfluenceMillis) {
            this.trackingId = trackingId;
            this.maxInfluencedMillis = maxInfluenceMillis;
        }

        public long getIntTrackingId() {
            return Long.parseLong(trackingId);
        }

        public JSONObject toJson() {
            try {
                JSONObject result = new JSONObject();
                result.put("trackingId", trackingId);
                result.put("maxInfluencedMillis", maxInfluencedMillis);
                return result;
            } catch (Exception e) {
                SwrveLogger.e("Could not serialize influence data:", e);
            }
            return null;
        }
    }

}
