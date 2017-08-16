package com.swrve.sdk.firebase;

import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveSDK;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.fail;

public class SwrveFirebaseInstanceIdServiceTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
    }

    @Test
    public void testOnTokenRefresh() throws Exception {
        try {
            SwrveFirebaseInstanceIdService instanceIDListenerService = Robolectric.setupService(SwrveFirebaseInstanceIdService.class);
            instanceIDListenerService.onCreate();
            instanceIDListenerService.onTokenRefresh();
        } catch (Exception e) {
            fail("Error SwrveFirebaseInstanceIdService.testOnTokenRefresh");
        }
    }
}
