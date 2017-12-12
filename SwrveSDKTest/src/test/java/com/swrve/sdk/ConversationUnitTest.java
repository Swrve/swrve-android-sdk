package com.swrve.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.engine.model.ChoiceInputResponse;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.UserInputResult;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveConversationCampaign;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class ConversationUnitTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        swrveSpy.init(mActivity);
        Mockito.reset(swrveSpy);
    }

    @Test
    public void testCdnRootV3() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_v3.json");
        String cdnRoot = ((SwrveAssetsManagerImp)swrveSpy.swrveAssetsManager).cdnImages;
        assertThat("Version 3 might be already cached, so need to make sure it does not fail", cdnRoot, equalTo("http://fake_cdn_root.com/someurl/image/"));
    }

    @Test
    public void testCdnPaths() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json");
        String cdnImages = ((SwrveAssetsManagerImp) swrveSpy.swrveAssetsManager).cdnImages;
        assertThat("cdnImages is not being read correctly", cdnImages, equalTo("http://fake_cdn_root.com/someurl/image/"));
        String cdnFonts = ((SwrveAssetsManagerImp) swrveSpy.swrveAssetsManager).cdnFonts;
        assertThat("cdnFonts is not being read correctly", cdnFonts, equalTo("http://fake_cdn_root.com/someurl/font/"));
    }

    @Test
    public void testConversationLoads() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);
        assertThat(conversation.getPages().size(), equalTo(1));

        ConversationPage conversationPage = conversation.getFirstPage();
        assertNotNull(conversationPage);
        assertThat(conversationPage.getTitle(), equalTo("Page 1 of x"));
    }

    @Test
    public void testConversationMissingAssets() throws Exception {

        String fontMVITitle = "2617fb3c279e30dd7c180de8679a2e2d33cf3551";
        String fontMVIOption1 = "2617fb3c279e30dd7c180de8679a2e2d33cf3552";
        String fontMVIOption2 = "2617fb3c279e30dd7c180de8679a2e2d33cf3553";
        String fontHtmlFrag = "2617fb3c279e30dd7c180de8679a2e2d33cf3554";
        String fontStarRating = "2617fb3c279e30dd7c180de8679a2e2d33cf3555";
        String fontButton1 = "2617fb3c279e30dd7c180de8679a2e2d33cf3556";
        String fontButton2 = "2617fb3c279e30dd7c180de8679a2e2d33cf3557";

        // missing all assets
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_with_diff_fonts.json");
        assertNull(conversation);

        // contains all assets
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_with_diff_fonts.json", fontMVITitle, fontMVIOption1, fontMVIOption2, fontHtmlFrag, fontStarRating, fontButton1, fontButton2);
        conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>()); // contains 7 custom font assets and a system font asset
        assertNotNull(conversation);

        // missing fontButton2 asset
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_with_diff_fonts.json", fontMVITitle, fontMVIOption1, fontMVIOption2, fontHtmlFrag, fontStarRating, fontButton1);
        conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNull(conversation);

        // missing fontButton1 asset
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_with_diff_fonts.json", fontMVITitle, fontMVIOption1, fontMVIOption2, fontHtmlFrag, fontStarRating, fontButton2);
        conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNull(conversation);

        // missing fontStarRating asset
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_with_diff_fonts.json", fontMVITitle, fontMVIOption1, fontMVIOption2, fontHtmlFrag, fontButton1, fontButton2);
        conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNull(conversation);

        // missing fontHtmlFrag asset
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_with_diff_fonts.json", fontMVITitle, fontMVIOption1, fontMVIOption2, fontStarRating, fontButton1, fontButton2);
        conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNull(conversation);

        // missing fontMVIOption2 asset
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_with_diff_fonts.json", fontMVITitle, fontMVIOption1, fontHtmlFrag, fontStarRating, fontButton1, fontButton2);
        conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNull(conversation);

        // missing fontMVIOption1 asset
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_with_diff_fonts.json", fontMVITitle, fontMVIOption2, fontHtmlFrag, fontStarRating, fontButton1, fontButton2);
        conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNull(conversation);

        // missing fontMVITitle asset
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_with_diff_fonts.json", fontMVIOption1, fontMVIOption2, fontHtmlFrag, fontStarRating, fontButton1, fontButton2);
        conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNull(conversation);

        // contains all assets
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_with_diff_fonts.json", fontMVITitle, fontMVIOption1, fontMVIOption2, fontHtmlFrag, fontStarRating, fontButton1, fontButton2);
        conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);
    }

    @Test
    public void testSwrveConversationMissingAssets() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json"); // missing asset on purpose
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNull(conversation);
    }

    @Test
    public void testGetConversationForEvent() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");

        assertNull(swrveSpy.getConversationForEvent("some_trigger_that_doesn't_exist", new HashMap<String, String>()));

        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);
        assertThat(conversation.getPages().size(), equalTo(1));
    }

    @Test
    public void testConversationImageAssetsDownload() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_v3.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");

        SwrveBaseCampaign swrveCampaign = swrveSpy.campaigns.get(0);
        assertTrue(swrveCampaign instanceof SwrveConversationCampaign);
        SwrveConversation conversation = ((SwrveConversationCampaign) swrveCampaign).getConversation();
        assertEquals(conversation.getCampaign().getId(), swrveCampaign.getId());

        assertTrue(conversation.areAssetsReady(swrveSpy.getAssetsOnDisk()));

        boolean assetDownloaded = false;
        for (ConversationPage conversationPage : conversation.getPages()) {
            for (ConversationAtom conversationAtom : conversationPage.getContent()) {
                if (ConversationAtom.TYPE.CONTENT_IMAGE == conversationAtom.getType()) {
                    Content modelContent = (Content) conversationAtom;
                    String filePath = conversation.getCacheDir().getAbsolutePath() + "/" + modelContent.getValue();
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    assertNotNull(bitmap);
                    assetDownloaded = true;
                }
            }
        }
        assertTrue(assetDownloaded);

        swrveSpy.swrveAssetsManager.getAssetsOnDisk().clear();
        assertFalse(conversation.areAssetsReady(swrveSpy.getAssetsOnDisk()));
    }

    @Test
    public void testConversationCallActionCalledByUser() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        assertNotNull(swrveSpy.campaigns);
        assertThat(swrveSpy.campaigns.size(), equalTo(1));
        SwrveBaseCampaign swrveCampaign = swrveSpy.campaigns.get(0);
        assertTrue(swrveCampaign instanceof SwrveConversationCampaign);
        SwrveConversation conversation = ((SwrveConversationCampaign) swrveCampaign).getConversation();
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        ISwrveConversationSDK swrveConversationSDKSpy = Mockito.spy(conversationEventHelper.swrveConversationSDK);
        conversationEventHelper.swrveConversationSDK = swrveConversationSDKSpy;

        conversationEventHelper.conversationCallActionCalledByUser(conversation, "fromPageTag", "toActionTag");

        Map<String, String> map = new HashMap();
        map.put("control", "toActionTag");
        map.put("page", "fromPageTag");
        map.put("event", "call");
        map.put("conversation", "82");
        Mockito.verify(swrveConversationSDKSpy).queueConversationEvent("Swrve.Conversations.Conversation-82.call", "call", "fromPageTag", 82, map);
    }

    @Test
    public void testConversationEncounteredError() throws Exception {
        SwrveConversation mockConversation = getMockSwrveConversation(1);
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        ISwrveConversationSDK swrveConversationSDKSpy = Mockito.spy(conversationEventHelper.swrveConversationSDK);
        conversationEventHelper.swrveConversationSDK = swrveConversationSDKSpy;

        conversationEventHelper.conversationEncounteredError(mockConversation, "currentPageTag", new Exception("FAKE EXCEPTION IN TESTS - IGNORE "));

        Mockito.verify(swrveConversationSDKSpy).queueConversationEvent("Swrve.Conversations.Conversation-1.error", "error", "currentPageTag", 1, null);
    }

    @Test
    public void testConversationEventsCommitedByUser() throws Exception {
        ArrayList<UserInputResult> userInteractions = new ArrayList<UserInputResult>() {
            {
                add(createUserInputResult(UserInputResult.TYPE_SINGLE_CHOICE));
                add(createUserInputResult(UserInputResult.TYPE_SINGLE_CHOICE));
                add(createUserInputResult(UserInputResult.TYPE_VIDEO_PLAY));
            }
        };
        SwrveConversation mockConversation = getMockSwrveConversation(1);
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        ISwrveConversationSDK swrveConversationSDKSpy = Mockito.spy(conversationEventHelper.swrveConversationSDK);
        conversationEventHelper.swrveConversationSDK = swrveConversationSDKSpy;

        conversationEventHelper.conversationEventsCommitedByUser(mockConversation, userInteractions);

        Map<String, String> map = new HashMap();
        map.put("result", null);
        map.put("page", "tag");
        map.put("event", "choice");
        map.put("conversation", "123");
        map.put("fragment", "fragmentTag");
        Mockito.verify(swrveConversationSDKSpy, times(2)).queueConversationEvent("Swrve.Conversations.Conversation-1.choice", "choice", "tag", 123, map);
    }

    private UserInputResult createUserInputResult(String type) {
        UserInputResult userInputResult = new UserInputResult();
        userInputResult.type = type;
        userInputResult.conversationId = 123;
        userInputResult.fragmentTag = "fragmentTag";
        userInputResult.pageTag = "tag";
        userInputResult.result = new ChoiceInputResponse();
        return userInputResult;
    }

    @Test
    public void testConversationLinkActionCalledByUser() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        conversationEventHelper.swrveConversationSDK = swrveSpy;
        conversationEventHelper.conversationLinkVisitActionCalledByUser(getMockSwrveConversation(1), "fromPageTag", "toActionTag");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Conversations.Conversation-1.visit");
        Map<String, Object> payload = new HashMap<>();
        payload.put("control", "toActionTag");
        payload.put("page", "fromPageTag");
        payload.put("event", "visit");
        payload.put("conversation", "1");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    @Test
    public void testConversationDeepLinkActionCalledByUser() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        conversationEventHelper.swrveConversationSDK = swrveSpy;
        conversationEventHelper.conversationDeeplinkActionCalledByUser(getMockSwrveConversation(1), "fromPageTag", "toActionTag");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Conversations.Conversation-1.deeplink");
        Map<String, Object> payload = new HashMap<>();
        payload.put("control", "toActionTag");
        payload.put("page", "fromPageTag");
        payload.put("event", "deeplink");
        payload.put("conversation", "1");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    @Test
    public void testConversationPageWasViewedByUser() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        conversationEventHelper.swrveConversationSDK = swrveSpy;
        conversationEventHelper.conversationPageWasViewedByUser(getMockSwrveConversation(1), "pageTag");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Conversations.Conversation-1.impression");
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "impression");
        payload.put("page", "pageTag");
        payload.put("conversation", "1");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    @Test
    public void testConversationTransitionedToOtherPage() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        conversationEventHelper.swrveConversationSDK = swrveSpy;
        conversationEventHelper.conversationTransitionedToOtherPage(getMockSwrveConversation(1), "fromPageTag", "toPageTag", "controlTag");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Conversations.Conversation-1.navigation");
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "navigation");
        payload.put("control", "controlTag");
        payload.put("to", "toPageTag");
        payload.put("page", "fromPageTag");
        payload.put("conversation", "1");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    @Test
    public void testConversationWasCancelledByUser() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        conversationEventHelper.swrveConversationSDK = swrveSpy;
        conversationEventHelper.conversationWasCancelledByUser(getMockSwrveConversation(1), "finalPageTag");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Conversations.Conversation-1.cancel");
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "cancel");
        payload.put("page", "finalPageTag");
        payload.put("conversation", "1");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    @Test
    public void testConversationWasFinishedByUser() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        conversationEventHelper.swrveConversationSDK = swrveSpy;
        conversationEventHelper.conversationWasFinishedByUser(getMockSwrveConversation(1), "endPageTag", "endControlTag");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Conversations.Conversation-1.done");
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "done");
        payload.put("control", "endControlTag");
        payload.put("page", "endPageTag");
        payload.put("conversation", "1");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    @Test
    public void testConversationWasStartedByUser() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        conversationEventHelper.swrveConversationSDK = swrveSpy;
        conversationEventHelper.conversationWasStartedByUser(getMockSwrveConversation(1), "pageTag");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Conversations.Conversation-1.start");
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "start");
        payload.put("page", "pageTag");
        payload.put("conversation", "1");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    // NOTE: Update this JSON when increasing the conversation version in the SDK
    @Test
    public void testSwrveConversationVersionFiltered() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_versions.json");
        assertNotNull(swrveSpy.campaigns);
        // One campaign with no version (defaulted to 1), another campaign with v1, and another high version is not loaded
        assertThat("Only 2 valid conversations can be parsed in this test. The raw json used in this test should contain one conversation that should not be parsed.\n " +
                "If current feature increments the version, then be sure the raw json in this test gets incremented also.", swrveSpy.campaigns.size(), equalTo(2));
    }

    @Test
    public void testSwrveConversationDeviceFiltered() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_filters.json");
        assertNotNull(swrveSpy.campaigns);
        // One with no requirements, another with "android" and a campaign with "ios_permissions" that will be rejected.
        assertThat(swrveSpy.campaigns.size(), equalTo(2));
    }

    private SwrveConversation getMockSwrveConversation(int id) {
        SwrveConversationCampaign campaign = mock(SwrveConversationCampaign.class);
        SwrveConversation conversation = mock(SwrveConversation.class);
        when(conversation.getId()).thenReturn(id);
        when(conversation.getCampaign()).thenReturn(campaign);
        return conversation;
    }
}
