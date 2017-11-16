package com.swrve.sdk.gcm;

import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveSDK;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.fail;

public class SwrveGcmInstanceIDListenerServiceTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
    }

    @Test
    public void testOnTokenRefresh() throws Exception {
        try {
            SwrveGcmInstanceIDListenerService instanceIDListenerService = Robolectric.setupService(SwrveGcmInstanceIDListenerService.class);
            instanceIDListenerService.onCreate();
            instanceIDListenerService.onTokenRefresh();
        } catch (Exception e) {
            fail("Error SwrveGcmInstanceIDListenerService.testOnTokenRefresh");
        }
    }
}
