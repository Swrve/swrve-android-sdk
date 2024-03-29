package com.swrve.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveButtonTheme;
import com.swrve.sdk.messaging.SwrveButtonThemeState;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveMessagePage;
import com.swrve.sdk.messaging.SwrveStorySettings;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SwrveCampaignUnitTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    private String [] BUTTON_THEME_ASSETS = new String[] {"background", "default_bg", "default_icon", "3417d97a686debe00ff011309ab48d018486.ttf", "pressed_bg", "pressed_icon", "focused_bg", "focused_icon"};

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
    public void testButtonThemeJSON() throws JSONException {
        String json = SwrveTestUtils.getAssetAsText(ApplicationProvider.getApplicationContext(), "campaign_native_button_everything.json");
        JSONObject campaigns = new JSONObject(json);
        JSONObject campaignData = campaigns.getJSONArray("campaigns").getJSONObject(0);
        Set<SwrveAssetsQueueItem> assetsQueue = new HashSet<>();
        SwrveInAppCampaign campaign = new SwrveInAppCampaign(swrveSpy, new SwrveCampaignDisplayer(), campaignData, assetsQueue, null);

        assertNotNull(campaign);

        SwrveMessageFormat format = campaign.getMessage().getFormats().get(0);
        SwrveButton button = format.getPages().get(456l).getButtons().get(0);
        assertNotNull(button);
        assertNotNull(button.getTheme());
        SwrveButtonTheme buttonTheme = button.getTheme();
        assertEquals(16, buttonTheme.getFontSize());
        assertEquals("18c23417d97a686debe00ff011309ab48d018486", buttonTheme.getFontDigest());
        assertEquals("", buttonTheme.getFontNativeStyle());
        assertEquals(1, buttonTheme.getTopPadding());
        assertEquals(2, buttonTheme.getRightPadding());
        assertEquals(3, buttonTheme.getBottomPadding());
        assertEquals(4, buttonTheme.getLeftPadding());
        assertEquals("default_bg", buttonTheme.getBgImage());
        assertEquals("#000000ff", buttonTheme.getFontColor());
        assertEquals("#FFFFFFFF", buttonTheme.getBgColor());
        assertEquals(5, buttonTheme.getBorderWidth());
        assertEquals("#0000001f", buttonTheme.getBorderColor());
        assertEquals(10, buttonTheme.getCornerRadius());
        assertTrue(buttonTheme.isTruncate());
        assertEquals("CENTER", buttonTheme.getHAlign());

        assertNotNull(buttonTheme.getPressedState());
        SwrveButtonThemeState pressedState = buttonTheme.getPressedState();
        assertEquals("#ffd0d0d0", pressedState.getFontColor());
        assertEquals("#7f7e7a7a", pressedState.getBgColor());
        assertEquals("#a7f3f700", pressedState.getBorderColor());
        assertEquals("pressed_bg", pressedState.getBgImage());

        assertNotNull(buttonTheme.getFocusedState());
        SwrveButtonThemeState focusState = buttonTheme.getFocusedState();
        assertEquals("#05000005", focusState.getFontColor());
        assertEquals("#05000006", focusState.getBgColor());
        assertEquals("#05000007", focusState.getBorderColor());
        assertEquals("focused_bg", focusState.getBgImage());
    }

    @Test
    public void testCampaignAssetsReadyForButtonTheme() throws Exception {
        // Verify happy case where everything is downloaded and campaign is returned
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_native_button_everything.json", BUTTON_THEME_ASSETS);
        assertNotNull(swrveSpy.getMessageCenterCampaign(102, null));
    }

    @Test
    public void testCampaignAssetsReadyForButtonTheme_missing_default_bg() throws Exception {
        assertCampaignAssetsReadyForButtonTheme("default_bg");
    }

    @Test
    public void testCampaignAssetsReadyForButtonTheme_missing_font() throws Exception {
        assertCampaignAssetsReadyForButtonTheme("3417d97a686debe00ff011309ab48d018486.ttf");
    }

    @Test
    public void testCampaignAssetsReadyForButtonTheme_missing_pressed_bg() throws Exception {
        assertCampaignAssetsReadyForButtonTheme("pressed_bg");
    }

    @Test
    public void testCampaignAssetsReadyForButtonTheme_missing_focused_bg() throws Exception {
        assertCampaignAssetsReadyForButtonTheme("focused_bg");
    }

    private void assertCampaignAssetsReadyForButtonTheme(String missingAsset) throws Exception {
        List<String> assets = new ArrayList<>(Arrays.asList(BUTTON_THEME_ASSETS));
        assertTrue(assets.remove(missingAsset));
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_native_button_everything.json", assets.toArray(new String[0]));
        assertNull(swrveSpy.getMessageCenterCampaign(102, null));
        assets.add(missingAsset);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_native_button_everything.json", assets.toArray(new String[0])); // load again with missing asset
        assertNotNull(swrveSpy.getMessageCenterCampaign(102, null));
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

    @Test
    public void testInAppStoryJSON() throws JSONException {
        String json = SwrveTestUtils.getAssetAsText(ApplicationProvider.getApplicationContext(), "campaign_in_app_story.json");
        JSONObject campaigns = new JSONObject(json);
        JSONObject campaignData = campaigns.getJSONArray("campaigns").getJSONObject(0);
        Set<SwrveAssetsQueueItem> assetsQueue = new HashSet<>();
        SwrveInAppCampaign campaign = new SwrveInAppCampaign(swrveSpy, new SwrveCampaignDisplayer(), campaignData, assetsQueue, null);

        SwrveMessageFormat format = campaign.getMessage().getFormats().get(0);
        assertEquals(1, format.getFirstPageId());
        Map<Long, SwrveMessagePage> pages  = format.getPages();
        assertNotNull(pages);

        SwrveStorySettings storySettings = format.getStorySettings();
        assertEquals(2500, storySettings.getPageDuration());
        assertEquals(12345, storySettings.getLastPageDismissId());
        assertEquals("Auto Dismiss?", storySettings.getLastPageDismissName());
        assertEquals(SwrveStorySettings.LastPageProgression.DISMISS, storySettings.getLastPageProgression());
        assertEquals(1, storySettings.getTopPadding());
        assertEquals(2, storySettings.getLeftPadding());
        assertEquals(3, storySettings.getBottomPadding());
        assertEquals(4, storySettings.getRightPadding());
        assertEquals("#fffffffa", storySettings.getBarColor());
        assertEquals("#fffffffb", storySettings.getBarBgColor());
        assertEquals(10, storySettings.getBarHeight());
        assertEquals(6, storySettings.getSegmentGap());
        assertNotNull(storySettings.getDismissButton());
        assertEquals(12345678, storySettings.getDismissButton().getButtonId());
        assertEquals("Dismiss?", storySettings.getDismissButton().getName());
        assertEquals("#fffffffc", storySettings.getDismissButton().getColor());
        assertEquals("#fffffffd", storySettings.getDismissButton().getPressedColor());
        assertEquals("#fffffffe", storySettings.getDismissButton().getFocusedColor());
        assertEquals(50, storySettings.getDismissButton().getSize());
        assertEquals(11, storySettings.getDismissButton().getMarginTop());
        assertTrue(storySettings.isGesturesEnabled());
        assertEquals("Dismiss", storySettings.getDismissButton().getAccessibilityText());
    }

    @Test
    public void testInAppStoryDifferentDataJSON() throws JSONException {
        // gestureEnabled is nullable so tests below cover all possible combinations
        SwrveStorySettings storySettings1 = getDummyStorySettings("loop", false, true, true);
        assertEquals(SwrveStorySettings.LastPageProgression.LOOP, storySettings1.getLastPageProgression());
        assertTrue(storySettings1.isGesturesEnabled());
        assertNotNull(storySettings1.getDismissButton());

        SwrveStorySettings storySettings2 = getDummyStorySettings("stop", true, false, false);
        assertEquals(SwrveStorySettings.LastPageProgression.STOP, storySettings2.getLastPageProgression());
        assertFalse(storySettings2.isGesturesEnabled());
        assertNull(storySettings2.getDismissButton());

        SwrveStorySettings storySettings3 = getDummyStorySettings("dismiss", true, true, false);
        assertEquals(SwrveStorySettings.LastPageProgression.DISMISS, storySettings3.getLastPageProgression());
        assertTrue(storySettings3.isGesturesEnabled());
        assertNull(storySettings3.getDismissButton());
    }

    private SwrveStorySettings getDummyStorySettings(String lastPageProgression, boolean hasGestures, boolean gestureEnabled,  boolean hasDismissButton) throws JSONException {
// @formatter:off
        String json =
                "{\n" +
                    "\"page_duration\": 7500,\n" +
                    "\"last_page_progression\": \"" + lastPageProgression + "\",\n";
        if (hasGestures) {
                    json +="\"gestures_enabled\": " + gestureEnabled + ",\n";
        }
        json +=
                    "\"padding\": {\n" +
                        "\"top\": 1,\n" +
                        "\"left\": 2,\n" +
                        "\"bottom\": 3,\n" +
                        "\"right\": 4\n" +
                    "},\n" +
                    "\"progress_bar\": {\n" +
                        "\"bar_color\": \"#ffffffff\",\n" +
                        "\"bg_color\": \"#ffffffff\",\n" +
                        "\"w\": -1,\n" +
                        "\"h\": 10,\n" +
                        "\"segment_gap\": 6\n" +
                    "}\n";

        if (hasDismissButton) {
            json +=
                    ",\n" +
                    "\"dismiss_button\": {\n" +
                        "\"id\": 12345,\n" +
                        "\"name\": \"Dismiss?\",\n" +
                        "\"color\": \"#ffffffff\",\n" +
                        "\"pressed_color\": \"#ffffffff\",\n" +
                        "\"focused_color\": \"#ffffffff\",\n" +
                        "\"size\": 7,\n" +
                        "\"margin_top\": 8,\n" +
                        "\"accessibility_text\": \"Dismiss\"\n" +
                    "}";
        }
        json += "}";
// @formatter:on
        JSONObject jsonObject = new JSONObject(json);
        return new SwrveStorySettings(jsonObject);
    }


}
