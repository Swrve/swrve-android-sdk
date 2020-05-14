package com.swrve.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SwrveAdmMessageReceiverTest extends SwrveBaseTest {
    @Test
    public void testRuns() {
        new SwrveAdmMessageReceiver();
        assertTrue(SwrveAdmMessageReceiver.checkADMLatestAvailable());
    }
}
