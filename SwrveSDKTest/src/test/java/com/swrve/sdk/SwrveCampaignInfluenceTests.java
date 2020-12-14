package com.swrve.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;
import java.util.List;

import static com.swrve.sdk.SwrveCampaignInfluence.INFLUENCED_PREFS;
import static org.mockito.Mockito.spy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwrveCampaignInfluenceTests extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = spy(swrveReal);
    }

    @Test
    public void testSaveInfluenceSilencePush() {

        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "2");

        Context context = swrveSpy.getContext();
        SwrveCampaignInfluence campaignInfluence = new SwrveCampaignInfluence();
        campaignInfluence.saveInfluencedCampaign(swrveSpy.getContext(), "1", bundle, new Date());

        SharedPreferences sharedPreferences = context.getSharedPreferences(INFLUENCED_PREFS, Context.MODE_PRIVATE);
        List<SwrveCampaignInfluence.InfluenceData> influencedData = campaignInfluence.getSavedInfluencedData(sharedPreferences);

        assertNotNull(influencedData);
        assertTrue(influencedData.get(0).silent);
        assertEquals(influencedData.get(0).trackingId, "1");
    }

    @Test
    public void testSaveInfluenceNormalPush() {

        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "2");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "2");

        Context context = swrveSpy.getContext();
        SwrveCampaignInfluence campaignInfluence = new SwrveCampaignInfluence();
        campaignInfluence.saveInfluencedCampaign(swrveSpy.getContext(), "2", bundle, new Date());

        SharedPreferences sharedPreferences = context.getSharedPreferences(INFLUENCED_PREFS, Context.MODE_PRIVATE);
        List<SwrveCampaignInfluence.InfluenceData> influencedData = campaignInfluence.getSavedInfluencedData(sharedPreferences);

        assertNotNull(influencedData);
        assertFalse(influencedData.get(0).silent);
        assertEquals(influencedData.get(0).trackingId, "2");
    }

    @Test
    public void testSaveAndDeleteMultipleInfluences() {

        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "21");

        Context context = swrveSpy.getContext();
        SwrveCampaignInfluence campaignInfluence = new SwrveCampaignInfluence();

        //Save first influence
        campaignInfluence.saveInfluencedCampaign(swrveSpy.getContext(), "1", bundle, new Date());

        //Update bundle and save second influence
        bundle.remove(SwrveNotificationConstants.SWRVE_TRACKING_KEY);
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "1234");
        campaignInfluence.saveInfluencedCampaign(swrveSpy.getContext(), "2", bundle, new Date());

        SharedPreferences sharedPreferences = context.getSharedPreferences(INFLUENCED_PREFS, Context.MODE_PRIVATE);
        List<SwrveCampaignInfluence.InfluenceData> influencedData = campaignInfluence.getSavedInfluencedData(sharedPreferences);

        assertNotNull(influencedData);
        assertFalse(influencedData.get(0).silent);
        assertEquals(influencedData.get(0).trackingId, "1");

        assertTrue(influencedData.get(1).silent);
        assertEquals(influencedData.get(1).trackingId, "2");
        assertEquals(influencedData.size(), 2);

        campaignInfluence.removeInfluenceCampaign(context, "1");

        influencedData = campaignInfluence.getSavedInfluencedData(sharedPreferences);
        assertEquals(influencedData.size(), 1);
        assertTrue(influencedData.get(0).silent);
        assertEquals(influencedData.get(0).trackingId, "2");

        campaignInfluence.removeInfluenceCampaign(context, "2");
        influencedData = campaignInfluence.getSavedInfluencedData(sharedPreferences);
        assertEquals(influencedData.size(), 0);
        assertNotNull(influencedData);
    }
}
