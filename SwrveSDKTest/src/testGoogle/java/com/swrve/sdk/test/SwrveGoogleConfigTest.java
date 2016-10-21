package com.swrve.sdk.test;

import com.swrve.sdk.SwrveAppStore;
import com.swrve.sdk.config.SwrveConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
public class SwrveGoogleConfigTest {

    @Test
    public void testSenderId() throws Exception {
        SwrveConfig swrveConfig = SwrveConfig.withPush("senderId");
        assertNotNull(swrveConfig);
        assertEquals("senderId", swrveConfig.getSenderId());

        swrveConfig.setSenderId("diff");
        assertEquals("diff", swrveConfig.getSenderId());
    }

    @Test
    public void testGoogleAdvertisingIdLogging() {
        SwrveConfig swrveConfig = new SwrveConfig();
        assertEquals(false, swrveConfig.isGAIDLoggingEnabled());

        swrveConfig.setGAIDLoggingEnabled(true);
        assertEquals(true, swrveConfig.isGAIDLoggingEnabled());
    }

    @Test
    public void testAndroidIdLogging() {
        SwrveConfig swrveConfig = new SwrveConfig();
        assertEquals(false, swrveConfig.isAndroidIdLoggingEnabled());

        swrveConfig.setAndroidIdLoggingEnabled(true);
        assertEquals(true, swrveConfig.isAndroidIdLoggingEnabled());
    }

    @Test
    public void testSDKAppStore() throws Exception {
        SwrveConfig swrveConfig = new SwrveConfig();
        assertTrue(swrveConfig.getAppStore() == SwrveAppStore.Google);
    }
}
