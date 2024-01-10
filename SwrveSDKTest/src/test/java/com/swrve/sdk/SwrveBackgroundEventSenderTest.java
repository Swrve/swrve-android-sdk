package com.swrve.sdk;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.impl.model.WorkSpec;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static com.swrve.sdk.SwrveBackgroundEventSender.DATA_KEY_EVENTS;
import static com.swrve.sdk.SwrveBackgroundEventSender.DATA_KEY_USER_ID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SwrveBackgroundEventSenderTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
        swrveSpy.init(mActivity);
        Mockito.reset(swrveSpy);
    }

    @Test
    public void testGetOneTimeWorkRequest() {

        SwrveBackgroundEventSender backgroundEventSenderSpy = spy(new SwrveBackgroundEventSender(swrveSpy, mActivity));
        ArrayList<String> events = Lists.newArrayList("event1", "event2");
        OneTimeWorkRequest workRequest = backgroundEventSenderSpy.getOneTimeWorkRequest("userId", events);

        WorkSpec workSpec = workRequest.getWorkSpec();
        assertEquals(NetworkType.CONNECTED, workSpec.constraints.getRequiredNetworkType());
        assertEquals("userId", workSpec.input.getString(DATA_KEY_USER_ID));
        assertArrayEquals(new String[]{"event1", "event2"}, workSpec.input.getStringArray(DATA_KEY_EVENTS));
    }

    @Test
    public void testSend() {

        SwrveBackgroundEventSender backgroundEventSenderSpy = spy(new SwrveBackgroundEventSender(swrveSpy, mActivity));
        doNothing().when(backgroundEventSenderSpy).enqueueWorkRequest(any(OneTimeWorkRequest.class));

        ArrayList<String> events = Lists.newArrayList("event1", "event2");
        backgroundEventSenderSpy.send("userId", events);

        verify(backgroundEventSenderSpy, atLeastOnce()).getOneTimeWorkRequest("userId", events);
        verify(backgroundEventSenderSpy, atLeastOnce()).enqueueWorkRequest(any(OneTimeWorkRequest.class));
    }

    @Test
    public void testServiceWithInvalidEventExtras() throws Exception {
        SwrveBackgroundEventSender sender = new SwrveBackgroundEventSender(swrveSpy, mActivity);

        Data data = new Data.Builder()
                .putString(DATA_KEY_USER_ID, "userId")
                .putStringArray(DATA_KEY_EVENTS, null) // null array
                .build();
        int eventsSent = sender.handleSendEvents(data);
        assertEquals(0, eventsSent);

        data = new Data.Builder()
                .putString(DATA_KEY_USER_ID, "userId")
                .putStringArray(DATA_KEY_EVENTS, new String[]{}) // empty array
                .build();
        eventsSent = sender.handleSendEvents(data);
        assertEquals(0, eventsSent);
    }

    @Test
    public void testServiceWithValidExtras() throws Exception {
        SwrveBackgroundEventSender sender = new SwrveBackgroundEventSender(swrveSpy, mActivity);
        ArrayList<String> events = new ArrayList<>();
        events.add("my_awesome_event");
        events.add("my_awesome_event2");
        events.add("my_awesome_event3");
        Data data = new Data.Builder()
                .putString(DATA_KEY_USER_ID, "userId")
                .putStringArray(DATA_KEY_EVENTS, events.toArray(new String[events.size()]))
                .build();
        int eventsSent = sender.handleSendEvents(data);
        assertEquals(3, eventsSent);
    }
}
