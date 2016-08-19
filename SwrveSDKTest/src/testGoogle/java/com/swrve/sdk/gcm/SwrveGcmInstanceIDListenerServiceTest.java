package com.swrve.sdk.gcm;

import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.SwrveTestUtils;
import com.swrve.sdk.test.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.fail;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
public class SwrveGcmInstanceIDListenerServiceTest {

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
    public void testOnTokenRefresh() throws Exception {
        try {
            SwrveGcmInstanceIDListenerService instanceIDListenerService = new SwrveGcmInstanceIDListenerService();
            instanceIDListenerService.onCreate();
            instanceIDListenerService.onTokenRefresh();
        } catch (Exception e) {
            fail("Error SwrveGcmInstanceIDListenerService.testOnTokenRefresh");
        }
    }
}
