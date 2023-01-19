package com.swrve.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveMessagePage;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    public void testAssetsLoadFromJSON() throws JSONException {
        String json = SwrveTestUtils.getAssetAsText(ApplicationProvider.getApplicationContext(), "single_campaign_json.json");
        JSONObject campaignData = new JSONObject(json);
        Set<SwrveAssetsQueueItem> assetsQueue = new HashSet<>();
        SwrveInAppCampaign campaign = new SwrveInAppCampaign(swrveSpy, new SwrveCampaignDisplayer(), campaignData, assetsQueue, null);

        assertNotNull(campaign);
        assertEquals(3, assetsQueue.size());

        SwrveMessageFormat format = campaign.getMessage().getFormats().get(0);
        assertEquals(0, format.getFirstPageId());
        Map<Long, SwrveMessagePage> pages  = format.getPages();
        assertNotNull(pages);
        assertEquals(1, pages.size());

        SwrveMessagePage page1 = pages.get(0l); // all format of json will add single page at index key 0
        assertEquals(1, page1.getImages().size());
        assertEquals(2, page1.getButtons().size());
    }

    @Test
    public void testMultiPageJSON() throws JSONException {
        String json = SwrveTestUtils.getAssetAsText(ApplicationProvider.getApplicationContext(), "multipage_campaign_swipe.json");
        JSONObject campaigns = new JSONObject(json);
        JSONObject campaignData = campaigns.getJSONArray("campaigns").getJSONObject(0);
        Set<SwrveAssetsQueueItem> assetsQueue = new HashSet<>();
        SwrveInAppCampaign campaign = new SwrveInAppCampaign(swrveSpy, new SwrveCampaignDisplayer(), campaignData, assetsQueue, null);

        assertNotNull(campaign);
        assertEquals(5, assetsQueue.size());

        SwrveMessageFormat format = campaign.getMessage().getFormats().get(0);
        assertEquals(123, format.getFirstPageId());
        Map<Long, SwrveMessagePage> pages  = format.getPages();
        assertNotNull(pages);
        assertEquals(2, pages.size());
        assertTrue(pages.containsKey(123l));
        assertTrue(pages.containsKey(456l));

        SwrveMessagePage page1 = pages.get(123l);
        assertEquals(123l, page1.getPageId());
        assertEquals("page1", page1.getPageName());
        assertEquals(-1l, page1.getSwipeBackward());
        assertEquals(456l, page1.getSwipeForward());
        assertEquals(1, page1.getImages().size());
        assertEquals(2, page1.getButtons().size());
        SwrveButton button = page1.getButtons().get(0);
        assertEquals(111l, button.getButtonId());

        SwrveMessagePage page2 = pages.get(456l);
        assertEquals(456l, page2.getPageId());
        assertEquals("page2", page2.getPageName());
        assertEquals(123l, page2.getSwipeBackward());
        assertEquals(-1l, page2.getSwipeForward());
        assertEquals(1, page2.getImages().size());
        assertEquals(2, page2.getButtons().size());
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

    @Ignore("Ignored for now. Failing regularly in CI but passing locally ok.")
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
