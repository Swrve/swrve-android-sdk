package com.swrve.sdk;

import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class SwrveBackgroundEventEventSenderTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
        swrveSpy.init(mActivity);
        Mockito.reset(swrveSpy);
    }

    @Test
    public void testServiceWithInvalidEventExtras() throws Exception {
        SwrveBackgroundEventSender sender = new SwrveBackgroundEventSender(swrveSpy, mActivity);

        Bundle bundle = new Bundle();
        bundle.putStringArrayList(SwrveBackgroundEventSender.EXTRA_EVENTS, null); // null array list
        int eventsSent = sender.handleSendEvents(bundle);
        assertEquals(0, eventsSent);

        bundle = new Bundle();
        bundle.putInt(SwrveBackgroundEventSender.EXTRA_EVENTS, 1); // not an array list
        eventsSent = sender.handleSendEvents(bundle);
        assertEquals(0, eventsSent);

        bundle = new Bundle();
        bundle.putStringArrayList(SwrveBackgroundEventSender.EXTRA_EVENTS, new ArrayList<String>()); // empty array list
        eventsSent = sender.handleSendEvents(bundle);
        assertEquals(0, eventsSent);
    }

    @Test
    public void testServiceWithValidExtras() throws Exception {
        SwrveBackgroundEventSender sender = new SwrveBackgroundEventSender(swrveSpy, mActivity);
        ArrayList<String> events = new ArrayList<>();
        events.add("my_awesome_event");
        events.add("my_awesome_event2");
        events.add("my_awesome_event3");
        ArrayList<Integer> messageIds = new ArrayList<>();
        messageIds.add(99);
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(SwrveBackgroundEventSender.EXTRA_EVENTS, events);
        int eventsSent = sender.handleSendEvents(bundle);
        assertEquals(3, eventsSent);
    }
}
