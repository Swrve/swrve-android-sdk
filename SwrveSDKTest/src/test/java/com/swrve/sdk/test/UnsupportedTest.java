package com.swrve.sdk.test;

import android.os.Build;

import com.swrve.sdk.ISwrve;
import com.swrve.sdk.SwrveBaseEmpty;
import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveIAPRewards;
import com.swrve.sdk.SwrveResourceManager;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.SwrveUserResourcesDiffListener;
import com.swrve.sdk.SwrveUserResourcesListener;
import com.swrve.sdk.config.SwrveConfigBase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.util.ReflectionHelpers;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test that the SDK fails gracefully in unsupported versions (lower than 4.X)
 */
public class UnsupportedTest extends SwrveBaseTest {

    private int originalSDKVersion;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        originalSDKVersion = android.os.Build.VERSION.SDK_INT;
        // Explicitly hack the static field that describes the sdk version
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 15);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        // revert to the original sdk version
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", originalSDKVersion);
    }

    @Test
    public void testSDKDoesNothingAndDoesntCrash() throws JSONException {

        if (SwrveHelper.sdkAvailable()) {
            // This test should only be executed in platforms levels that the sdk does not support. Otherwise fail it.
            fail("UnsupportedTestHelper explicitly tests sdk on devices where it is not meant to run");
        }

        // Add here all the external API calls and objects that a customer can invoke
        ISwrve sdk = SwrveSDK.createInstance(mActivity.getApplication(), 572, "fake_api_key");
        // The SDK is an empty SDK
        assertTrue(sdk instanceof SwrveBaseEmpty);

        Map<String, String> payload = new HashMap<>();
        payload.put("key", "value");
        SwrveIAPRewards rewards = new SwrveIAPRewards("USD", 99);
        rewards.addCurrency("USD", 100);
        rewards.addItem("stars", 1);
        JSONObject rewardsJson = rewards.getRewardsJSON();
        assertNotNull(rewardsJson);

        sdk.sessionStart();
        sdk.event("new_event");
        sdk.event("event", payload);
        sdk.purchase("item", "USB", 10, 1);
        sdk.currencyGiven("diamonds", 99);
        sdk.userUpdate(payload);
        sdk.iap(1, "productId", 0.99, "USB");
        sdk.iap(1, "productId", 0.99, "USB", rewards);
        SwrveResourceManager resourceManager = sdk.getResourceManager();
        assertNotNull(rewardsJson);

        sdk.setResourcesListener(() -> {
        });
        sdk.getUserResources(new SwrveUserResourcesListener() {
            @Override
            public void onUserResourcesSuccess(Map<String, Map<String, String>> resources, String resourcesAsJSON) {
            }

            @Override
            public void onUserResourcesError(Exception exception) {
            }
        });
        sdk.getUserResourcesDiff(new SwrveUserResourcesDiffListener() {
            @Override
            public void onUserResourcesDiffSuccess(Map<String, Map<String, String>> oldResourcesValues, Map<String, Map<String, String>> newResourcesValues, String resourcesAsJSON) {
            }

            @Override
            public void onUserResourcesDiffError(Exception exception) {
            }
        });
        sdk.sendQueuedEvents();
        sdk.flushToDisk();
        sdk.shutdown();
        sdk.setLanguage(Locale.JAPAN);
        String language = sdk.getLanguage();
        assertEquals("ja-JP", language);

        String apiKey = sdk.getApiKey();
        assertNotNull(apiKey);

        String userId = sdk.getUserId();
        assertNotNull(userId);

        JSONObject deviceInfo = sdk.getDeviceInfo();
        assertNotNull(deviceInfo);

        sdk.refreshCampaignsAndResources();

        String appStoreUrl = sdk.getAppStoreURLForApp(572);
        assertNull(appStoreUrl);

        File cacheDir = sdk.getCacheDir();
        assertNotNull(cacheDir);

        Date initialisedTime = sdk.getInitialisedTime();
        assertNotNull(initialisedTime);

        SwrveConfigBase config = sdk.getConfig();
        assertNotNull(config);

        sdk.stopTracking();
    }
}
