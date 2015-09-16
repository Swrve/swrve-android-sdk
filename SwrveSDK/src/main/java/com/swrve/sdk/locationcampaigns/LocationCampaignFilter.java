package com.swrve.sdk.locationcampaigns;

import android.Manifest;
import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import com.plotprojects.retail.android.FilterableNotification;
import com.plotprojects.retail.android.NotificationFilterReceiver;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveSDKBase;
import com.swrve.sdk.locationcampaigns.model.LocationCampaign;
import com.swrve.sdk.locationcampaigns.model.LocationMessage;
import com.swrve.sdk.locationcampaigns.model.LocationPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

            // presumption here for FA that is that location campaigns have been refreshed

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

    // TODO dom: need to refactor init sdk method so certain variables, executors, etc  are initialised. Currently most of this is done in onCreate method.

    protected List<FilterableNotification> filterLocationCampaigns(List<FilterableNotification> filterableNotifications, long now) {

        List<CampaignMatch> campaignMatches = new ArrayList<>();
        Map<String, LocationCampaign> locationCampaigns = SwrveSDKBase.getInstance().getLocationCampaigns();
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
                campaignMatches.add(new CampaignMatch(filterableNotification, locationCampaign));
            } else {
                Log.i(LOG_TAG, "LocationCampaign is out of date:" + filterableNotification.getData());
            }
        }

        List<FilterableNotification> notificationsToSend = new ArrayList<>();
        if (campaignMatches.size() == 0) {
            Log.i(LOG_TAG, "No LocationCampaigns were matched ");
        } else {
            CampaignMatch match = getByMostRecentlyStarted(campaignMatches);
            FilterableNotification notificationToSend = match.filterableNotification;
            LocationMessage message = match.locationCampaign.getMessage();

            notificationToSend.setData(String.valueOf(message.getId())); // swap locationMessageId into "data" engagement event later
            notificationToSend.setMessage(message.getBody()); // update content of notification
            notificationsToSend.add(notificationToSend);

            sendLocationImpression(message.getId());
        }
        return notificationsToSend;
    }

    protected CampaignMatch getByMostRecentlyStarted(List<CampaignMatch> campaignMatches) {

        // only send the most recently started campaign, so sort matched campaigns by start date.
        Collections.sort(campaignMatches, new Comparator<CampaignMatch>() {
            public int compare(CampaignMatch o1, CampaignMatch o2) {
                long a = o1.locationCampaign.getStart();
                long b = o2.locationCampaign.getStart();
                if (a < b)
                    return 1;
                else if (a == b)
                    return 0;
                else
                    return -1;
            }
        });
        return campaignMatches.get(0);
    }

    protected void sendLocationImpression(int id) {
        SwrveSDKBase.event("Swrve.Location.Location-" + id + ".impression");
        SwrveSDKBase.sendQueuedEvents();
    }

    class CampaignMatch {
        FilterableNotification filterableNotification;
        LocationCampaign locationCampaign;

        CampaignMatch(FilterableNotification filterableNotification, LocationCampaign locationCampaign) {
            this.filterableNotification = filterableNotification;
            this.locationCampaign = locationCampaign;
        }
    }
}