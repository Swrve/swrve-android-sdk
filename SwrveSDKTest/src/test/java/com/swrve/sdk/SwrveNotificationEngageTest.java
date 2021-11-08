package com.swrve.sdk;

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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.notifications.model.SwrveNotificationButton;
import com.swrve.sdk.test.MainActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class SwrveNotificationEngageTest extends SwrveBaseTest {

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
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveCommon.setSwrveCommon(swrveSpy);
        doNothing().when(swrveSpy).sendEventsInBackground(any(Context.class), anyString(), any(ArrayList.class));

        mShadowActivity.getBroadcastIntents().clear();
    }

    @Test
    public void testOpenActivity() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1234");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH);

        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);
        notificationEngage.processIntent(intent);

        Intent nextIntent = shadowApplication.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals("com.swrve.sdk.test.MainActivity", nextIntent.getComponent().getClassName());

        List<Intent> broadcastIntents = mShadowActivity.getBroadcastIntents();
        assertNotNull(broadcastIntents);
        assertEquals(1, broadcastIntents.size());
        assertEquals("android.intent.action.CLOSE_SYSTEM_DIALOGS", broadcastIntents.get(0).getAction());
    }

    @Test
    public void testOpenDeeplink() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("customdata", "customdata_value");
        extras.putString(SwrveNotificationConstants.DEEPLINK_KEY, "swrve://deeplink/campaigns");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);

        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);
        notificationEngage.processIntent(intent);

        Intent nextStartedActivity = mShadowActivity.getNextStartedActivity();
        assertNotNull(nextStartedActivity);
        assertEquals("swrve://deeplink/campaigns", nextStartedActivity.getData().toString());
        assertTrue(nextStartedActivity.hasExtra("customdata"));
        assertEquals("customdata_value", nextStartedActivity.getStringExtra("customdata"));
    }

    @Test
    public void testPressedUrlAction() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, 1);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY, SwrveNotificationButton.ActionType.OPEN_URL);
        intent.putExtra(SwrveNotificationConstants.CONTEXT_ID_KEY, "1");
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_URL_KEY, "swrve://deeplink/campaigns");

        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);
        notificationEngage.processIntent(intent);

        Intent nextStartedActivity = mShadowActivity.getNextStartedActivity();
        assertNotNull(nextStartedActivity);
        assertEquals("swrve://deeplink/campaigns", nextStartedActivity.getData().toString());
    }

    @Test
    public void testPressedActionClosedNotification() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY, SwrveNotificationButton.ActionType.OPEN_URL);
        intent.putExtra(SwrveNotificationConstants.CONTEXT_ID_KEY, "1");
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_URL_KEY, "swrve://deeplink/campaigns");
        intent.putExtra(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, 1);

        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);
        notificationEngage.processIntent(intent);
        SwrveNotificationEngage engageSpy = Mockito.spy(notificationEngage);
        Mockito.doNothing().when(engageSpy).closeNotification(1); // assets are manually mocked
        engageSpy.processIntent(intent);
        Mockito.verify(engageSpy).closeNotification(1);
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

        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);
        notificationEngage.processIntent(intent);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> arrayListCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, Mockito.atLeast(1)).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), arrayListCaptor.capture());
        ArrayList engagementEvents = arrayListCaptor.getAllValues().get(0);
        Map<String, String> expectedPayload = SwrveHelper.getBundleAsMap(eventPayload);
        SwrveNotificationTestUtils.assertEngagedEvent((String) engagementEvents.get(0), "Swrve.Messages.Push-4567.engaged", expectedPayload);
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

        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);
        notificationEngage.processIntent(intent);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> arrayListCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, Mockito.atLeast(1)).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), arrayListCaptor.capture());

        ArrayList events = arrayListCaptor.getAllValues().get(0);
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

        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);
        notificationEngage.processIntent(intent);

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

        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);
        notificationEngage.processIntent(intent);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> arrayListCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, Mockito.atLeast(1)).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), arrayListCaptor.capture());

        ArrayList events = (ArrayList) arrayListCaptor.getAllValues().get(0);
        Map<String, String> expectedPayload = SwrveHelper.getBundleAsMap(eventPayload);
        SwrveTestUtils.assertGenericEvent((String)events.get(0), "", GENERIC_EVENT_CAMPAIGN_TYPE_GEO, GENERIC_EVENT_ACTION_TYPE_ENGAGED, expectedPayload);
    }
}
