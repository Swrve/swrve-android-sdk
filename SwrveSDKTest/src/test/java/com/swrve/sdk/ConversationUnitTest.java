package com.swrve.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.engine.model.ChoiceInputResponse;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.UserInputResult;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveConversationCampaign;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        Mockito.doNothing().when(swrveSpy).downloadAssets(Mockito.anySet()); // assets are manually mocked
        swrveSpy.init(mActivity);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testSwrveConversation() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);
        assertThat(conversation.getPages().size(), equalTo(3));

        ConversationPage conversationPage = conversation.getFirstPage();
        assertNotNull(conversationPage);
        assertThat(conversationPage.getTitle(), equalTo("Page 1 of x"));
    }

    @Test
    public void testGetConversationForEvent() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        assertNull(swrveSpy.getConversationForEvent("some_trigger_that_doesn't_exist", new HashMap<String, String>()));

        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);
        assertThat(conversation.getPages().size(), equalTo(3));
    }

    @Test
    public void testConversationAssetsDownload() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        SwrveBaseCampaign swrveCampaign = swrveSpy.campaigns.get(0);
        assertTrue(swrveCampaign instanceof SwrveConversationCampaign);
        SwrveConversation conversation = ((SwrveConversationCampaign) swrveCampaign).getConversation();
        assertEquals(conversation.getCampaign().getId(), swrveCampaign.getId());

        assertTrue(conversation.areAssetsReady(swrveSpy.getAssetsOnDisk()));

        boolean assetDownloaded = false;
        for (ConversationPage conversationPage : conversation.getPages()) {
            for (ConversationAtom conversationAtom : conversationPage.getContent()) {
                if (ConversationAtom.TYPE_CONTENT_IMAGE.equalsIgnoreCase(conversationAtom.getType().toString())) {
                    Content modelContent = (Content) conversationAtom;
                    String filePath = conversation.getCacheDir().getAbsolutePath() + "/" + modelContent.getValue();
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    assertNotNull(bitmap);
                    assetDownloaded = true;
                }
            }
        }
        assertTrue(assetDownloaded);

        swrveSpy.assetsOnDisk.clear();
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
        swrveSpy.storageExecutor.shutdown();
        swrveSpy.storageExecutor.awaitTermination(1, TimeUnit.SECONDS);
        assertConversationEvent("Swrve.Conversations.Conversation-1.visit", "visit", "fromPageTag");
    }

    @Test
    public void testConversationPageWasViewedByUser() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        conversationEventHelper.swrveConversationSDK = swrveSpy;
        conversationEventHelper.conversationPageWasViewedByUser(getMockSwrveConversation(1), "pageTag");
        swrveSpy.storageExecutor.shutdown();
        swrveSpy.storageExecutor.awaitTermination(1, TimeUnit.SECONDS);
        assertConversationEvent("Swrve.Conversations.Conversation-1.impression", "impression", "pageTag");
    }

    @Test
    public void testConversationTransitionedToOtherPage() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        conversationEventHelper.swrveConversationSDK = swrveSpy;
        conversationEventHelper.conversationTransitionedToOtherPage(getMockSwrveConversation(1), "fromPageTag", "toPageTag", "controlTag");
        swrveSpy.storageExecutor.shutdown();
        swrveSpy.storageExecutor.awaitTermination(1, TimeUnit.SECONDS);
        assertConversationEvent("Swrve.Conversations.Conversation-1.navigation", "navigation", "fromPageTag");
    }

    @Test
    public void testConversationWasCancelledByUser() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        conversationEventHelper.swrveConversationSDK = swrveSpy;
        conversationEventHelper.conversationWasCancelledByUser(getMockSwrveConversation(1), "finalPageTag");
        swrveSpy.storageExecutor.shutdown();
        swrveSpy.storageExecutor.awaitTermination(1, TimeUnit.SECONDS);
        assertConversationEvent("Swrve.Conversations.Conversation-1.cancel", "cancel", "finalPageTag");
    }

    @Test
    public void testConversationWasFinishedByUser() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        conversationEventHelper.swrveConversationSDK = swrveSpy;
        conversationEventHelper.conversationWasFinishedByUser(getMockSwrveConversation(1), "endPageTag", "endControlTag");
        swrveSpy.storageExecutor.shutdown();
        swrveSpy.storageExecutor.awaitTermination(1, TimeUnit.SECONDS);
        assertConversationEvent("Swrve.Conversations.Conversation-1.done", "done", "endPageTag");
    }

    @Test
    public void testConversationWasStartedByUser() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversationEventHelper conversationEventHelper = new SwrveConversationEventHelper();
        conversationEventHelper.swrveConversationSDK = swrveSpy;
        conversationEventHelper.conversationWasStartedByUser(getMockSwrveConversation(1), "pageTag");
        swrveSpy.storageExecutor.shutdown();
        swrveSpy.storageExecutor.awaitTermination(1, TimeUnit.SECONDS);
        assertConversationEvent("Swrve.Conversations.Conversation-1.start", "start", "pageTag");
    }

    // NOTE: Update this JSON when increasing the conversation version in the SDK
    @Test
    public void testSwrveConversationVersionFiltered() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_versions.json");
        assertNotNull(swrveSpy.campaigns);
        // One campaign with no version (defaulted to 1), another campaign with v1, v4 is not loaded
        assertThat(swrveSpy.campaigns.size(), equalTo(2));
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

    private void assertConversationEvent(String eventName, String eventNamePayload, String pageTag) {
        Map<Long, String> events = swrveSpy.cachedLocalStorage.getFirstNEvents(50);

        String conversationEvent = null;
        for (Map.Entry<Long, String> entry : events.entrySet()) {
            //Long key = entry.getKey();
            String event = entry.getValue();
            if(event.contains("Conversation")) {
                conversationEvent = event;
                break;
            }
        }
        assertNotNull(conversationEvent);

        Gson gson = new Gson(); // eg: {"type":"event","time":1458144937972,"seqnum":6,"name":"Swrve.Conversations.Conversation-1","payload":{"page":"pageTag","event":"impression","conversation":"1"}}
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> event = gson.fromJson(conversationEvent, type);
        assertEquals(5, event.size());
        assertTrue(event.containsKey("type"));
        assertEquals("event", event.get("type"));
        assertTrue(event.containsKey("name"));
        assertEquals(eventName, event.get("name"));
        assertTrue(event.containsKey("time"));
        assertTrue(event.containsKey("payload"));
        assertTrue(event.get("payload") instanceof Map);
        Map<String, Object> payload = (Map)event.get("payload");
        assertTrue(payload.containsKey("page"));
        assertEquals(pageTag, payload.get("page"));
        assertTrue(payload.containsKey("event"));
        assertEquals(eventNamePayload, payload.get("event"));
        assertTrue(payload.containsKey("conversation"));
        assertEquals("1", payload.get("conversation"));
    }
}
