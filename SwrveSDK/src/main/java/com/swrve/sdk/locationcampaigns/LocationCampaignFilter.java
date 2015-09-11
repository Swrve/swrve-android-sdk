package com.swrve.sdk.locationcampaigns;

import android.Manifest;
import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import com.plotprojects.retail.android.FilterableNotification;
import com.plotprojects.retail.android.NotificationFilterReceiver;
import com.swrve.sdk.SwrveSDKBase;
import com.swrve.sdk.locationcampaigns.model.LocationPayload;

import java.util.ArrayList;
import java.util.List;

public class LocationCampaignFilter extends NotificationFilterReceiver {

    private static final String LOG_TAG = "SwrveLocation";

    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    @Override
    public List<FilterableNotification> filterNotifications(List<FilterableNotification> notifications) {

        Log.d(LOG_TAG, "LocationCampaignNotificationFilter. Got " + notifications.size() + " notifications from plot projects.");
        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "swrve_wakelock");
        try {
            wakeLock.acquire();

            // todo dom: presumption here that we don't need to refresh location campaigns
            return filterLocationCampaigns(notifications);
        } catch (Exception ex) {
            Log.e(LOG_TAG, "LocationCampaignNotificationFilter exception.", ex);
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
        return new ArrayList<>();
    }

    private List<FilterableNotification> filterLocationCampaigns(List<FilterableNotification> filterableNotifications){

        List<FilterableNotification> notificationsToSend = new ArrayList<>();
        for (FilterableNotification filterableNotification : filterableNotifications) {

            LocationPayload locationPayload = LocationPayload.fromJSON(filterableNotification.getData());
            // todo dom: get campaignId from locationPayload and check if it matches any location campaigns recently downloaded.
            boolean match = true;
            if (match) {
                // todo: get location message variant from matched LocationCampaign:
                // todo:    --> and update message text
                // todo:    --> and add the locationMessageId into "data" to be used when notification is engaged.
                String locationMessageId = "2468";
                String locationMessageBody = filterableNotification.getMessage() + ". Content from LocationMessage.body should be swapped in at filterLocationCampaigns.";
                filterableNotification.setData(locationMessageId);
                filterableNotification.setMessage(locationMessageBody);
                notificationsToSend.add(filterableNotification);
            }
        }

        // TODO dom: if more than one notification to send, then reduce campaigns list to the most recently started one, and send impression for that one only.
        //String locationMessageId = filterableNotification.getData();
        String locationMessageId = "2468";
        SwrveSDKBase.event("Swrve.Location.Location-" + locationMessageId + ".impression");
        SwrveSDKBase.sendQueuedEvents();

        Log.d(LOG_TAG, "LocationCampaignsFilterService.targetLocationCampaigns. Sending " + notificationsToSend.size() + " notifications.");
        return notificationsToSend;
    }
}