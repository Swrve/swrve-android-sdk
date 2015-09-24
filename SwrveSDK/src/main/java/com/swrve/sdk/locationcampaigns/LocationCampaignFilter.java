package com.swrve.sdk.locationcampaigns;

import android.Manifest;
import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import com.plotprojects.retail.android.FilterableNotification;
import com.plotprojects.retail.android.NotificationFilterReceiver;
import com.swrve.sdk.BuildConfig;
import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveSDKBase;
import com.swrve.sdk.locationcampaigns.model.LocationCampaign;
import com.swrve.sdk.locationcampaigns.model.LocationMessage;
import com.swrve.sdk.locationcampaigns.model.LocationPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LocationCampaignFilter extends NotificationFilterReceiver {

    private static final String LOG_TAG = "SwrveLocation";

    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    @Override
    public List<FilterableNotification> filterNotifications(List<FilterableNotification> notifications) {

        Log.d(LOG_TAG, "LocationCampaignFilter. Received " + notifications.size() + " notifications to send.");
        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "swrve_wakelock");
        try {
            wakeLock.acquire();

            // TODO: presumption for FA is that location campaigns have been refreshed from ABTS by starting the app. Also that certain variables have been initialised. For FA hack this. For GA fix this.
            ((SwrveBase) (SwrveSDKBase.getInstance())).initSDKForLocationCampaigns(this);

            return filterLocationCampaigns(notifications, System.currentTimeMillis());
        } catch (Exception ex) {
            Log.e(LOG_TAG, "LocationCampaignFilter exception.", ex);
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
        return new ArrayList<>();
    }

    protected List<FilterableNotification> filterLocationCampaigns(List<FilterableNotification> filterableNotifications, long now) {

        Map<String, LocationCampaign> locationCampaigns = SwrveSDKBase.getInstance().getLocationCampaigns();
        Log.d(LOG_TAG, "LocationCampaigns: cache size of " + locationCampaigns.size());
        if (BuildConfig.DEBUG && locationCampaigns.size() > 0 && locationCampaigns.size() < 20) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, LocationCampaign> entry : locationCampaigns.entrySet()) {
                builder.append(entry.getValue().getId() + ",");
            }
            Log.d(LOG_TAG, "LocationCampaigns in cache:" + builder);
        }

        TreeMap<Long, FilterableNotification> locationCampaignsMatched =  new TreeMap<>();
        for (FilterableNotification filterableNotification : filterableNotifications) {

            LocationPayload locationPayload = LocationPayload.fromJSON(filterableNotification.getData());
            if (locationPayload == null || SwrveHelper.isNullOrEmpty(locationPayload.getCampaignId())) {
                Log.e(LOG_TAG, "LocationPayload is invalid. Payload:" + filterableNotification.getData());
                continue;
            }

            LocationCampaign locationCampaign = locationCampaigns.get(locationPayload.getCampaignId());
            if (locationCampaign == null || locationCampaign.getMessage() == null) {
                Log.i(LOG_TAG, "LocationCampaign not downloaded, or not targeted, or invalid. Payload:" + filterableNotification.getData());
                continue;
            }

            if (locationCampaign.getStart() <= now && (locationCampaign.getEnd() >= now || locationCampaign.getEnd() == 0)) {
                locationCampaignsMatched.put(locationCampaign.getStart(), filterableNotification); // store filterableNotification keyed on start time of campaign
            } else {
                Log.i(LOG_TAG, "LocationCampaign is out of date. \nnow:" + now + "\nlocationCampaign:" + locationCampaign);
            }
        }

        List<FilterableNotification> notificationsToSend = new ArrayList<>();
        if (locationCampaignsMatched.size() == 0) {
            Log.i(LOG_TAG, "No LocationCampaigns were matched ");
        } else {
            FilterableNotification notificationToSend = getByMostRecentlyStarted(locationCampaignsMatched);
            LocationPayload locationPayload = LocationPayload.fromJSON(notificationToSend.getData());
            LocationCampaign locationCampaign = locationCampaigns.get(locationPayload.getCampaignId());
            LocationMessage message = locationCampaign.getMessage();

            notificationToSend.setData(String.valueOf(message.getId())); // swap locationMessageId into "data" engagement event later
            notificationToSend.setMessage(message.getBody()); // update content of notification
            notificationsToSend.add(notificationToSend);

            sendLocationImpression(message.getId());
        }
        return notificationsToSend;
    }

    protected FilterableNotification getByMostRecentlyStarted(TreeMap<Long, FilterableNotification> locationCampaignsMatched) {
        // only send the most recently started campaign. The treemap is keyed on start date so get the last entry.
        FilterableNotification filterableNotification = locationCampaignsMatched.lastEntry().getValue();
        return filterableNotification;
    }

    protected void sendLocationImpression(int id) {
        SwrveSDKBase.event("Swrve.Location.Location-" + id + ".impression");
        SwrveSDKBase.sendQueuedEvents();
    }
}