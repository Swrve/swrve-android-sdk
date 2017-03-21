package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertTrue;

public class SwrveAmazonConfigTest extends SwrveBaseTest {

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
