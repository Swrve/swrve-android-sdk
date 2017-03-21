package com.swrve.sdk;

import android.content.BroadcastReceiver;
import android.content.Intent;

import com.swrve.sdk.test.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.IntentServiceController;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwrveWakefulTest extends SwrveBaseTest {

    private ShadowApplication shadowApplication;
    private MainActivity activity;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        activity = Robolectric.buildActivity(MainActivity.class).create().visible().get();
        shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application);
    }

    private static void removeSingletonInstance() throws Exception{
        Field hack = SwrveSDKBase.class.getDeclaredField("instance");
        hack.setAccessible(true);
        hack.set(null, null);
    }

    @After
    public void tearDown() throws Exception {
        removeSingletonInstance();
    }

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
        assertFalse(nextIntent.hasExtra(SwrveWakefulService.EXTRA_EVENTS));
    }

    @Test
    public void testReceiverWithExtras() throws Exception {
        Intent intent = new Intent("com.swrve.sdk.test.swrve.SwrveWakeful");
        intent.putStringArrayListExtra(SwrveWakefulService.EXTRA_EVENTS, new ArrayList<String>());
        assertTrue(shadowApplication.hasReceiverForIntent(intent));

        List<BroadcastReceiver> receiversForIntent = shadowApplication.getReceiversForIntent(intent);
        assertEquals(1, receiversForIntent.size());
        SwrveWakefulReceiver receiver = (SwrveWakefulReceiver) receiversForIntent.get(0);

        receiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);
        Intent nextIntent = shadowApplication.peekNextStartedService();
        assertNotNull(nextIntent);
        assertTrue(nextIntent.hasExtra(SwrveWakefulService.EXTRA_EVENTS));
    }

    @Test
    public void testServiceWithNoExtras() {
        Intent intent = new Intent(activity, SwrveWakefulService.class);
        SwrveWakefulServiceWrapper service = new SwrveWakefulServiceWrapper();
        service.onCreate();
        service.onHandleIntent(intent);
        assertFalse(service.handleSendEventsCalled);
    }

    @Test
    public void testServiceWithInvalidEventExtras() {
        Intent intent = new Intent(activity, SwrveWakefulService.class);
        intent.putStringArrayListExtra(SwrveWakefulService.EXTRA_EVENTS, null); // null array list
        SwrveWakefulServiceWrapper service = new SwrveWakefulServiceWrapper();
        service.onCreate();
        service.onHandleIntent(intent);
        assertFalse(service.handleSendEventsCalled);

        intent = new Intent(activity, SwrveWakefulService.class);
        intent.putExtra(SwrveWakefulService.EXTRA_EVENTS, 1); // not an array list
        service = new SwrveWakefulServiceWrapper();
        service.onCreate();
        service.onHandleIntent(intent);
        assertFalse(service.handleSendEventsCalled);

        intent = new Intent(activity, SwrveWakefulService.class);
        intent.putStringArrayListExtra(SwrveWakefulService.EXTRA_EVENTS, new ArrayList<String>()); // empty array list
        service = new SwrveWakefulServiceWrapper();
        service.onCreate();
        service.onHandleIntent(intent);
        assertFalse(service.handleSendEventsCalled);
    }

    @Test
    public void testServiceWithValidExtras() {
        SwrveSDK.createInstance(activity, 1, "apiKey");
        Intent intent = new Intent(activity, SwrveWakefulService.class);
        ArrayList<String> events = new ArrayList<String>();
        // TODO: This should be real event JSON
        events.add("my_awesome_event");
        events.add("my_awesome_event2");
        events.add("my_awesome_event3");
        ArrayList<Integer> messageIds = new ArrayList<Integer>();
        messageIds.add(99);
        intent.putStringArrayListExtra(SwrveWakefulService.EXTRA_EVENTS, events);
        IntentServiceController<SwrveWakefulServiceWrapper> serviceController = IntentServiceController.of(Robolectric.getShadowsAdapter(), new SwrveWakefulServiceWrapper(), intent);
        serviceController.create();
        serviceController.handleIntent();
        SwrveWakefulServiceWrapper service = serviceController.get();
        assertTrue(service.handleSendEventsCalled);
        assertEquals(3, service.eventsSent);
    }

    class SwrveWakefulServiceWrapper extends SwrveWakefulService {

        boolean handleSendEventsCalled;
        int eventsSent;

        @Override
        protected int handleSendEvents(ArrayList<String> eventsJson) throws Exception {
            handleSendEventsCalled = true;
            eventsSent = super.handleSendEvents(eventsJson);
            return eventsSent;
        }
    }
}
