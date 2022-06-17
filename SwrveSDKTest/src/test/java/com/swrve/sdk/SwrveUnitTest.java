package com.swrve.sdk;

import static com.ibm.icu.impl.Assert.fail;
import static com.swrve.sdk.ISwrveCommon.CACHE_RESOURCES;
import static com.swrve.sdk.ISwrveCommon.SDK_PREFS_NAME;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.Lists;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.device.ITelephonyManager;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class SwrveUnitTest extends SwrveBaseTest {

    private static final String iso8601regex = "\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[1-2]\\d|3[0-1])T(?:[0-1]\\d|2[0-3]):[0-5]\\d:[0-5]\\d.\\d\\d\\d(Z|[+]\\d\\d:\\d\\d)";
    private Swrve swrveSpy;
    private SwrveBackgroundEventSender backgroundEventSenderMock;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();

        backgroundEventSenderMock = mock(SwrveBackgroundEventSender.class);
        doNothing().when(backgroundEventSenderMock).send(anyString(), anyList());
        doReturn(backgroundEventSenderMock).when(swrveSpy).getSwrveBackgroundEventSender(any(Context.class));

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
    public void testInitWithoutAppVersion() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveConfig config = new SwrveConfig();
        config.setAppVersion(null);
        Swrve swrve = SwrveTestUtils.createSpyInstance(config);
        assertNotNull(swrve.appVersion); // // Check generated app version
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
    public void testSwitchAppId() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        Swrve swrve1 = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        assertTrue(swrve1.isStarted());

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        Swrve swrve2 = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 2, "apiKey_different");
        assertTrue(swrve2.isStarted()); // after switching appId, the sdk will still be started.
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
    public void testDeviceUpdate() {
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
        ISwrve sdk = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);
        assertTrue(sdk instanceof SwrveEmpty);
        assertNotNull(SwrveSDK.getInstance());

        // Test custom blacklist
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        config = new SwrveConfig();
        config.setModelBlackList(Lists.newArrayList("custom_model"));
        SwrveHelper.buildModel = "custom_model";
        sdk = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);
        assertTrue(sdk instanceof SwrveEmpty);
        assertNotNull(SwrveSDK.getInstance());

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveHelper.buildModel = "not_custom_model";
        sdk = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);
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
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);

        assertFalse("Test getting the joined value when sdk has NOT been initialised.", swrveSpy.initialised);
        String joined2 = swrveSpy.getJoined();
        assertEquals(joined2, joined1);
    }

    @Test
    public void testStartStopCampaignsAndResourcesTimer() {

        // blank to begin with until onResume is called
        assertEquals("", swrveSpy.foregroundActivity);

        // Resume MainActivity
        swrveSpy.onResume(mActivity);

        // verify MainActivity is foregroundActivity and startCampaignsAndResourcesTimer is called
        assertEquals(mActivity.getClass().getCanonicalName(), swrveSpy.foregroundActivity);
        verify(swrveSpy, atLeastOnce()).startCampaignsAndResourcesTimer(true);

        // Resume ConversationActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ConversationActivity.class);
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
    public void testSessionStartUponInit() {
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

    @Test
    public void testSendEventsInBackground() throws Exception {

        // setup mocks
        QaUser qaUserMock = mock(QaUser.class);
        QaUser.instance = qaUserMock;

        ArrayList<String> events = Lists.newArrayList("some_event_json");
        swrveSpy.sendEventsInBackground(mActivity, "userId", events);

        verify(backgroundEventSenderMock, times(1)).send("userId", events);
        verify(qaUserMock, atLeastOnce())._wrappedEvents(events);
    }

    @Test
    public void testRestrictedEventName() {
        SwrveSDK.event("valid.event.name1");
        Mockito.verify(swrveSpy, Mockito.times(1))._event("valid.event.name1");

        SwrveSDK.event("Swrve.thisEventIsRestrictedAndWillNotBeQueued");
        Mockito.verify(swrveSpy, Mockito.never())._event("Swrve.thisEventIsRestrictedAndWillNotBeQueued");

        SwrveSDK.event("valid.event.name2");
        Mockito.verify(swrveSpy, Mockito.times(1))._event("valid.event.name2");
    }

    @Test
    public void testUserInfo() throws Exception {
        Settings.Secure.putString(mActivity.getContentResolver(), Settings.Secure.ANDROID_ID, "my_android_id");

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveConfig config = new SwrveConfig();
        config.setAndroidIdLoggingEnabled(true);
        config.setAutoDownloadCampaignsAndResources(false);
        Swrve swrveSpy = SwrveTestUtils.createSpyInstance(config);
        SwrveTestUtils.runSingleThreaded(swrveSpy);

        ITelephonyManager telephonyManagerMock = mock(ITelephonyManager.class);
        doReturn("vodafone IE").when(telephonyManagerMock).getSimOperatorName();
        doReturn("ie").when(telephonyManagerMock).getSimCountryIso();
        doReturn("27201").when(telephonyManagerMock).getSimOperator();
        doReturn(telephonyManagerMock).when(swrveSpy).getTelephonyManager(any(Context.class));

        swrveSpy.onCreate(mActivity);

        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JSONObject> jsonObjectCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(swrveSpy, atLeastOnce()).deviceUpdate(userIdStringCaptor.capture(), jsonObjectCaptor.capture());

        JSONObject attributeDevices = jsonObjectCaptor.getValue();

        // Check that the user update event contains all the device info
        assertTrue(attributeDevices.has("swrve.device_name"));
        assertTrue(attributeDevices.has("swrve.os"));
        assertTrue(attributeDevices.has("swrve.os_version"));
        assertTrue(attributeDevices.has("swrve.device_width"));
        assertTrue(attributeDevices.has("swrve.device_height"));
        assertTrue(attributeDevices.has("swrve.device_dpi"));
        assertTrue(attributeDevices.has("swrve.android_device_xdpi"));
        assertTrue(attributeDevices.has("swrve.android_device_ydpi"));
        assertTrue(attributeDevices.has("swrve.language"));
        assertTrue(attributeDevices.has("swrve.device_region"));
        assertEquals(2, attributeDevices.getString("swrve.device_region").length());
        assertTrue(attributeDevices.has("swrve.utc_offset_seconds"));
        assertTrue(attributeDevices.has("swrve.timezone_name"));
        assertTrue(attributeDevices.has("swrve.sdk_version"));
        assertTrue(attributeDevices.has("swrve.sdk_flavour"));
        assertTrue(attributeDevices.has("swrve.sdk_init_mode"));
        assertTrue(attributeDevices.has("swrve.device_type"));
        assertEquals("auto_auto", attributeDevices.get("swrve.sdk_init_mode"));
        List<String> expectedDeviceTypes = new ArrayList<>();
        expectedDeviceTypes.add("tv");
        expectedDeviceTypes.add("mobile");
        assertTrue(expectedDeviceTypes.contains(attributeDevices.get("swrve.device_type")));
        assertEquals("my_android_id", attributeDevices.get("swrve.android_id"));
        assertEquals(swrveSpy.getConfig().getAppStore(), attributeDevices.get("swrve.app_store"));
        assertTrue(attributeDevices.has("swrve.install_date"));
        // Carrier info
        assertEquals("vodafone IE", attributeDevices.get("swrve.sim_operator.name"));
        assertEquals("ie", attributeDevices.get("swrve.sim_operator.iso_country_code"));
        assertEquals("27201", attributeDevices.get("swrve.sim_operator.code"));

        assertEquals(true, attributeDevices.get("swrve.permission.notifications_enabled"));
        assertEquals(NotificationManagerCompat.IMPORTANCE_NONE, attributeDevices.get("swrve.permission.notifications_importance"));
    }

    @Test
    public void testUserInfoInitMode() throws Exception {

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.AUTO);
        config.setAutoStartLastUser(true);
        Swrve swrveSpy = SwrveTestUtils.createSpyInstance(config);

        assertDeviceUpdateInitMode(swrveSpy, "auto_auto");

        config.setInitMode(SwrveInitMode.AUTO);
        config.setAutoStartLastUser(false);
        assertDeviceUpdateInitMode(swrveSpy, "auto");

        config.setInitMode(SwrveInitMode.MANAGED);
        config.setAutoStartLastUser(true);
        assertDeviceUpdateInitMode(swrveSpy, "managed_auto");

        config.setInitMode(SwrveInitMode.MANAGED);
        config.setAutoStartLastUser(false);
        assertDeviceUpdateInitMode(swrveSpy, "managed");
    }

    private void assertDeviceUpdateInitMode(Swrve swrveSpy, String expectedInitMode) throws Exception {
        JSONObject attributeDevices = swrveSpy._getDeviceInfo();
        assertTrue(attributeDevices.has("swrve.sdk_init_mode"));
        assertEquals(expectedInitMode, attributeDevices.get("swrve.sdk_init_mode"));
    }

    @Test
    public void testUserInfoManagedMode() throws Exception {

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.MANAGED);
        Swrve swrveSpy = SwrveTestUtils.createSpyInstance(config);
        SwrveTestUtils.runSingleThreaded(swrveSpy);

        swrveSpy.onCreate(mActivity);

        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JSONObject> jsonObjectCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(swrveSpy, atLeastOnce()).deviceUpdate(userIdStringCaptor.capture(), jsonObjectCaptor.capture());

        JSONObject attributeDevices = jsonObjectCaptor.getValue();
        assertTrue(attributeDevices.has("swrve.sdk_init_mode"));
        assertEquals("managed_auto", attributeDevices.get("swrve.sdk_init_mode"));
    }

    @Test
    public void testUserInfoManagedAutoMode() throws Exception {

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SharedPreferences settings = mActivity.getSharedPreferences(SDK_PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("trackingState", "").commit(); // blank out the state from the setup()

        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.MANAGED);
        config.setAutoStartLastUser(false); // Note the auto start false
        Swrve swrveSpy = SwrveTestUtils.createSpyInstance(config);
        SwrveTestUtils.runSingleThreaded(swrveSpy);

        swrveSpy.start(mActivity);

        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JSONObject> jsonObjectCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(swrveSpy, atLeastOnce()).deviceUpdate(userIdStringCaptor.capture(), jsonObjectCaptor.capture());

        JSONObject attributeDevices = jsonObjectCaptor.getValue();
        assertTrue(attributeDevices.has("swrve.sdk_init_mode"));
        assertEquals("managed", attributeDevices.get("swrve.sdk_init_mode"));
    }

    @Test
    public void testNoCarrierInfo() throws Exception {

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveConfig config = new SwrveConfig();
        Swrve swrveSpy = SwrveTestUtils.createSpyInstance(config);
        SwrveTestUtils.runSingleThreaded(swrveSpy);

        ITelephonyManager telephonyManagerMock = mock(ITelephonyManager.class);
        doReturn(null).when(telephonyManagerMock).getSimOperatorName();
        doReturn(null).when(telephonyManagerMock).getSimCountryIso();
        doReturn(null).when(telephonyManagerMock).getSimOperator();
        doReturn(telephonyManagerMock).when(swrveSpy).getTelephonyManager(any(Context.class));

        swrveSpy.onCreate(mActivity);

        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JSONObject> jsonObjectCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(swrveSpy, atLeastOnce()).deviceUpdate(userIdStringCaptor.capture(), jsonObjectCaptor.capture());

        JSONObject attributeDevices = jsonObjectCaptor.getValue();

        // Check that the user update event contains all the device info
        assertFalse(attributeDevices.has("swrve.sim_operator.name"));
        assertFalse(attributeDevices.has("swrve.sim_operator.iso_country_code"));
        assertFalse(attributeDevices.has("swrve.sim_operator.code"));
    }

    @Test
    public void testSwrveResourceManager() throws Exception {
        final String cacheFileContents = "[{\"uid\": \"animal.ant\", \"name\": \"ant\", \"cost\": \"5.50\", \"quantity\": \"6\", \"tail\": \"false\"},{\"uid\": \"animal.bear\",\"name\": \"bear\", \"cost\": \"9.99\",\"quantity\": \"20\", \"tail\": \"true\"}]";
        JSONArray resourceJson = new JSONArray(cacheFileContents);

        swrveSpy.onCreate(mActivity);

        SwrveResourceManager resourceManager = swrveSpy.getResourceManager();
        resourceManager.setResourcesFromJSON(resourceJson);

        // Check the resources were written correctly to resource manager and functions to retrieve individual values work as expected
        assertEquals(2, resourceManager.getResources().size());

        SwrveResource resource1 = resourceManager.getResource("animal.ant");
        assertNotNull(resource1);
        assertTrue(resource1.getAttributeKeys().contains("name"));
        assertTrue(resource1.getAttributeKeys().contains("cost"));
        assertTrue(resource1.getAttributeKeys().contains("quantity"));
        assertTrue(resource1.getAttributeKeys().contains("tail"));
        assertEquals("ant", resourceManager.getAttributeAsString("animal.ant", "name", "anonymous"));
        assertEquals("5.50", resourceManager.getAttributeAsString("animal.ant", "cost", "0"));
        assertEquals(6, resourceManager.getAttributeAsInt("animal.ant", "quantity", 0));
        assertFalse(resourceManager.getAttributeAsBoolean("animal.ant", "tail", true));

        SwrveResource resource2 = resourceManager.getResource("animal.bear");
        assertNotNull(resource2);
        assertTrue(resource2.getAttributeKeys().contains("name"));
        assertTrue(resource2.getAttributeKeys().contains("cost"));
        assertTrue(resource2.getAttributeKeys().contains("quantity"));
        assertTrue(resource2.getAttributeKeys().contains("tail"));
        assertEquals("bear", resourceManager.getAttributeAsString("animal.bear", "name", "anonymous"));
        assertEquals("9.99", resourceManager.getAttributeAsString("animal.bear", "cost", "0"));
        assertEquals(20, resourceManager.getAttributeAsInt("animal.bear", "quantity", 0));
        assertTrue(resourceManager.getAttributeAsBoolean("animal.bear", "tail", false));

        // Test that when new resources are loaded, old ones are removed correctly
        final String newCacheFileContents = "[{\"uid\": \"animal.ant\", \"name\": \"ant\", \"cost\": \"5.95\", \"quantity\": \"6\", \"tail\": \"false\"}]";
        try {
            resourceJson = new JSONArray(newCacheFileContents);
        } catch (JSONException e) {
            assertTrue(false); // Invalid JSON
        }
        resourceManager.setResourcesFromJSON(resourceJson);

        assertEquals(1, resourceManager.getResources().size());
        assertNotNull(resourceManager.getResource("animal.ant"));
        assertEquals("5.95", resourceManager.getAttributeAsString("animal.ant", "cost", "0"));

        // Check default value is used correctly for unknown resources
        assertEquals(5, resourceManager.getAttributeAsInt("unknown", "invalid", 5));
        assertFalse(resourceManager.getAttributeAsBoolean("unknown", "invalid", false));
        assertEquals("defaultvalue", resourceManager.getAttributeAsString("unknown", "invalid", "defaultvalue"));
        assertEquals("4.5", resourceManager.getAttributeAsString("unknown", "invalid", "4.5"));
    }

    @Test
    public void testGetUserResources() {
        final String originalResponseBody = "[{ 'uid': 'animal.ant', 'name': 'ant', 'cost': '550', 'cost_type': 'gold'}, { 'uid': 'animal.bear', 'name': 'bear', 'cost': '999', 'cost_type': 'gold'}]";
        String userId = swrveSpy.getUserId();
        swrveSpy.multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_RESOURCES, originalResponseBody , swrveSpy.getUniqueKey(userId));
        swrveSpy.getUserResources(new SwrveUserResourcesListener() {
            @Override
            public void onUserResourcesSuccess(Map<String, Map<String, String>> resources, String resourcesAsJSON) {

                assertEquals(2, resources.size());

                try {
                    JSONArray json = new JSONArray(resourcesAsJSON);
                    assertEquals(json.length(), 2);
                    for (int i = 0, j = json.length(); i < j; i++) {
                        JSONObject resource = json.getJSONObject(i);
                        assert (resource.has("uid"));
                        if (resource.getString("uid").equals("animal.ant")) {
                            assertEquals(resource.getString("name"), "ant");
                            assertEquals(resource.getInt("cost"), 550);
                            assertEquals(resource.getString("cost_type"), "gold");
                        } else if (resource.getString("uid").equals("animal.bear")) {
                            assertEquals(resource.getString("name"), "bear");
                            assertEquals(resource.getInt("cost"), 999);
                            assertEquals(resource.getString("cost_type"), "gold");
                        } else {
                            assertFalse(true); // shouldn't be any other resources
                        }
                    }
                } catch (Exception e) {
                    SwrveLogger.e("Exception", e);
                }
            }

            @Override
            public void onUserResourcesError(Exception exception) {
                fail(exception);
            }
        });
    }

    @Ignore("SWRVE-27318 unstable test, so rewrite")
    @Test
    public void testGetUserResourcesDiff() throws Exception {

        final AtomicBoolean waitCallback = new AtomicBoolean(false);
        Mockito.when( swrveSpy.restClientExecutorExecute(Mockito.any(Runnable.class)) ).thenCallRealMethod();

        final String originalResponseBody = "[{ 'uid': 'animal.ant', 'diff': { 'cost': { 'old': '550', 'new': '666' }}}, { 'uid': 'animal.bear', 'diff': { 'level': { 'old': '10', 'new': '9000' }}}]";
        final Set<String> resourceIds = new HashSet<>();
        resourceIds.add("animal.ant");
        resourceIds.add("animal.bear");

        IRESTClient restClientMock = mock(IRESTClient.class);
        doAnswer((Answer<Void>) invocation -> {
            RESTCacheResponseListener callback = (RESTCacheResponseListener) invocation.getArguments()[2];
            callback.onResponse(new RESTResponse(200, originalResponseBody, null));
            return null;
        }).when(restClientMock).get(anyString(), anyMap(), any(IRESTResponseListener.class));
        swrveSpy.restClient = restClientMock;

        swrveSpy.getUserResourcesDiff(new SwrveUserResourcesDiffListener() {
            @Override
            public void onUserResourcesDiffSuccess(Map<String, Map<String, String>> oldResourcesValues, Map<String, Map<String, String>> newResourcesValues, String resourcesAsJSON) {
                assertEquals(originalResponseBody, resourcesAsJSON);
                assertEquals(resourceIds, oldResourcesValues.keySet());
                assertEquals(resourceIds, newResourcesValues.keySet());
                waitCallback.set(true);
            }

            @Override
            public void onUserResourcesDiffError(Exception exception) {
            }
        });

        await().untilTrue(waitCallback);
    }

    @Test
    public void testGetApiKey() {
        swrveSpy.onCreate(mActivity);
        assertEquals("apiKey", swrveSpy.getApiKey());
    }

    @Test
    public void testGetContextActivity() {
        swrveSpy.onCreate(mActivity);
        assertEquals(mActivity, mActivity);
    }

    @Test
    public void testGetConfig() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveConfig config = new SwrveConfig();
        Swrve swrve = SwrveTestUtils.createSpyInstance(config);
        assertEquals(config, swrve.getConfig());
    }

    @Test
    public void testDisableSendQueuedEventsOnResume() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveConfig config = new SwrveConfig();
        config.setSendQueuedEventsOnResume(false);
        swrveSpy = SwrveTestUtils.createSpyInstance(config);
        SwrveTestUtils.runSingleThreaded(swrveSpy);
        swrveSpy.onCreate(mActivity);

        swrveSpy.event("generic_event_1", null);
        swrveSpy.event("generic_event_2", null);
        swrveSpy.event("generic_event_3", null);

        int numberOfStoredEventsBefore = getAllEventsInPrimaryStorage(swrveSpy);
        assertTrue(numberOfStoredEventsBefore >= 3);
        swrveSpy.lastSessionTick = swrveSpy.getNow().getTime() + 200000;
        swrveSpy.onResume(mActivity);

        // should be same amount of events stored after resume called
        int numberOfStoredEventsAfter= getAllEventsInPrimaryStorage(swrveSpy);
        assertEquals(numberOfStoredEventsBefore, numberOfStoredEventsAfter);
    }

    private int getAllEventsInPrimaryStorage(Swrve swrve) {
        return swrve.multiLayerLocalStorage.getPrimaryStorage().getFirstNEvents(Integer.MAX_VALUE, swrve.getUserId()).size();
    }

    @Test
    public void testNoInvalidSignature() {

        final String originalResponseBody = "[]";
        String userId = swrveSpy.getUserId();
        swrveSpy.multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_RESOURCES, originalResponseBody , swrveSpy.getUniqueKey(userId));
        swrveSpy.getUserResources(new SwrveUserResourcesListener() {
            @Override
            public void onUserResourcesSuccess(Map<String, Map<String, String>> resources, String resourcesAsJSON) {
            }
            @Override
            public void onUserResourcesError(Exception exception) {
            }
        });

        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> parametersMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> payloadMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Boolean> triggerEventListenerCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(swrveSpy, atLeastOnce()).queueEvent(userIdStringCaptor.capture(),
                eventStringCaptor.capture(), parametersMapCaptor.capture(), payloadMapCaptor.capture(), triggerEventListenerCaptor.capture());

        assertTrue(eventStringCaptor.getAllValues().size() > 0);
        List<String> events1 = eventStringCaptor.getAllValues();
        List<Map> parameters1 = parametersMapCaptor.getAllValues();
        assertFalse(hasEventName("Swrve.signature_invalid", events1, parameters1));

        // Force a cache invalid, fake modification to resource cache signature
        swrveSpy.multiLayerLocalStorage.getPrimaryStorage().setCacheEntry(userId, CACHE_RESOURCES, "fake_new_content");

        swrveSpy.getUserResources(new SwrveUserResourcesListener() {
            @Override
            public void onUserResourcesSuccess(Map<String, Map<String, String>> resources, String resourcesAsJSON) {
            }
            @Override
            public void onUserResourcesError(Exception exception) {
            }
        });

        verify(swrveSpy, atLeastOnce()).queueEvent(userIdStringCaptor.capture(),
                eventStringCaptor.capture(), parametersMapCaptor.capture(), payloadMapCaptor.capture(), triggerEventListenerCaptor.capture());
        assertTrue(eventStringCaptor.getAllValues().size() > 0);
        List<String> events2 = eventStringCaptor.getAllValues();
        List<Map> parameters2 = parametersMapCaptor.getAllValues();
        assertTrue(hasEventName("Swrve.signature_invalid", events2, parameters2));
    }

    private boolean hasEventName(String eventName, List<String> events, List<Map> parameters) {
        boolean foundEvent = false;
        for (int i = 0; i < events.size(); i++) {
            String event = events.get(i);
            if(event.equals("event")) {
                if(parameters.get(i).containsKey("name")) {
                    if(parameters.get(i).get("name").equals(eventName)) {
                        foundEvent = true;
                    }
                }
            }
        }
        return foundEvent;
    }
    
    @Test
    public void testOnPause() {
        String userId = swrveSpy.getUserId();
        swrveSpy.onPause();
        InOrder inOrder = inOrder(swrveSpy, swrveSpy);
        inOrder.verify(swrveSpy, times(1)).flushToDisk();
        inOrder.verify(swrveSpy, times(1)).generateNewSessionInterval();
        inOrder.verify(swrveSpy, times(1)).saveCampaignsState(userId);
    }

    @Test
    public void testRetrievePersonalizationProperties() {
        Map<String, String> testRealtimeUserProperties = new HashMap<>();
        testRealtimeUserProperties.put("key1", "value1");

        Map<String, String> providerResponse = new HashMap<>();
        providerResponse.put("key2", "value2");

        Map<String, String> messageCenterResponse = new HashMap<>();
        messageCenterResponse.put("key3", "value3");

        // verify when there's nothing return null and don't crash
        Map<String, String> resultProperties = swrveSpy.retrievePersonalizationProperties(null, null);
        assertEquals(null, resultProperties);

        swrveSpy.realTimeUserProperties = testRealtimeUserProperties;

        Map<String, String> expectedProperties = new HashMap<>();
        expectedProperties.put("user.key1", "value1");

        // verify with no callback, just real time user properties
        resultProperties = swrveSpy.retrievePersonalizationProperties(null, null);
        assertEquals(expectedProperties, resultProperties);

        // verify from trigger / setting off personalization provider
        swrveSpy.personalizationProvider = eventPayload -> {
            if (!SwrveHelper.isNullOrEmpty(eventPayload) && eventPayload.containsKey("change_value")) {
                Map<String, String> map = providerResponse;
                map.put("key3", "event_payload");
                return map;
            }
            return providerResponse;
        };

        expectedProperties = new HashMap<>();
        expectedProperties.put("user.key1", "value1");
        expectedProperties.put("key2", "value2");

        resultProperties = swrveSpy.retrievePersonalizationProperties(null, null);
        assertEquals(resultProperties, expectedProperties);

        // verify with event payload
        expectedProperties.put("key3", "event_payload");

        Map<String, String> eventPayload = new HashMap<>();
        eventPayload.put("change_value", "value");
        resultProperties = swrveSpy.retrievePersonalizationProperties(eventPayload, null);
        assertEquals(resultProperties, expectedProperties);

        // verify from message center (directly passing in properties)
        expectedProperties = new HashMap<>();
        expectedProperties.put("user.key1", "value1");
        expectedProperties.put("key3", "value3");

        resultProperties = swrveSpy.retrievePersonalizationProperties(null, messageCenterResponse);
        assertEquals(resultProperties, expectedProperties);
    }
}
