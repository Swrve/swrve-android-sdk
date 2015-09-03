package com.swrve.sdk.locationcampaigns;

import android.Manifest;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import com.plotprojects.retail.android.FilterableNotification;
import com.plotprojects.retail.android.NotificationFilter;
import com.plotprojects.retail.android.NotificationFilterUtil;
import com.swrve.sdk.SwrveSDKBase;
import com.swrve.sdk.locationcampaigns.model.LocationCampaignPayload;
import com.swrve.sdk.services.SwrveBroadcastReceiver;
import com.swrve.sdk.services.SwrveIntentService;

import java.util.ArrayList;
import java.util.List;

public class LocationCampaignFilterService extends IntentService implements NotificationFilter {

    private static final String LOG_TAG = "SwrveLocation";

    public LocationCampaignFilterService() {
        super("LocationCampaignsFilterService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            NotificationFilterUtil.Batch batch = NotificationFilterUtil.getBatch(intent, this);
            List<FilterableNotification> filterableNotifications = batch.getNotifications();
            if (filterableNotifications != null && filterableNotifications.size() > 0) {
                filterLocationCampaigns(filterableNotifications, intent);
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Error in LocationCampaignsFilterService", ex);
        }
    }

    // todo dom: what if no internet connection?
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    protected void filterLocationCampaigns(List<FilterableNotification> filterableNotifications, Intent intent) {

        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "filter_location_campaigns");
        try {
            wakeLock.acquire();

            boolean hasValidLocationCampaigns = false;
            for (FilterableNotification filterableNotification : filterableNotifications) {
                try {
                    LocationCampaignPayload locationCampaignPayload = LocationCampaignPayload.fromJSON(filterableNotification.getData()); // todo add check for null or empty or bad data
                    SwrveSDKBase.getInstance().onGeofenceCrossed(locationCampaignPayload.getCampaignId(), locationCampaignPayload.getGeofenceId(), filterableNotification.getTrigger());
                    hasValidLocationCampaigns = true;
                } catch (Exception ex) {
                    Log.e(LOG_TAG, "Error in LocationCampaignsFilterService", ex);
                }
            }

            if (hasValidLocationCampaigns) {
                SwrveSDKBase.sendQueuedEvents(); // todo note, this is done asynchronously, so we may need semaphore/latch or something to block until its finished.
                scheduleAlarm(this, intent);
            }
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private void scheduleAlarm(Context context, Intent locationIntent) {
        Intent intentToSchedule = new Intent(context, SwrveBroadcastReceiver.class);
        intentToSchedule.putExtra(SwrveIntentService.EXTRA_LOCATION, locationIntent); //
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intentToSchedule, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 1000, pendingIntent); // todo dom: make time period configurable and pass in as param
    }

    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public static void targetLocationCampaigns(Context context, Intent intent) {

        if(intent == null || !intent.hasExtra(SwrveIntentService.EXTRA_LOCATION)) {
            Log.e(LOG_TAG, "LocationCampaignsFilterService.targetLocationCampaigns plot projects intent is null.");
            return;
        }

        Intent ppIntent = (Intent) intent.getExtras().get(SwrveIntentService.EXTRA_LOCATION);
        NotificationFilterUtil.Batch batch = NotificationFilterUtil.getBatch(ppIntent, context);
        if (batch == null || batch.getNotifications().size() == 0) {
            Log.e(LOG_TAG, "LocationCampaignsFilterService.targetLocationCampaigns: Problem getting batch notifications.");
            return;
        }

        List<FilterableNotification> notificationsToSend = new ArrayList<>();
        List<FilterableNotification> filterableNotifications = batch.getNotifications();
        if (filterableNotifications != null && filterableNotifications.size() > 0) {

            SwrveSDKBase.getInstance().refreshCampaignsAndResources(); // todo this needs to be done synchronously and wait until it finishes.

            for (FilterableNotification filterableNotification : filterableNotifications) {

                LocationCampaignPayload locationCampaignPayload = LocationCampaignPayload.fromJSON(filterableNotification.getData());
                // todo dom: get campaignId from locationCampaignPayload and check if it matches any location campaigns recently downloaded.
                boolean match = true;
                if (match) {
                    // todo: get location message variant from matched LocationCampaign:
                    // todo:    --> and update message text
                    // todo:    --> and add the locationMessageId into "data" to be used when notification is engaged.
                    String locationMessageId = "2468";
                    String locationMessageBody = filterableNotification.getMessage() + ". Content from LocationMessage.body should be swapped in here.";
                    filterableNotification.setData(locationMessageId);
                    filterableNotification.setMessage(locationMessageBody);
                    notificationsToSend.add(filterableNotification);
                }
            }
        }

        // TODO dom: if more than one notification to send, then reduce campaigns list to the most recently started one, and send impression for that one only.
        //String locationMessageId = filterableNotification.getData();
        String locationMessageId = "2468";
        SwrveSDKBase.event("Swrve.Location.Location-" + locationMessageId + ".impression");

        Log.d(LOG_TAG, "LocationCampaignsFilterService.targetLocationCampaigns. Sending " + notificationsToSend.size() + " notifications.");
        batch.sendNotifications(notificationsToSend); // always call sendNotifications even if List is empty
    }
}
