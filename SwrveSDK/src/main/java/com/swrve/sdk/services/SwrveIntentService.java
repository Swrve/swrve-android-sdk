package com.swrve.sdk.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.swrve.sdk.locationcampaigns.LocationCampaignFilterService;

public class SwrveIntentService extends IntentService {

    private static final String LOG_TAG = "SwrveSDK";

    public static final String EXTRA_LOCATION = "Location";

    public SwrveIntentService() {
        super("SwrveIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            Intent ppIntent = (Intent) intent.getExtras().get(EXTRA_LOCATION);
            if (ppIntent != null) {
                LocationCampaignFilterService.targetLocationCampaigns(this, intent);
            } else {
                Log.e(LOG_TAG, "Swrve: No recognised intent for SwrveIntentService");
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Swrve: Error in SwrveIntentService", ex);
        } finally {
            SwrveBroadcastReceiver.completeWakefulIntent(intent);
        }
    }
}
