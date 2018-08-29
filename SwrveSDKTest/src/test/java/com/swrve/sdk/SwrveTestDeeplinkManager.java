package com.swrve.sdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.ui.SwrveInAppMessageActivity;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.swrve.sdk.ISwrveCommon.CACHE_AD_CAMPAIGNS_DEBUG;
import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_ID_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CONTEXT_ID_KEY;
import static com.swrve.sdk.SwrveDeeplinkManager.SWRVE_AD_MESSAGE;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SwrveTestDeeplinkManager extends SwrveBaseTest {

    private Swrve swrveSpy;

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest

        SwrveDeeplinkManager swrveDeeplinkManager = new SwrveDeeplinkManager(swrveSpy.getContentRequestParams(), swrveSpy.getConfig(), swrveSpy.getContext(), swrveSpy.swrveAssetsManager, swrveSpy.restClient);
        SwrveDeeplinkManager swrveDeeplinkManagerSpy = Mockito.spy(swrveDeeplinkManager);
        swrveSpy.swrveDeeplinkManager = swrveDeeplinkManagerSpy;

        IRESTClient spyRestClient = new IRESTClient() {
            @Override
            public void get(String endpoint, IRESTResponseListener callback) {
            }

            @Override
            public void get(String endpoint, Map<String, String> params, IRESTResponseListener callback) throws UnsupportedEncodingException {
                String response = null;
                if (params.containsValue("295412")) {
                    response = SwrveTestUtils.getAssetAsText(mActivity, "ad_journey_campaign_conversation.json");
                }
                if (params.containsValue("295411")) {
                    response = SwrveTestUtils.getAssetAsText(mActivity, "ad_journey_campaign_message.json");
                }
                callback.onResponse(new RESTResponse(200, response, null));
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback) {
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback, String contentType) {
                String response = null;
                if (endpoint.contains("batch")) {
                    response = "{}";
                }
                callback.onResponse(new RESTResponse(200, response, null));
            }
        };

        swrveSpy.swrveDeeplinkManager.setRestClient(spyRestClient);

        SwrveCommon.setSwrveCommon(swrveSpy);
        swrveSpy.init(mActivity);
    }

    @Test
    public void testIsSwrveDeeplink() throws Exception {

        Bundle bundle = new Bundle();
        String targetUrlKey =  "target_url";

        bundle.putString(targetUrlKey,"swrve://app?");
        assertTrue(SwrveDeeplinkManager.isSwrveDeeplink(bundle) == false);

        bundle.putString(targetUrlKey,"swrve://app?param1=1&param2=2");
        assertTrue(SwrveDeeplinkManager.isSwrveDeeplink(bundle) == false);

        bundle.putString(targetUrlKey,"swrve://app?param1=1&ad_content=2");
        assertTrue(SwrveDeeplinkManager.isSwrveDeeplink(bundle) == true);

        bundle.putString(targetUrlKey,"customer://?param1=1&ad_content=2");
        assertTrue(SwrveDeeplinkManager.isSwrveDeeplink(bundle) == true);
    }

    @Test
    public void testQueueDeeplinkGenericEvent_Queued() throws Exception {

        SwrveDeeplinkManager swrveDeeplinkManager = new SwrveDeeplinkManager(swrveSpy.getContentRequestParams(), swrveSpy.getConfig(), swrveSpy.getContext(), swrveSpy.swrveAssetsManager, swrveSpy.restClient);
        SwrveDeeplinkManager swrveDeeplinkManagerSpy = Mockito.spy(swrveDeeplinkManager);
        swrveSpy.swrveDeeplinkManager = swrveDeeplinkManagerSpy;

        swrveDeeplinkManagerSpy.queueDeeplinkGenericEvent("facebook", "291145", "blackfriday", "install");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> events = ArgumentCaptor.forClass(ArrayList.class);
        Mockito.verify(swrveSpy, Mockito.atLeastOnce()).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), events.capture());

        List<ArrayList> capturedProperties = events.getAllValues();
        String jsonString = capturedProperties.get(0).get(0).toString();
        JSONObject jObj = new JSONObject(jsonString);

        assertTrue(jObj.has("time"));
        assertTrue(jObj.has("seqnum"));
        assertTrue(jObj.get("type").equals(EVENT_TYPE_GENERIC_CAMPAIGN));
        assertTrue(jObj.get("id").equals("-1"));
        assertTrue(jObj.get(GENERIC_EVENT_CAMPAIGN_TYPE_KEY).equals("external_source_facebook"));
        assertTrue(jObj.get(GENERIC_EVENT_ACTION_TYPE_KEY).equals("install"));
        assertTrue(jObj.get(GENERIC_EVENT_CONTEXT_ID_KEY).equals("blackfriday"));
        assertTrue(jObj.get(GENERIC_EVENT_CAMPAIGN_ID_KEY).equals("291145"));
    }

    @Test
    public void testHandleDeeplink_Conversation() throws Exception {

        // 1. Test generic_campaign_event queued / sent
        // 2. Test conversation was shown
        // 3. Test conversation details were written to cache (for QA purposes)

        Bundle bundle = new Bundle();
        bundle.putString("target_url","swrve://app?ad_content=295412&ad_source=facebook&ad_campaign=BlackFriday");

        swrveSpy.swrveDeeplinkManager.handleDeeplink(bundle);

        // 1. Generic Event is queued

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> events = ArgumentCaptor.forClass(ArrayList.class);
        Mockito.verify(swrveSpy, Mockito.atLeastOnce()).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), events.capture());

        List<ArrayList> capturedProperties = events.getAllValues();
        String jsonString = capturedProperties.get(0).get(0).toString();
        JSONObject jObj = new JSONObject(jsonString);

        assertTrue(jObj.has("time"));
        assertTrue(jObj.has("seqnum"));
        assertTrue(jObj.get("type").equals(EVENT_TYPE_GENERIC_CAMPAIGN));
        assertTrue(jObj.get("id").equals("-1"));
        assertTrue(jObj.get(GENERIC_EVENT_CAMPAIGN_TYPE_KEY).equals("external_source_facebook"));
        assertTrue(jObj.get(GENERIC_EVENT_ACTION_TYPE_KEY).equals("reengage"));
        assertTrue(jObj.get(GENERIC_EVENT_CONTEXT_ID_KEY).equals("BlackFriday"));
        assertTrue(jObj.get(GENERIC_EVENT_CAMPAIGN_ID_KEY).equals("295412"));

        // 2. Test Conversation was shown

        await().until(campaignShown());

        ShadowActivity shadowMainActivity = Shadows.shadowOf(mActivity);
        Intent nextIntent = shadowMainActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(nextIntent.getComponent(), new ComponentName(mActivity, ConversationActivity.class));

        SwrveBaseConversation conversation = (SwrveBaseConversation) nextIntent.getSerializableExtra("conversation");
        assertEquals(conversation.getId(), 8587);
        assertThat(conversation.getName(), equalTo("FB Ad Journey Conversation Test"));

        // 3. Conversation Cached

        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        final String userId = swrveCommon.getUserId();
        String adCampaigns = swrveSpy.multiLayerLocalStorage.getCacheEntry(userId, CACHE_AD_CAMPAIGNS_DEBUG);
        JSONObject rootJson = new JSONObject(adCampaigns);
        JSONObject campaignJson = rootJson.getJSONObject("campaign");
        assertEquals(campaignJson.getInt("id"), 295412);
        assertEquals(campaignJson.get("subject"), "Survey");
        assertTrue(campaignJson.has("conversation"));
    }

    @Test
    public void testHandleDeeplink_Message() throws Exception {

        // 1. Test generic_campaign_event queued / sent
        // 2. Test message was shown
        // 3. Test message details were written to cache (for QA purposes)

        Bundle bundle = new Bundle();
        bundle.putString("target_url","swrve://app?ad_content=295411&ad_source=facebook&ad_campaign=BlackFriday");

        swrveSpy.swrveDeeplinkManager.handleDeeplink(bundle);

        //*** 1. Generic Event is queued

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> events = ArgumentCaptor.forClass(ArrayList.class);
        Mockito.verify(swrveSpy, Mockito.atLeastOnce()).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), events.capture());

        List<ArrayList> capturedProperties = events.getAllValues();
        String jsonString = capturedProperties.get(0).get(0).toString();
        JSONObject jObj = new JSONObject(jsonString);

        assertTrue(jObj.has("time"));
        assertTrue(jObj.has("seqnum"));
        assertTrue(jObj.get("type").equals(EVENT_TYPE_GENERIC_CAMPAIGN));
        assertTrue(jObj.get("id").equals("-1"));
        assertTrue(jObj.get(GENERIC_EVENT_CAMPAIGN_TYPE_KEY).equals("external_source_facebook"));
        assertTrue(jObj.get(GENERIC_EVENT_ACTION_TYPE_KEY).equals("reengage"));
        assertTrue(jObj.get(GENERIC_EVENT_CONTEXT_ID_KEY).equals("BlackFriday"));
        assertTrue(jObj.get(GENERIC_EVENT_CAMPAIGN_ID_KEY).equals("295411"));

        // 2. Test Conversation was shown

        await().until(campaignShown());

        ShadowActivity shadowMainActivity = Shadows.shadowOf(mActivity);
        Intent nextIntent = shadowMainActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(nextIntent.getComponent(), new ComponentName(mActivity, SwrveInAppMessageActivity.class));
        Bundle extras = nextIntent.getExtras();
        assertNotNull(extras);

        boolean state = extras.getBoolean(SWRVE_AD_MESSAGE);
        assertTrue(state);

        SwrveMessage message = swrveSpy.getAdMesage();

        assertEquals(message.getId(), 298085);
        assertThat(message.getName(), equalTo("Double format"));

        // 3. Message Cached

        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        final String userId = swrveCommon.getUserId();
        String adCampaigns = swrveSpy.multiLayerLocalStorage.getCacheEntry(userId, CACHE_AD_CAMPAIGNS_DEBUG);
        JSONObject rootJson = new JSONObject(adCampaigns);
        JSONObject campaignJson = rootJson.getJSONObject("campaign");
        assertEquals(campaignJson.getInt("id"), 295411);
        assertEquals(campaignJson.get("subject"), "Facebook Ad Message");
        assertTrue(campaignJson.has("messages"));
    }

    @Test
    public void testHandleDeeplink_ConversationShownOncePerAppLoad() throws Exception {

        Bundle bundle = new Bundle();
        bundle.putString("target_url","swrve://app?ad_content=295412&ad_source=facebook&ad_campaign=BlackFriday");

        swrveSpy.swrveDeeplinkManager.handleDeeplink(bundle);
        verify(swrveSpy.swrveDeeplinkManager, times(1)).loadCampaign("295412","reengage");
        await().until(campaignShown());
        swrveSpy.swrveDeeplinkManager.handleDeeplink(bundle);
        verify(swrveSpy.swrveDeeplinkManager, times(1)).loadCampaign("295412","reengage");
    }

    @Test
    public void testHandleDeeplink_MessagehownOncePerAppLoad() throws Exception {

        Bundle bundle = new Bundle();
        bundle.putString("target_url","swrve://app?ad_content=295411&ad_source=facebook&ad_campaign=BlackFriday");

        swrveSpy.swrveDeeplinkManager.handleDeeplink(bundle);
        verify(swrveSpy.swrveDeeplinkManager, times(1)).loadCampaign("295411","reengage");
        await().until(campaignShown());
        swrveSpy.swrveDeeplinkManager.handleDeeplink(bundle);
        verify(swrveSpy.swrveDeeplinkManager, times(1)).loadCampaign("295411","reengage");
    }

    private Callable<Boolean> campaignShown() {
        return new Callable<Boolean>() {
            public Boolean call() {
                ShadowActivity shadowMainActivity = Shadows.shadowOf(mActivity);
                Intent nextIntent = shadowMainActivity.peekNextStartedActivity();
                return (nextIntent != null);
            }
        };
    }
}
