package com.swrve.sdk.test;

import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.config.SwrveConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SwrveFirebaseConfigTest extends SwrveBaseTest {

    @Test
    public void testAdvertisingIdLogging() {
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
    public void testSDKAppStore() {
        SwrveConfig swrveConfig = new SwrveConfig();
        assertTrue(swrveConfig.getAppStore().equals("google"));
    }

    @Test
    public void testObtainRegistrationId() {
        SwrveConfig swrveConfig = new SwrveConfig();
        assertEquals(true, swrveConfig.isPushRegistrationAutomatic());

        swrveConfig.setPushRegistrationAutomatic(false);
        assertEquals(false, swrveConfig.isPushRegistrationAutomatic());
    }
}
