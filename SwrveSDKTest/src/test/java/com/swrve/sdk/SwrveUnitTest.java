package com.swrve.sdk;

import android.content.Intent;
import android.os.Build;

import com.google.common.collect.Lists;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.conversations.ui.ConversationActivity;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SwrveUnitTest extends SwrveBaseTest {

    private static final String iso8601regex = "\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[1-2]\\d|3[0-1])T(?:[0-1]\\d|2[0-3]):[0-5]\\d:[0-5]\\d.\\d\\d\\d(Z|[+]\\d\\d:\\d\\d)";
    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();
        SwrveTestUtils.disableRestClientExecutor(swrveSpy);
        swrveSpy.init(mActivity);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        SwrveHelper.buildModel = Build.MODEL;
    }

    @Test
    public void testInitWithAppVersion() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        String appVersion = "my_version";
        SwrveConfig config = new SwrveConfig();
        config.setAppVersion(appVersion);
        Swrve swrve = SwrveTestUtils.createSpyInstance(config);
        assertEquals(appVersion, swrve.appVersion);
    }

    @Test
    public void testLanguage() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();

        Locale language1 = Locale.JAPANESE;
        Locale language2 = Locale.CHINESE;
        SwrveConfig config = new SwrveConfig();
        config.setLanguage(language1);
        Swrve swrve = SwrveTestUtils.createSpyInstance(config);
        assertEquals("ja", swrve.getLanguage());
        swrve.setLanguage(language2);
        assertEquals("zh", swrve.getLanguage());
    }

    @Test
    public void testInitialisationAndUserIdGenerated() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        swrveSpy = SwrveTestUtils.createSpyInstance();
        String userId = SwrveSDK.getUserId();
        assertNotNull(userId);
    }

    @Test
    public void testDeviceInfoQueued() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();

        swrveSpy = SwrveTestUtils.createSpyInstance();

        verify(swrveSpy, atMost(0)).queueDeviceUpdateNow(anyString(), anyString(), Mockito.anyBoolean()); // device info not queued
        verify(swrveSpy, atMost(0)).deviceUpdate(anyString(), any(JSONObject.class));
        swrveSpy.onCreate(mActivity);
        verify(swrveSpy, atMost(1)).queueDeviceUpdateNow(anyString(), anyString(), Mockito.anyBoolean()); // device info queued once upon init
        verify(swrveSpy, atMost(1)).deviceUpdate(anyString(), any(JSONObject.class));

        swrveSpy.onCreate(mActivity);
        verify(swrveSpy, atMost(1)).queueDeviceUpdateNow(anyString(), anyString(), Mockito.anyBoolean()); // device info not queued, because sdk already initialised
        verify(swrveSpy, atMost(1)).deviceUpdate(anyString(), any(JSONObject.class));

        swrveSpy.onResume(mActivity);
        verify(swrveSpy, atMost(1)).queueDeviceUpdateNow(anyString(), anyString(), Mockito.anyBoolean()); // device info not queued, because sdk already initialised
        verify(swrveSpy, atMost(1)).deviceUpdate(anyString(), any(JSONObject.class));
    }

    @Test
    public void testSessionEnd() {
        SwrveSDK.sessionEnd();
        SwrveTestUtils.assertQueueEvent(swrveSpy, "session_end", null, null);
    }

    @Test
    public void testQueueEvent() {
        SwrveSDK.event("this_name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "this_name");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, null);
    }

    @Test
    public void testQueueEventAndPayload() {
        Map<String, String> payload = new HashMap<>();
        payload.put("k1", "v1");
        SwrveSDK.event("this_name", payload);

        Map<String, Object> expectedParameters = new HashMap<>();
        expectedParameters.put("name", "this_name");
        Map<String, Object> expectedPayload = new HashMap<>();
        payload.put("k1", "v1");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", expectedParameters, expectedPayload);
    }

    @Test
    public void testQueueEventAndInvalidPayload() {
        Mockito.reset(swrveSpy); // reset the setup init calls on swrveSpy so the never() test can be done below
        Map<String, String> payloadInvalid = new HashMap<>();
        payloadInvalid.put(null, null);
        SwrveSDK.event("this_name", payloadInvalid);
        verify(swrveSpy, never()).queueEvent(anyString(), anyMap(), anyMap());

        Map<String, String> payloadValid = new HashMap<>();
        payloadValid.put("valid", null);
        SwrveSDK.event("this_name", payloadValid);
        verify(swrveSpy, Mockito.times(1)).queueEvent(anyString(), anyMap(), anyMap());
    }

    @Test
    public void testPurchase() {
        SwrveSDK.purchase("item_purchase", "€", 99, 5);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("item", "item_purchase");
        parameters.put("cost", "99");
        parameters.put("quantity", "5");
        parameters.put("currency", "€");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "purchase", parameters, null);
    }

    @Test
    public void testIAP() {
        SwrveSDK.iap(1, "com.swrve.product1", 0.99, "USD");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("app_store", "unknown_store");
        parameters.put("cost", 0.99);
        parameters.put("quantity", 1);
        parameters.put("product_id", "com.swrve.product1");
        parameters.put("local_currency", "USD");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "iap", parameters, null);
    }

    @Test
    public void testIAPRewards() {
        SwrveIAPRewards rewards = new SwrveIAPRewards();
        rewards.addCurrency("gold", 203);
        rewards.addCurrency("coins", 105);
        rewards.addItem("sword", 59);
        SwrveSDK.iap(2, "com.swrve.product2", 1.99, "EUR", rewards);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("app_store", "unknown_store");
        parameters.put("cost", 1.99);
        parameters.put("quantity", 2);
        parameters.put("product_id", "com.swrve.product2");
        parameters.put("local_currency", "EUR");
        parameters.put("rewards", rewards.getRewardsJSON().toString());
        SwrveTestUtils.assertQueueEvent(swrveSpy, "iap", parameters, null);
    }

    @Test
    public void testCurrencyGiven() {
        SwrveSDK.currencyGiven("givenCurrency2", 999.56);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("given_currency", "givenCurrency2");
        parameters.put("given_amount", "999.56");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "currency_given", parameters, null);
    }

    @Test
    public void testUserUpdate() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("a0", "b0");
        SwrveSDK.userUpdate(attributes);

        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> attributesJSON = new HashMap<>();
        attributesJSON.put("a0", "b0");
        parameters.put("attributes", new JSONObject(attributesJSON).toString());
        SwrveTestUtils.assertQueueEvent(swrveSpy, "user", parameters, null);
    }

    @Test
    public void testDeviceUpdate() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("attributes", swrveSpy.getDeviceInfo());
        swrveSpy.deviceUpdate(swrveSpy.getUserId(),swrveSpy.getDeviceInfo());
        SwrveTestUtils.assertQueueEvent(swrveSpy, "device_update", parameters, null);
    }

    @Test
    public void testDeviceUpdate_AuthPushConstantSet() throws Exception {
        JSONObject deviceInfo =  swrveSpy.getDeviceInfo();
        assertEquals(deviceInfo.getBoolean("swrve.can_receive_authenticated_push"),true);
    }

    @Test
    public void testModelBlacklist() throws Exception {
        // Test default blacklist
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveConfig config = new SwrveConfig();
        SwrveHelper.buildModel = "Calypso AppCrawler";
        ISwrve sdk = SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        assertTrue(sdk instanceof SwrveEmpty);
        assertNotNull(SwrveSDK.getInstance());

        // Test custom blacklist
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        config = new SwrveConfig();
        config.setModelBlackList(Lists.newArrayList("custom_model"));
        SwrveHelper.buildModel = "custom_model";
        sdk = SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        assertTrue(sdk instanceof SwrveEmpty);
        assertNotNull(SwrveSDK.getInstance());

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveHelper.buildModel = "not_custom_model";
        sdk = SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        assertTrue(sdk instanceof Swrve);
        assertNotNull(SwrveSDK.getInstance());
    }

    @Test
    public void testAutoShowMessagesDelay() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();

        // configure sdk to disable autoShowMessagesEnabled after 1 second
        SwrveConfig config = new SwrveConfig();

        config.setInAppMessageConfig(new SwrveInAppMessageConfig.Builder().autoShowMessagesMaxDelay(500l).build());
        swrveSpy = SwrveTestUtils.createSpyInstance(config);

        // create instance should not call disableAutoShowAfterDelay and the default for autoShowMessagesEnabled should be false
        Assert.assertEquals("AutoDisplayMessages should be true upon sdk init.", false, swrveSpy.autoShowMessagesEnabled);
        verify(swrveSpy, atMost(0)).disableAutoShowAfterDelay();

        swrveSpy.onCreate(mActivity);

        // After init of sdk the disableAutoShowAfterDelay should be called
        verify(swrveSpy, atMost(1)).disableAutoShowAfterDelay();

        // sleep 2 seconds and test autoShowMessagesEnabled has been disabled.
        Thread.sleep(1000l);
        Assert.assertEquals("AutoDisplayMessages should be true upon sdk init.", false, swrveSpy.autoShowMessagesEnabled);
    }

    @Test
    public void testGetJoined() throws Exception {

        assertTrue("Test getting the joined value when sdk has been initialised.", swrveSpy.initialised);
        String joined1 = swrveSpy.getJoined();
        assertTrue("Joined should not be null or empty", SwrveHelper.isNotNullOrEmpty(joined1));
        long joinedLong = new Long(joined1);
        assertTrue("Joined should be greater than zero", joinedLong > 0);

        // recreate the sdk but make sure its not initialised.
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);

        assertFalse("Test getting the joined value when sdk has NOT been initialised.", swrveSpy.initialised);
        String joined2 = swrveSpy.getJoined();
        assertEquals(joined2, joined1);
    }

    @Test
    public void testStartStopCampaignsAndResourcesTimer() {

        doNothing().when(swrveSpy).shutdownCampaignsAndResourcesTimer();
        doNothing().when(swrveSpy).startCampaignsAndResourcesTimer(anyBoolean());

        // blank to begin with until onResume is called
        assertEquals("", swrveSpy.foregroundActivity);

        // Resume MainActivity
        swrveSpy.onResume(mActivity);

        // verify MainActivity is foregroundActivity and startCampaignsAndResourcesTimer is called
        assertEquals(mActivity.getClass().getCanonicalName(), swrveSpy.foregroundActivity);
        verify(swrveSpy, atLeastOnce()).startCampaignsAndResourcesTimer(true);

        // Resume ConversationActivity
        Intent intent = new Intent(RuntimeEnvironment.application, ConversationActivity.class);
        ActivityController<ConversationActivity> activityController = Robolectric.buildActivity(ConversationActivity.class, intent);
        ConversationActivity conversationActivity = activityController.get();
        swrveSpy.onResume(conversationActivity);

        // verify conversationActivity is foregroundActivity and startCampaignsAndResourcesTimer is called
        assertEquals(conversationActivity.getClass().getCanonicalName(), swrveSpy.foregroundActivity);
        verify(swrveSpy, atLeastOnce()).startCampaignsAndResourcesTimer(false); // session start is false this time

        // Stop main activity
        swrveSpy.onStop(mActivity);

        // verify foregroundActivity is still conversationActivity and shutdownCampaignsAndResourcesTimer is not called
        assertEquals(conversationActivity.getClass().getCanonicalName(), swrveSpy.foregroundActivity);
        verify(swrveSpy, never()).shutdownCampaignsAndResourcesTimer();

        // Stop conversation activity
        swrveSpy.onStop(conversationActivity);

        // verify foregroundActivity is blank and shutdownCampaignsAndResourcesTimer is called
        assertEquals("", swrveSpy.foregroundActivity);
        verify(swrveSpy, atLeastOnce()).shutdownCampaignsAndResourcesTimer();
    }

    @Test
    public void testSessionStartUponInit() throws Exception {
        // verify that sessionStart is first and then startCampaignsAndResourcesTimer.
        InOrder inOrder = inOrder(swrveSpy, swrveSpy);
        inOrder.verify(swrveSpy, times(1)).sessionStart();
        inOrder.verify(swrveSpy, times(1)).startCampaignsAndResourcesTimer(true);
    }

    @Test
    public void testSessionStart() {
        SwrveSessionListener sessionListenerMock =  mock(SwrveSessionListener.class);
        swrveSpy.sessionListener = sessionListenerMock;
        SwrveSDK.sessionStart();
        verify(swrveSpy, atLeastOnce()).restClientExecutorExecute(any(Runnable.class));
        verify(sessionListenerMock, atLeastOnce()).sessionStarted();
    }

    @Test
    public void testSendSessionStart() throws Exception {

        ISwrveEventListener eventListenerMock = mock(ISwrveEventListener.class);
        swrveSpy.eventListener = eventListenerMock;

        SwrveEventsManager eventsManagerMock =  mock(SwrveEventsManager.class);
        doReturn(eventsManagerMock).when(swrveSpy).getSwrveEventsManager(anyString(), anyString(), anyString());
        doReturn(100).when(swrveSpy).getNextSequenceNumber();

        swrveSpy.sendSessionStart(123);

        String deviceId = swrveSpy.multiLayerLocalStorage.getCacheEntry(swrveSpy.getUserId(), "device_id");
        verify(swrveSpy, atLeastOnce()).getSwrveEventsManager(swrveSpy.getUserId(), deviceId, swrveSpy.getSessionKey());

        verify(swrveSpy, atLeastOnce()).restClientExecutorExecute(any(Runnable.class));
        List<String> list = new ArrayList<>();
        list.add("{\"type\":\"session_start\",\"time\":123,\"seqnum\":100}");
        verify(eventsManagerMock, atLeastOnce()).storeAndSendEvents(list, swrveSpy.multiLayerLocalStorage.getPrimaryStorage());

        // verify eventlistener triggered for auto showing new session campaigns
        verify(eventListenerMock, atLeastOnce()).onEvent("Swrve.session.start", null);
    }

    @Test
    public void testOnResumeNewSession() throws Exception {

        verify(swrveSpy, times(1)).sessionStart(); // init was called in setup so sessionStart already called

        // fast forward time but not far enough for a new session to be started
        long newSessionTime = swrveSpy.lastSessionTick - 100;
        doReturn(newSessionTime).when(swrveSpy).getSessionTime();
        swrveSpy.onResume(mActivity);
        verify(swrveSpy, times(1)).sessionStart(); // session start is ill only once

        // fast forward time for a new session to be started
        newSessionTime = swrveSpy.lastSessionTick + 100;
        doReturn(newSessionTime).when(swrveSpy).getSessionTime();
        swrveSpy.onResume(mActivity);
        // verify that new session "sessionStart" is first and then startCampaignsAndResourcesTimer.
        InOrder inOrder = inOrder(swrveSpy, swrveSpy);
        inOrder.verify(swrveSpy, times(2)).sessionStart();
        inOrder.verify(swrveSpy, times(1)).generateNewSessionInterval();
        inOrder.verify(swrveSpy, times(1)).startCampaignsAndResourcesTimer(true);
        inOrder.verify(swrveSpy, times(1)).disableAutoShowAfterDelay();
    }

    @Test
    public void testUserUpdateDate() throws Exception {

        doNothing().when(swrveSpy).queueEvent(anyString(), anyMap(), anyMap());

        SwrveSDK.userUpdate("a0", new Date());

        ArgumentCaptor<String> eventStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> parametersMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> payloadMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(swrveSpy, times(2)).queueEvent(eventStringCaptor.capture(), parametersMapCaptor.capture(), payloadMapCaptor.capture());

        String eventType = eventStringCaptor.getValue();
        assertEquals("user", eventType);
        Map<String, Object> parameters = parametersMapCaptor.getValue();
        Object attributes = parameters.get("attributes");
        assertNotNull(attributes);
        JSONObject jsonObject = (JSONObject)attributes;
        String date = jsonObject.getString("a0");
        assertNotNull(date);
        Pattern regex = Pattern.compile(iso8601regex);
        assertTrue(regex.matcher(date).matches());
        Map<String, String> payload = payloadMapCaptor.getValue();
        assertNull(payload);
    }

    @Test
    public void testiso8601Regex() {
        Pattern regex = Pattern.compile(iso8601regex);
        String[] badinputs = new String[]{"2015-03-16T23:59:59+00:00", "2015-03-16T23:59:59+00", "2015-03-16T23:59:59+0000", "2015-03-16T23:59:59.000+00", "2015-03-16T23:59:59.000+0000",
                "2015-03-16T23:59:59+09:00", "2015-17-16T23:59:59+10", "2015-03-16T23:59:59-0100", "2015-03-16T23:59:59.000+00", "2015-03-16T23:59:59.000+0000", "2015-03-16T23:59:59.500+00", "2015-03-16T23:59:59.600+0000", "2016-22-11T17:08:50.000Z"};
        String[] goodInputs = new String[]{"2016-03-11T09:29:33.915Z", "2016-02-11T17:08:50.000Z", "2015-03-16T23:59:59.000Z", "2015-03-16T23:59:59.000+00:00", "2015-03-16T23:59:59.999Z", "2015-03-16T23:59:59.999+00:00"};
        verifyRegex(regex, badinputs, goodInputs);
    }

    protected void verifyRegex(Pattern regex, String[] shouldFail, String[] shouldPass){
        for (String candit : shouldFail) {
            Assert.assertFalse(regex.matcher(candit).matches());
        }
        for (String candit : shouldPass) {
            Assert.assertTrue(regex.matcher(candit).matches());
        }
    }
}
