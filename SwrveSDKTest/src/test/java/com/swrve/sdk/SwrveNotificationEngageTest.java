package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_ENGAGED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_GEO;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.notifications.model.SwrveNotificationButton;
import com.swrve.sdk.test.MainActivity;
import com.swrve.sdk.test.SplashActivity;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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
        swrveSpy.started = true;

        mShadowActivity.getBroadcastIntents().clear();
    }

    @Test
    public void testOpenActivity() throws Exception {

        SwrveNotificationEngage notificationEngageSpy = processIntent(false);
        verify(notificationEngageSpy, Mockito.atLeastOnce()).openActivity(any(Bundle.class));

        Intent nextIntent = shadowApplication.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals("com.swrve.sdk.test.MainActivity", nextIntent.getComponent().getClassName());
    }

    @Test
    public void testOpenActivityNotCalledWithDisabledProxyActivity() throws Exception {

        // disable the proxy activity
        notificationConfig = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, com.swrve.sdk.test.R.drawable.ic_launcher, null)
                .activityClass(MainActivity.class)
                .useEngagementProxy(false)
                .build();
        doReturn(notificationConfig).when(swrveSpy).getNotificationConfig();
        swrveSpy.config.setNotificationConfig(notificationConfig);

        SwrveNotificationEngage notificationEngageSpy = processIntent(true);
        verify(notificationEngageSpy, Mockito.never()).openActivity(any(Bundle.class));
    }

    private SwrveNotificationEngage processIntent(boolean doNotOpenIntent) {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1234");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH);
        if (doNotOpenIntent) {
            intent.putExtra(SwrveNotificationEngage.DO_NOT_OPEN_INTENT, true);
        }

        SwrveNotificationEngage notificationEngageSpy = spy(new SwrveNotificationEngage(mActivity));
        NotificationMediaManager notificationMediaManagerMock = mock(NotificationMediaManager.class);
        doReturn(notificationMediaManagerMock).when(notificationEngageSpy).getNotificationMediaManager(mActivity);
        notificationEngageSpy.processIntent(intent);
        verify(notificationMediaManagerMock, Mockito.atLeastOnce()).deleteGifUri(intent);
        return notificationEngageSpy;
    }

    @Test
    public void testOpenActivityWithIntentListener() {

        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1234");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH);

        // override the default notification config in the setup
        notificationConfig = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, com.swrve.sdk.test.R.drawable.ic_launcher, null)
                .notificationIntentListener((pushBundle, deeplink) -> {
                    assertNull(deeplink);
                    assertEquals("validBundle", pushBundle.getString(SwrveNotificationConstants.TEXT_KEY));
                    assertEquals("1234", pushBundle.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
                    Intent customIntent = new Intent(ApplicationProvider.getApplicationContext(), SplashActivity.class);
                    customIntent.putExtra("custom_test_k1", "custom_test_v1");
                    return customIntent;
                })
                .build();
        swrveSpy.config.setNotificationConfig(notificationConfig);

        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);
        notificationEngage.processIntent(intent);

        Intent nextIntent = shadowApplication.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals("com.swrve.sdk.test.SplashActivity", nextIntent.getComponent().getClassName());
        assertEquals("custom_test_v1", nextIntent.getStringExtra("custom_test_k1"));
    }

    @Test
    public void testOpenDeeplink() {
        SwrveNotificationEngage notificationEngageSpy = processDeeplinkIntent(false);
        verify(notificationEngageSpy, Mockito.atLeastOnce()).openDeeplink(any(Bundle.class), anyString());

        Intent nextStartedActivity = mShadowActivity.getNextStartedActivity();
        assertNotNull(nextStartedActivity);
        assertEquals("swrve://deeplink/campaigns", nextStartedActivity.getData().toString());
        assertTrue(nextStartedActivity.hasExtra("customdata"));
        assertEquals("customdata_value", nextStartedActivity.getStringExtra("customdata"));
    }

    @Test
    public void testOpenDeeplinkNotCalledWithDisabledProxyActivity() {

        // disable the proxy activity
        notificationConfig = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, com.swrve.sdk.test.R.drawable.ic_launcher, null)
                .activityClass(MainActivity.class)
                .useEngagementProxy(false)
                .build();
        doReturn(notificationConfig).when(swrveSpy).getNotificationConfig();
        swrveSpy.config.setNotificationConfig(notificationConfig);

        SwrveNotificationEngage notificationEngageSpy = processDeeplinkIntent(true);
        verify(notificationEngageSpy, Mockito.never()).openDeeplink(any(Bundle.class), anyString());
    }

    private SwrveNotificationEngage processDeeplinkIntent(boolean doNotOpenIntent) {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("customdata", "customdata_value");
        extras.putString(SwrveNotificationConstants.DEEPLINK_KEY, "swrve://deeplink/campaigns");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        if (doNotOpenIntent) {
            intent.putExtra(SwrveNotificationEngage.DO_NOT_OPEN_INTENT, true);
        }

        SwrveNotificationEngage notificationEngageSpy = spy(new SwrveNotificationEngage(mActivity));
        notificationEngageSpy.processIntent(intent);
        return notificationEngageSpy;
    }

    @Test
    public void testOpenDeeplinkWithIntentListener() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("customdata", "customdata_value");
        extras.putString(SwrveNotificationConstants.DEEPLINK_KEY, "swrve://deeplink/campaigns");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);

        // override the default notification config in the setup
        notificationConfig = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, com.swrve.sdk.test.R.drawable.ic_launcher, null)
                .notificationIntentListener((pushBundle, deeplink) -> {
                    assertEquals("swrve://deeplink/campaigns", deeplink);
                    assertEquals("customdata_value", pushBundle.getString("customdata"));
                    assertEquals("4567", pushBundle.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
                    Intent customIntent = new Intent(ApplicationProvider.getApplicationContext(), SplashActivity.class);
                    customIntent.putExtra("custom_test_k1", "custom_test_v1");
                    customIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    return customIntent;
                })
                .build();
        swrveSpy.config.setNotificationConfig(notificationConfig);

        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);
        notificationEngage.processIntent(intent);

        Intent nextIntent = shadowApplication.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals("com.swrve.sdk.test.SplashActivity", nextIntent.getComponent().getClassName());
        assertEquals("custom_test_v1", nextIntent.getStringExtra("custom_test_k1"));
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, nextIntent.getFlags());
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

        SwrveNotificationEngage notificationEngageSpy = spy(new SwrveNotificationEngage(mActivity));
        NotificationMediaManager notificationMediaManagerMock = mock(NotificationMediaManager.class);
        doReturn(notificationMediaManagerMock).when(notificationEngageSpy).getNotificationMediaManager(mActivity);
        notificationEngageSpy.processIntent(intent);

        verify(notificationMediaManagerMock, Mockito.atLeastOnce()).deleteGifUri(intent);

        Intent nextStartedActivity = mShadowActivity.getNextStartedActivity();
        assertNotNull(nextStartedActivity);
        assertEquals("swrve://deeplink/campaigns", nextStartedActivity.getData().toString());
    }

    @Test
    public void testPressedUrlActionOpenCampaign() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, 1);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY, SwrveNotificationButton.ActionType.OPEN_CAMPAIGN);
        intent.putExtra(SwrveNotificationConstants.CONTEXT_ID_KEY, "1");
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_URL_KEY, "1");

        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);

        assertNull(swrveSpy.notificationSwrveCampaignId);
        notificationEngage.processIntent(intent);
        assertEquals("1", swrveSpy.notificationSwrveCampaignId);
    }

    @Test
    public void testPressedActionClosedNotification() {
        int notificationId = 1; // Your notification ID

        fireNotification("Title", "Message", notificationId);

        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY, SwrveNotificationButton.ActionType.OPEN_URL);
        intent.putExtra(SwrveNotificationConstants.CONTEXT_ID_KEY, "1");
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_URL_KEY, "swrve://deeplink/campaigns");
        intent.putExtra(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, notificationId);

        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);
        notificationEngage.processIntent(intent);

        await().until(notificationPosted(0));
    }

    private void fireNotification(String title, String message, int notificationId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("456", "channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mActivity, "456");
        PendingIntent pendingIntent = PendingIntent.getActivity(mActivity, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setSmallIcon(com.swrve.sdk.test.R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();
        NotificationManager notificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);

        await().until(notificationPosted(0));
        notificationManager.notify(notificationId, notification);

        await().until(notificationPosted(1));
    }

    private Callable<Boolean> notificationPosted(int numberOfNotifications) {
        return () -> {
            NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
            return (notifications.size() == numberOfNotifications);
        };
    }

    private Intent createPushEngagedIntent(Bundle eventPayload, String trackingData, String platform) {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        if (SwrveHelper.isNotNullOrEmpty(trackingData)) {
            extras.putString(SwrveNotificationConstants.TRACKING_DATA_KEY, trackingData);
        }
        if (SwrveHelper.isNotNullOrEmpty(platform)) {
            extras.putString(SwrveNotificationConstants.PLATFORM_KEY, platform);
        }
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH);
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrveNotificationConstants.EVENT_PAYLOAD, eventPayload);
        return intent;
    }

    @Test
    public void testEventPushEngaged() {
        Bundle eventPayload = new Bundle();
        eventPayload.putString("k1", "v1");
        Intent intent = createPushEngagedIntent(eventPayload, null, null);

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
    public void testEventPushEngagedWithTrackingData() {
        Bundle eventPayload = new Bundle();
        eventPayload.putString("k1", "v1");
        eventPayload.putString("trackingData", "1234");
        eventPayload.putString("platform", "android");
        Intent intent = createPushEngagedIntent(eventPayload, "1234", "android");

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

    private Intent createPushButtonEngagedIntent(Bundle eventPayload, String trackingData, String platform) {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4567");
        if (SwrveHelper.isNotNullOrEmpty(trackingData)) {
            extras.putString(SwrveNotificationConstants.TRACKING_DATA_KEY, trackingData);
        }
        if (SwrveHelper.isNotNullOrEmpty(platform)) {
            extras.putString(SwrveNotificationConstants.PLATFORM_KEY, platform);
        }
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
        Intent intent = createPushButtonEngagedIntent(eventPayload, null, null);

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
    public void testEventPushButtonEngagedWithTrackingData() {
        Bundle eventPayload = new Bundle();
        eventPayload.putString("k1", "v1");
        eventPayload.putString("trackingData", "1234");
        eventPayload.putString("platform", "android");
        Intent intent = createPushButtonEngagedIntent(eventPayload, "1234", "android");

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

    @Test
    public void testHandlePushEngagement() {

        SwrveNotificationEngage notificationEngageMock = mock(SwrveNotificationEngage.class);
        doReturn(notificationEngageMock).when(swrveSpy).getNotificationEngage();

        String sid = "4567";
        Intent intent1 = new Intent();
        Bundle bundle1 = new Bundle();
        bundle1.putString("_sid", sid);
        intent1.putExtras(bundle1);
        swrveSpy.handlePushEngagement(intent1);

        Intent intent2 = new Intent();
        Bundle bundle2 = new Bundle();
        bundle2.putString("_sid", sid);
        intent2.putExtras(bundle2);
        swrveSpy.handlePushEngagement(intent2);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(swrveSpy, times(2)).handlePushEngagement(intentCaptor.capture());
        assertTrue(swrveSpy.processedSids.contains(sid));
        assertEquals(1, swrveSpy.processedSids.size());
        verify(swrveSpy, Mockito.times(1)).getNotificationEngage(); // the synchronized block ensures its called only once
        verify(notificationEngageMock, Mockito.times(1)).processIntent(any(Intent.class));
    }
}
