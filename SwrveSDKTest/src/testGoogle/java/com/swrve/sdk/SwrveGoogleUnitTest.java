package com.swrve.sdk;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class SwrveGoogleUnitTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        swrveSpy.init(mActivity);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testSetRegistrationId() throws Exception {
        Mockito.verify(swrveSpy, Mockito.atMost(1)).queueDeviceInfoNow(Mockito.anyBoolean()); // device info queued once upon init

        swrveSpy.setRegistrationId("reg1");
        assertEquals("reg1", swrveSpy.getRegistrationId());
        Mockito.verify(swrveSpy, Mockito.atMost(2)).queueDeviceInfoNow(Mockito.anyBoolean()); // device info queued again so atMost== 2

        swrveSpy.setRegistrationId("reg1");
        assertEquals("reg1", swrveSpy.getRegistrationId());
        Mockito.verify(swrveSpy, Mockito.atMost(2)).queueDeviceInfoNow(Mockito.anyBoolean()); // device info NOT queued again because the regId has NOT changed so remains atMost== 2

        swrveSpy.setRegistrationId("reg2");
        assertEquals("reg2", swrveSpy.getRegistrationId());
        Mockito.verify(swrveSpy, Mockito.atMost(3)).queueDeviceInfoNow(Mockito.anyBoolean()); // device info queued again because the regId has changed so atMost== 3
    }

}
