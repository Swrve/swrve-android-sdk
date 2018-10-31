package com.swrve.sdk;

import android.os.Build;

import com.google.common.collect.Lists;
import com.swrve.sdk.config.SwrveConfig;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwrveUnitTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
        swrveSpy.init(mActivity);
        Mockito.reset(swrveSpy);
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
        Swrve swrve = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        SwrveTestUtils.setSDKInstance(swrve);
        assertEquals(appVersion, swrve.appVersion);
    }

    @Test
    public void testLanguage() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();

        Locale language1 = Locale.JAPANESE;
        Locale language2 = Locale.CHINESE;
        SwrveConfig config = new SwrveConfig();
        config.setLanguage(language1);
        ISwrve swrve = SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        SwrveTestUtils.setSDKInstance(swrve);
        assertEquals("ja", swrve.getLanguage());
        swrve.setLanguage(language2);
        assertEquals("zh", swrve.getLanguage());
    }

    @Test
    public void testInitialisationAndUserIdGenerated() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();

        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        String userId = SwrveSDK.getUserId();
        assertNotNull(userId);
    }

    @Test
    public void testDeviceInfoQueued() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();

        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        Swrve swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);

        Mockito.verify(swrveSpy, Mockito.atMost(0)).queueDeviceUpdateNow(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()); // device info not queued
        Mockito.verify(swrveSpy, Mockito.atMost(0)).deviceUpdate(Mockito.anyString(),Mockito.any(JSONObject.class));
        swrveSpy.onCreate(mActivity);
        Mockito.verify(swrveSpy, Mockito.atMost(1)).queueDeviceUpdateNow(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()); // device info queued once upon init
        Mockito.verify(swrveSpy, Mockito.atMost(1)).deviceUpdate(Mockito.anyString(),Mockito.any(JSONObject.class));

        swrveSpy.onCreate(mActivity);
        Mockito.verify(swrveSpy, Mockito.atMost(1)).queueDeviceUpdateNow(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()); // device info not queued, because sdk already initialised
        Mockito.verify(swrveSpy, Mockito.atMost(1)).deviceUpdate(Mockito.anyString(),Mockito.any(JSONObject.class));

        swrveSpy.onResume(mActivity);
        Mockito.verify(swrveSpy, Mockito.atMost(1)).queueDeviceUpdateNow(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()); // device info not queued, because sdk already initialised
        Mockito.verify(swrveSpy, Mockito.atMost(1)).deviceUpdate(Mockito.anyString(),Mockito.any(JSONObject.class));

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
        config.setAutoShowMessagesMaxDelay(1000l);
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        Swrve swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.disableAssetsManager(swrveSpy);

        // create instance should not call disableAutoShowAfterDelay and the default for autoShowMessagesEnabled should be false
        Assert.assertEquals("AutoDisplayMessages should be true upon sdk init.", false, swrveSpy.autoShowMessagesEnabled);
        Mockito.verify(swrveSpy, Mockito.atMost(0)).disableAutoShowAfterDelay();

        swrveSpy.onCreate(mActivity);

        // After init of sdk the disableAutoShowAfterDelay should be called and the default for autoShowMessagesEnabled should be true
        Assert.assertEquals("AutoDisplayMessages should be true upon sdk init.", true, swrveSpy.autoShowMessagesEnabled);
        Mockito.verify(swrveSpy, Mockito.atMost(1)).disableAutoShowAfterDelay();

        // sleep 2 seconds and test autoShowMessagesEnabled has been disabled.
        Thread.sleep(2000l);
        Assert.assertEquals("AutoDisplayMessages should be true upon sdk init.", false, swrveSpy.autoShowMessagesEnabled);
    }
}
