package com.swrve.sdk;

import android.content.Intent;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swrve.sdk.notifications.model.SwrveNotificationButton;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.manifest.BroadcastReceiverData;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.swrve.sdk.ISwrveCommon.EVENT_PAYLOAD_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_ENGAGED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_GEO;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SwrveNotificationEngageReceiverTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        mShadowActivity.getBroadcastIntents().clear();
    }

    @Test
    public void testReceiverInManifest() {
        List<BroadcastReceiverData> receiverDataList = shadowApplication.getAppManifest().getBroadcastReceivers();
        boolean inManifest = false;
        for (BroadcastReceiverData receiverData : receiverDataList) {
            if (receiverData.getClassName().equals("com.swrve.sdk.SwrveNotificationEngageReceiver")) {
                inManifest = true;
                break;
            }
        }
        assertTrue(inManifest);
    }

    @Test
    public void testReceiverOpenActivity() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1234");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH);

        SwrveNotificationEngageReceiver receiver = new SwrveNotificationEngageReceiver();
        receiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        Intent nextIntent = shadowApplication.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals("com.swrve.sdk.test.MainActivity", nextIntent.getComponent().getClassName());

        List<Intent> broadcastIntents = mShadowActivity.getBroadcastIntents();
        assertNotNull(broadcastIntents);
        assertEquals(2, broadcastIntents.size());
        assertEquals("android.intent.action.CLOSE_SYSTEM_DIALOGS", broadcastIntents.get(1).getAction());
    }

    @Test
    public void testReceiverOpenDeeplink() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("customdata", "customdata_value");
        extras.putString(SwrveNotificationConstants.DEEPLINK_KEY, "swrve://deeplink/campaigns");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);

        SwrveNotificationEngageReceiver pushEngageReceiver = new SwrveNotificationEngageReceiver();
        pushEngageReceiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();

        Intent nextStartedActivity = mShadowActivity.getNextStartedActivity();
        assertNotNull(nextStartedActivity);
        assertEquals("swrve://deeplink/campaigns", nextStartedActivity.getData().toString());
        assertTrue(nextStartedActivity.hasExtra("customdata"));
        assertEquals("customdata_value", nextStartedActivity.getStringExtra("customdata"));
    }

    @Test
    public void testReceiverPressedUrlAction() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, 1);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY, SwrveNotificationButton.ActionType.OPEN_URL);
        intent.putExtra(SwrveNotificationConstants.CONTEXT_ID_KEY, "1");
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_URL_KEY, "swrve://deeplink/campaigns");

        SwrveNotificationEngageReceiver pushEngageReceiver = new SwrveNotificationEngageReceiver();
        pushEngageReceiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();

        Intent nextStartedActivity = mShadowActivity.getNextStartedActivity();
        assertNotNull(nextStartedActivity);
        assertEquals("swrve://deeplink/campaigns", nextStartedActivity.getData().toString());
    }

    @Test
    public void testReceiverPressedActionClosedNotification() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY, SwrveNotificationButton.ActionType.OPEN_URL);
        intent.putExtra(SwrveNotificationConstants.CONTEXT_ID_KEY, "1");
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_URL_KEY, "swrve://deeplink/campaigns");
        intent.putExtra(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, 1);

        SwrveNotificationEngageReceiver pushEngageReceiver = new SwrveNotificationEngageReceiver();
        SwrveNotificationEngageReceiver receiverSpy = Mockito.spy(pushEngageReceiver);
        Mockito.doNothing().when(receiverSpy).closeNotification(1); // assets are manually mocked
        receiverSpy.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);
        Mockito.verify(receiverSpy).closeNotification(1);

        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
    }

    @Test
    public void testEventPushEngaged() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH);
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        Bundle eventPayload = new Bundle();
        eventPayload.putString("k1", "v1");
        intent.putExtra(SwrveNotificationConstants.EVENT_PAYLOAD, eventPayload);

        SwrveNotificationEngageReceiver receiver = new SwrveNotificationEngageReceiver();
        receiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        List<String> events = SwrveTestUtils.getEventsQueued(mShadowActivity);
        assertEquals(1, events.size());
        Map<String, String> expectedPayload = SwrveHelper.getBundleAsMap(eventPayload);
        assertEngagedEvent(events.get(0), "Swrve.Messages.Push-4567.engaged", expectedPayload);
    }

    @Test
    public void testEventGeoEngaged() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, GENERIC_EVENT_CAMPAIGN_TYPE_GEO);
        Bundle eventPayload = new Bundle();
        eventPayload.putString("k1", "v1");
        intent.putExtra(SwrveNotificationConstants.EVENT_PAYLOAD, eventPayload);

        SwrveNotificationEngageReceiver receiver = new SwrveNotificationEngageReceiver();
        receiver.onReceive(mActivity, intent);

        List<String> events = SwrveTestUtils.getEventsQueued(mShadowActivity);
        assertEquals(1, events.size());
        Map<String, String> expectedPayload = SwrveHelper.getBundleAsMap(eventPayload);
        SwrveTestUtils.assertGenericEvent(events.get(0), "", GENERIC_EVENT_CAMPAIGN_TYPE_GEO, GENERIC_EVENT_ACTION_TYPE_ENGAGED, expectedPayload);
    }

    @Test
    public void testEventPushButtonEngaged() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.CONTEXT_ID_KEY, "2");
        intent.putExtra(SwrveNotificationConstants.BUTTON_TEXT_KEY, "btn3");
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY, SwrveNotificationButton.ActionType.DISMISS);
        Bundle eventPayload = new Bundle();
        eventPayload.putString("k1", "v1");
        intent.putExtra(SwrveNotificationConstants.EVENT_PAYLOAD, eventPayload);

        SwrveNotificationEngageReceiver receiver = new SwrveNotificationEngageReceiver();
        receiver.onReceive(mActivity, intent);

        List<String> events = SwrveTestUtils.getEventsQueued(mShadowActivity);
        assertEquals(2, events.size());
        for(String event : events) { // can't guarantee which event is in list first so just iterate through them
            if(event.contains("generic_campaign_event")) {
                Map<String, String> expectedPayload = SwrveHelper.getBundleAsMap(eventPayload);
                expectedPayload.put("buttonText", "btn3");
                SwrveTestUtils.assertGenericEvent(event, "2", GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK, expectedPayload);
            } else {
                Map<String, String> expectedPayload = SwrveHelper.getBundleAsMap(eventPayload);
                assertEngagedEvent(event, "Swrve.Messages.Push-4567.engaged", expectedPayload);
            }
        }
    }

    @Test
    public void testEventGeoButtonEngaged() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.CONTEXT_ID_KEY, "2");
        intent.putExtra(SwrveNotificationConstants.BUTTON_TEXT_KEY, "btn3");
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY, SwrveNotificationButton.ActionType.DISMISS);
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, GENERIC_EVENT_CAMPAIGN_TYPE_GEO);
        Bundle eventPayload = new Bundle();
        eventPayload.putString("k1", "v1");
        intent.putExtra(SwrveNotificationConstants.EVENT_PAYLOAD, eventPayload);

        SwrveNotificationEngageReceiver receiver = new SwrveNotificationEngageReceiver();
        receiver.onReceive(mActivity, intent);

        List<String> events = SwrveTestUtils.getEventsQueued(mShadowActivity);
        assertEquals(2, events.size());
        for(String event : events) { // can't guarantee which event is in list first so just iterate through them
            if(event.contains("button_click")) {
                Map<String, String> expectedPayload = SwrveHelper.getBundleAsMap(eventPayload);
                expectedPayload.put("buttonText", "btn3");
                SwrveTestUtils.assertGenericEvent(event, "2", GENERIC_EVENT_CAMPAIGN_TYPE_GEO, GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK, expectedPayload);
            } else {
                Map<String, String> expectedPayload = SwrveHelper.getBundleAsMap(eventPayload);
                SwrveTestUtils.assertGenericEvent(event, "", GENERIC_EVENT_CAMPAIGN_TYPE_GEO, GENERIC_EVENT_ACTION_TYPE_ENGAGED, expectedPayload);
            }
        }
    }

    private void assertEngagedEvent(String eventJson, String eventName, Map<String, String> expectedPayload) {
        Gson gson = new Gson(); // eg: {"type":"event","time":1466519995192,"name":"Swrve.Messages.Push-1.engaged"}
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> event = gson.fromJson(eventJson, type);
        assertTrue(event.containsKey("type"));
        assertEquals("event", event.get("type"));
        assertTrue(event.containsKey("name"));
        assertEquals(eventName, event.get("name"));
        assertTrue(event.containsKey("time"));
        assertTrue(event.containsKey("seqnum"));

        if (expectedPayload != null && expectedPayload.size() > 0) {
            assertTrue(event.containsKey(EVENT_PAYLOAD_KEY));
            Map<String, String> actualPayload = (Map) event.get(EVENT_PAYLOAD_KEY);
            assertEquals(expectedPayload, actualPayload);
        }
    }
}
