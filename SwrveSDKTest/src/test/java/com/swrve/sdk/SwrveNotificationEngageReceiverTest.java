package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.notifications.model.SwrveNotificationButton;
import com.swrve.sdk.test.MainActivity;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_ENGAGED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_GEO;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class SwrveNotificationEngageReceiverTest extends SwrveBaseTest {

    private SwrveNotificationConfig notificationConfig;
    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        notificationConfig = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, com.swrve.sdk.test.R.drawable.ic_launcher, null)
                .activityClass(MainActivity.class)
                .build();
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig);
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveCommon.setSwrveCommon(swrveSpy);
        doNothing().when(swrveSpy).sendEventsInBackground(any(Context.class), anyString(), any(ArrayList.class));

        mShadowActivity.getBroadcastIntents().clear();
    }

    @Test
    public void testConvertPushPayloadToJSONObject_OneLevelDeep() throws Exception {
        // Unfortunately the "_s.JsonPayload" is only included when payload is more than one level deep.
        // This test is for when its one level deep
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, "somefakevalue");
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "456");
        bundle.putString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY, "789");
        bundle.putString("customkey1", "customvalue1");
        bundle.putString("customkey2", "customvalue2");
        bundle.putString("customkey3", "customvalue3");

        JSONObject payload = SwrveHelper.convertPayloadToJSONObject(bundle);

        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertEquals("123", payload.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY));
        assertEquals("somefakevalue", payload.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY));
        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY));
        assertEquals("456", payload.getString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY));
        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY));
        assertEquals("789", payload.getString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY));

        // payloads
        assertTrue(payload.has("customkey1"));
        assertEquals("customvalue1", payload.getString("customkey1"));
        assertTrue(payload.has("customkey2"));
        assertEquals("customvalue2", payload.getString("customkey2"));
        assertTrue(payload.has("customkey3"));
        assertEquals("customvalue3", payload.getString("customkey3"));
    }

    @Test
    public void testConvertPushPayloadToJSONObject_TwoLevelDeep() throws Exception {
        // Unfortunately the "_s.JsonPayload" is only included when payload is more than one level deep.
        // This test is for when its one level deep
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, "somefakevalue");
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "456");
        bundle.putString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY, "789");
        bundle.putString("root1", "value1");
        bundle.putString("root2", "value2");

        String twoDeepJson =
                "{" +
                    "\"g1\":{" +
                        "\"key1\":\"value1\"," +
                        "\"key2\":\"value2\"" +
                    "}," +
                    "\"g2\":{" +
                        "\"key3\":\"value3\"" +
                    "}," +
                    "\"root1\":\"value1\"," +
                    "\"root2\":\"value2\"" +
                "}";
        bundle.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, twoDeepJson);

        JSONObject payload = SwrveHelper.convertPayloadToJSONObject(bundle);

        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertEquals("123", payload.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY));
        assertEquals("somefakevalue", payload.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY));
        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY));
        assertEquals("456", payload.getString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY));
        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY));
        assertEquals("789", payload.getString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY));

        // payloads
        assertTrue(payload.has("root1"));
        assertEquals("value1", payload.getString("root1"));
        assertTrue(payload.has("g1"));
        assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}", payload.getString("g1"));
        assertTrue(payload.has("g2"));
        assertEquals("{\"key3\":\"value3\"}", payload.getString("g2"));
    }

    @Test
    public void testReceiverInManifest() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, SwrveNotificationEngageReceiver.class);
        List<ResolveInfo> receiverDataList = ctx.getPackageManager().queryBroadcastReceivers(intent, 0);
        boolean inManifest = false;
        for (ResolveInfo receiverData : receiverDataList) {
            if (receiverData.activityInfo.name.equals("com.swrve.sdk.SwrveNotificationEngageReceiver")) {
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
        assertEquals(1, broadcastIntents.size());
        assertEquals("android.intent.action.CLOSE_SYSTEM_DIALOGS", broadcastIntents.get(0).getAction());
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
    }

    private Intent createPushEngagedIntent(Bundle eventPayload) {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH);
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.EVENT_PAYLOAD, eventPayload);

        return intent;
    }

    @Test
    public void testEventPushEngaged() {
        Bundle eventPayload = new Bundle();
        eventPayload.putString("k1", "v1");
        Intent intent = createPushEngagedIntent(eventPayload);

        SwrveNotificationEngageReceiver receiver = new SwrveNotificationEngageReceiver();
        receiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> arrayListCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, Mockito.atLeast(1)).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), arrayListCaptor.capture());
        ArrayList engagementEvents = (ArrayList) arrayListCaptor.getAllValues().get(0);
        Map<String, String> expectedPayload = SwrveHelper.getBundleAsMap(eventPayload);
        SwrveNotificationTestUtils.assertEngagedEvent((String) engagementEvents.get(0), "Swrve.Messages.Push-4567.engaged", expectedPayload);
    }

    @Test
    public void testEventGeoEngaged() throws Exception {
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

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> arrayListCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, Mockito.atLeast(1)).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), arrayListCaptor.capture());

        ArrayList events = (ArrayList) arrayListCaptor.getAllValues().get(0);
        Map<String, String> expectedPayload = SwrveHelper.getBundleAsMap(eventPayload);
        SwrveTestUtils.assertGenericEvent((String)events.get(0), "", GENERIC_EVENT_CAMPAIGN_TYPE_GEO, GENERIC_EVENT_ACTION_TYPE_ENGAGED, expectedPayload);
    }

    private Intent createPushButtonEngagedIntent(Bundle eventPayload) {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.CONTEXT_ID_KEY, "2");
        intent.putExtra(SwrveNotificationConstants.BUTTON_TEXT_KEY, "btn3");
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY, SwrveNotificationButton.ActionType.DISMISS);
        intent.putExtra(SwrveNotificationConstants.EVENT_PAYLOAD, eventPayload);

        return intent;
    }

    @Test
    public void testEventPushButtonEngaged() {
        Bundle eventPayload = new Bundle();
        eventPayload.putString("k1", "v1");
        Intent intent = createPushButtonEngagedIntent(eventPayload);

        SwrveNotificationEngageReceiver receiver = new SwrveNotificationEngageReceiver();
        receiver.onReceive(mActivity, intent);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> arrayListCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, Mockito.atLeast(1)).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), arrayListCaptor.capture());

        ArrayList events = (ArrayList) arrayListCaptor.getAllValues().get(0);
        Map<String, String> expectedPayload = SwrveHelper.getBundleAsMap(eventPayload);
        SwrveNotificationTestUtils.assertEngagedEvent((String)events.get(0), "Swrve.Messages.Push-4567.engaged", expectedPayload);
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

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> arrayListCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, Mockito.atLeast(1)).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), arrayListCaptor.capture());

        ArrayList events = (ArrayList) arrayListCaptor.getAllValues().get(0);
        Map<String, String> expectedPayload = SwrveHelper.getBundleAsMap(eventPayload);
        SwrveTestUtils.assertGenericEvent((String)events.get(0), "", GENERIC_EVENT_CAMPAIGN_TYPE_GEO, GENERIC_EVENT_ACTION_TYPE_ENGAGED, expectedPayload);
    }
}
