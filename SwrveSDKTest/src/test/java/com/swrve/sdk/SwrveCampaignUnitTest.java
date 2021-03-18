package com.swrve.sdk;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

public class SwrveCampaignUnitTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();
        SwrveTestUtils.disableRestClientExecutor(swrveSpy);
        swrveSpy.init(mActivity);
    }

    @Test
    public void testLoadFromJSON() throws JSONException {
        String json = SwrveTestUtils.getAssetAsText(ApplicationProvider.getApplicationContext(), "single_campaign_json.json");
        JSONObject campaignData = new JSONObject(json);
        Set<SwrveAssetsQueueItem> assetsQueue = new HashSet<>();
        SwrveInAppCampaign campaign = new SwrveInAppCampaign(swrveSpy, new SwrveCampaignDisplayer(), campaignData, assetsQueue, null);

        assertNotNull(campaign);
        assertEquals(3, assetsQueue.size());
    }

    @Test
    public void testLoadFromJSONEmptyAssets() throws JSONException {
        String json = SwrveTestUtils.getAssetAsText(ApplicationProvider.getApplicationContext(), "single_campaign_json_empty_assets.json");
        JSONObject campaignData = new JSONObject(json);
        Set<SwrveAssetsQueueItem> assetsQueue = new HashSet<>();
        SwrveInAppCampaign campaign = new SwrveInAppCampaign(swrveSpy, new SwrveCampaignDisplayer(), campaignData, assetsQueue, null);

        assertNotNull(campaign);
        assertEquals(0, assetsQueue.size());
    }

    // Campaign throttle limit: delay before first message
    @Test
    public void testGetMessageForEventWaitFirstTimeCampaign() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_limits.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        // Do not return any until delay_first_message
        SwrveBaseMessage message = swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);

        // Even after corresponding app throttle limit has expired
        Date secondsLater31 = new Date(System.currentTimeMillis() + 31000l);
        doReturn(secondsLater31).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);

        // Return message after both limits expire
        Date secondsLater60 = new Date(System.currentTimeMillis() + 60000l);
        doReturn(secondsLater60).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
        assertTrue("message should be instanceof SwrveMessage", (message instanceof SwrveMessage));
        assertEquals(165, message.getId());
    }

    // Campaign throttle limit: minimum delay between messages
    @Test
    public void testGetMessageForEventCampaignWaitIfDisplayed() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_limits.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        Date secondsLater60 = new Date(System.currentTimeMillis() + 60000l);
        doReturn(secondsLater60).when(swrveSpy).getNow();
        SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
        assertEquals(165, message.getId());
        swrveSpy.messageWasShownToUser(message.getFormats().get(0));

        // Make sure no 2nd message is shown until after min_delay_between_messages limit defined in campaigns
        Date secondsLater31 = new Date(secondsLater60.getTime() + 31000l);
        doReturn(secondsLater31).when(swrveSpy).getNow();
        message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);

        // Ensure message is show after delay
        Date secondsLater30 = new Date(secondsLater31.getTime() + 30000l);
        doReturn(secondsLater30).when(swrveSpy).getNow();
        message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
    }
}
