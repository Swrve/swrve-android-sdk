package com.swrve.sdk;

import android.content.BroadcastReceiver;
import android.content.Intent;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwrveWakefulTest extends SwrveBaseTest {

    @Test
    public void testReceiverNoExtras() throws Exception {
        Intent intent = new Intent("com.swrve.sdk.test.swrve.SwrveWakeful");
        assertTrue(shadowApplication.hasReceiverForIntent(intent));

        List<BroadcastReceiver> receiversForIntent = shadowApplication.getReceiversForIntent(intent);
        assertEquals(1, receiversForIntent.size());
        SwrveWakefulReceiver receiver = (SwrveWakefulReceiver) receiversForIntent.get(0);

        receiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);
        Intent nextIntent = shadowApplication.peekNextStartedService();
        assertNotNull(nextIntent);
        assertFalse(nextIntent.hasExtra(SwrveBackgroundEventSender.EXTRA_EVENTS));
    }

    @Test
    public void testReceiverWithExtras() throws Exception {
        Intent intent = new Intent("com.swrve.sdk.test.swrve.SwrveWakeful");
        intent.putStringArrayListExtra(SwrveBackgroundEventSender.EXTRA_EVENTS, new ArrayList<String>());
        assertTrue(shadowApplication.hasReceiverForIntent(intent));

        List<BroadcastReceiver> receiversForIntent = shadowApplication.getReceiversForIntent(intent);
        assertEquals(1, receiversForIntent.size());
        SwrveWakefulReceiver receiver = (SwrveWakefulReceiver) receiversForIntent.get(0);

        receiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);
        Intent nextIntent = shadowApplication.peekNextStartedService();
        assertNotNull(nextIntent);
        assertTrue(nextIntent.hasExtra(SwrveBackgroundEventSender.EXTRA_EVENTS));
    }
}
