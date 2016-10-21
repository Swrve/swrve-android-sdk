package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.test.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
public class SwrveAmazonConfigTest {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
    }

    @After
    public void tearDown() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testSDKAppStore() throws Exception {
        SwrveConfig config = SwrveSDK.getConfig();
        assertTrue(config.getAppStore() == SwrveAppStore.Amazon);
    }
}
