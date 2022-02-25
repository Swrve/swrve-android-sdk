package com.swrve.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

public class SwrveAmazonMessageReceiverTest extends SwrveBaseTest {
    @Test
    public void testRuns() {
        new SwrveAdmMessageReceiver();
        assertTrue(SwrveAdmMessageReceiver.checkADMLatestAvailable());
    }
}
