package com.swrve.sdk;

import android.annotation.TargetApi;
import android.os.Build;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.test.BuildConfig;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class SwrveUnitTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
        swrveSpy.init(mActivity);

        Mockito.reset(swrveSpy);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testInitWithAppVersion() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
        String appVersion = "my_version";
        SwrveConfig config = new SwrveConfig();
        config.setAppVersion(appVersion);
        Swrve swrve = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        assertEquals(appVersion, swrve.appVersion);
    }

    @Test
    public void testLanguage() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();

        String strangeLanguage = "strange_language";
        String strangeLanguage2 = "strange_language_other";
        SwrveConfig config = new SwrveConfig();
        config.setLanguage(strangeLanguage);
        ISwrve swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        assertEquals(strangeLanguage, swrve.getLanguage());
        swrve.setLanguage(strangeLanguage2);
        assertEquals(strangeLanguage2, swrve.getLanguage());
    }

    @Test
    public void testInitialisationWithUserId() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();

        ISwrve swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey");
        String userId = swrve.getUserId();
        assertNotNull(userId);

        SwrveTestUtils.removeSwrveSDKSingletonInstance();

        SwrveConfig config = new SwrveConfig();
        config.setUserId("custom_user_id");
        swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        String userId2 = swrve.getUserId();
        assertNotSame(userId, userId2);
        assertEquals("custom_user_id", userId2);
    }

    @Test
    public void testInitialisationWithNoId() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();

        ISwrve swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey");
        String userId = swrve.getUserId();
        assertNotNull(userId);

        SwrveTestUtils.removeSwrveSDKSingletonInstance();

        swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey");
        String userId2 = swrve.getUserId();
        assertEquals(userId, userId2);
    }

    @Test
    public void testGetUserIdForced() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
        SwrveConfig config = new SwrveConfig();
        config.setUserId("forced");
        ISwrve swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        assertEquals("forced", swrve.getUserId());
    }

    @Test
    public void testDeviceInfoQueued() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey");
        Swrve swrveSpy = Mockito.spy(swrveReal);
        Mockito.verify(swrveSpy, Mockito.atMost(0)).queueDeviceInfoNow(Mockito.anyBoolean()); // device info not queued
        swrveSpy.onCreate(mActivity);
        Mockito.verify(swrveSpy, Mockito.atMost(1)).queueDeviceInfoNow(Mockito.anyBoolean()); // device info queued once upon init

        swrveSpy.onCreate(mActivity);
        Mockito.verify(swrveSpy, Mockito.atMost(1)).queueDeviceInfoNow(Mockito.anyBoolean()); // device info not queued, because sdk already initialised

        swrveSpy.onResume(mActivity);
        Mockito.verify(swrveSpy, Mockito.atMost(1)).queueDeviceInfoNow(Mockito.anyBoolean()); // device info not queued, because sdk already initialised
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
        parameters.put("cost", Double.valueOf(0.99));
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
        parameters.put("cost", Double.valueOf(1.99));
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

}
