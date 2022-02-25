package com.swrve.sdk;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.config.SwrveConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

public class SwrveAmazonConfigTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
    }

    @After
    public void tearDown() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testSDKAppStore() {
        SwrveConfig config = SwrveSDK.getConfig();
        assertTrue(config.getAppStore() == SwrveAppStore.Amazon);
    }
}
