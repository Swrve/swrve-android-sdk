package com.swrve.sdk;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_INFLUENCED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class SwrvePushServiceManagerTest extends SwrveBaseTest {

    private int dummyIconResource = 12345;
    private NotificationChannel dummyChannel = null;

    @Test
    public void testNotificationConfigAccentColorResourceId() {

        int colorResourceId = com.swrve.sdk.test.R.color.test_color_green;
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel)
                .accentColorResourceId(colorResourceId);
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);

        assertNumberOfNotification(0); // 0 to begin with because nothing has been processed
        sendSimpleBundleToPushServiceManager();
        assertNumberOfNotification(1); // there can only be one notification for this test

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(Color.parseColor("#217913"), notifications.get(0).color);
    }

    @Test
    public void testNotificationConfigAccentColor() {

        int colorARGB = ContextCompat.getColor(mActivity, com.swrve.sdk.test.R.color.test_color_green);
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel)
                .accentColorResourceId(colorARGB);
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);

        assertNumberOfNotification(0); // 0 to begin with because nothing has been processed
        sendSimpleBundleToPushServiceManager();
        assertNumberOfNotification(1); // there can only be one notification for this test

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(Color.parseColor("#217913"), notifications.get(0).color);
    }

    @Test
    public void testNotificationCustomFilterSuppress() {

        // create pointer to customFilter and change it throughout the test.
        SwrveNotificationCustomFilter customFilter = null; // default is null
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationCustomFilter(customFilter);

        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);

        assertNumberOfNotification(0); // 0 to begin with because nothing has been processed

        sendSimpleBundleToPushServiceManager();
        assertNumberOfNotification(1); // +1 because customFilter is null and default implementation is to return same notification

        customFilter = (builder, id, jsonPayload) -> null; // returning null here will suppress it
        notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationCustomFilter(customFilter);
        SwrveSDK.getConfig().setNotificationConfig(notificationConfig.build());
        sendSimpleBundleToPushServiceManager();
        assertNumberOfNotification(1); // still 1 because new customFilter has suppressed it

        customFilter = (builder, id, jsonPayload) -> builder.build();
        notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationCustomFilter(customFilter);
        SwrveSDK.getConfig().setNotificationConfig(notificationConfig.build());
        sendSimpleBundleToPushServiceManager();
        assertNumberOfNotification(2); // +1, so total is 2 because new customFilter
    }

    @Test
    public void testNotificationCustomFilterModify() {

        // create pointer to customFilter and change it throughout the test.
        SwrveNotificationCustomFilter customFilter = null; // default is null
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationCustomFilter(customFilter);

        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);

        assertNumberOfNotification(0); // 0 to begin with because nothing has been processed

        sendSimpleBundleToPushServiceManager();
        assertTickerText("plain text");

        // clear all and test modifying a notification
        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        assertNumberOfNotification(0);

        customFilter = (builder, id, jsonPayload) -> builder.setTicker("modified ticker text").build();
        notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationCustomFilter(customFilter);
        SwrveSDK.getConfig().setNotificationConfig(notificationConfig.build());
        sendSimpleBundleToPushServiceManager();
        assertTickerText("modified ticker text");
    }

    @Test
    public void testNotificationCustomFilterJsonPayload() {

        SwrveNotificationCustomFilter customFilterSpy = spy(new MySwrveNotificationCustomFilter());
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationCustomFilter(customFilterSpy);
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);

        assertNumberOfNotification(0); // 0 to begin with because nothing has been processed

        sendSimpleBundleToPushServiceManager();

        // verify the custom payload is delivered to SwrveNotificationCustomFilter
        verify(customFilterSpy, atLeastOnce())
                .filterNotification(any(NotificationCompat.Builder.class), anyInt(), eq("some custom values"));
    }

    @Test
    public void testNotificationAuthenticatedSavedToDB() throws Exception {

        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel);
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        Swrve swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveCommon.setSwrveCommon(swrveSpy);

        assertNumberOfNotification(0);

        Bundle bundleWithAUI = new Bundle();
        bundleWithAUI.putString(SwrveNotificationConstants.SWRVE_AUTH_USER_KEY, SwrveSDK.getUserId());
        bundleWithAUI.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        String json = "{\n" +
                "\"notification_id\": 123,\n" +
                "\"title\": \"title\",\n" +
                "\"subtitle\": \"subtitle\",\n" +
                "\"version\": 1,\n" +
                "\"expanded\": {" +
                "\"title\": \"expanded title\",\n" +
                "\"body\": \"expanded body\"}" +
                "}";
        bundleWithAUI.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundleWithAUI.putString(SwrveNotificationConstants.TEXT_KEY, "plain text");
        bundleWithAUI.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, "some custom values");
        bundleWithAUI.putString("sound", "default");

        SwrvePushServiceManager pushServiceManager = new SwrvePushServiceManager(mActivity);
        pushServiceManager.processMessage(bundleWithAUI);

        assertNumberOfNotification(1);

        Mockito.verify(swrveSpy).saveNotificationAuthenticated(123);
    }

    @Test
    public void testNonNotificationAuthenticatedNotSavedToDB() throws Exception {

        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel);
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        Swrve swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveCommon.setSwrveCommon(swrveSpy);

        assertNumberOfNotification(0);

        Bundle bundleWithoutAUI = new Bundle();
        bundleWithoutAUI.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        String json = "{\n" +
                "\"notification_id\": 123,\n" +
                "\"title\": \"title\",\n" +
                "\"subtitle\": \"subtitle\",\n" +
                "\"version\": 1,\n" +
                "\"expanded\": {" +
                "\"title\": \"expanded title\",\n" +
                "\"body\": \"expanded body\"}" +
                "}";
        bundleWithoutAUI.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundleWithoutAUI.putString(SwrveNotificationConstants.TEXT_KEY, "plain text");
        bundleWithoutAUI.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, "some custom values");
        bundleWithoutAUI.putString("sound", "default");

        SwrvePushServiceManager pushServiceManager = new SwrvePushServiceManager(mActivity);
        pushServiceManager.processMessage(bundleWithoutAUI);

        assertNumberOfNotification(1);

        Mockito.verify(swrveSpy, never()).saveNotificationAuthenticated(anyInt());
    }

    @Test
    public void testSilentPush() throws Exception {

        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        Swrve swrveSpy = spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration

        SwrveSilentPushListener silentPushListenerMock = Mockito.mock(SwrveSilentPushListener.class);
        ISwrveCommon swrveCommonSpy = mock(ISwrveCommon.class);
        SwrveCommon.setSwrveCommon(swrveCommonSpy);
        doReturn(silentPushListenerMock).when(swrveCommonSpy).getSilentPushListener();

        SwrvePushServiceManager pushServiceManager = new SwrvePushServiceManager(mActivity);
        SwrvePushServiceManager pushServiceManagerSpy = spy(pushServiceManager);

        // Send some valid silent pushes
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value1\"}");
        doReturn(SwrveTestUtils.parseDate("2017/01/01 0:00")).when(pushServiceManagerSpy).getNow();
        pushServiceManagerSpy.processMessage(bundle);

        ArgumentCaptor<JSONObject> payloadCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(silentPushListenerMock, atLeastOnce()).onSilentPush(any(Context.class), payloadCaptor.capture());
        assertEquals("{\"custom\":\"value1\"}", payloadCaptor.getValue().toString());

        // Silent push 2
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "2");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value2\"}");
        doReturn(SwrveTestUtils.parseDate("2017/01/01 10:00")).when(pushServiceManagerSpy).getNow();
        pushServiceManagerSpy.processMessage(bundle);


        // Silent push 3
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "3");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value3\"}");
        doReturn(SwrveTestUtils.parseDate("2017/01/01 11:00")).when(pushServiceManagerSpy).getNow();
        pushServiceManagerSpy.processMessage(bundle);

        assertNumberOfNotification(0);

        // Init the SDK, should read influence data
        swrveSpy.campaignInfluence = spy(new SwrveCampaignInfluence());
        when(swrveSpy.campaignInfluence.getNow()).thenReturn(SwrveTestUtils.parseDate("2017/01/01 13:00"));
        swrveSpy.init(mActivity);
        swrveSpy.onPause();
        swrveSpy.onResume(mActivity);
        swrveSpy.onPause();
        swrveSpy.onResume(mActivity);

        List<Intent> eventIntents = shadowApplication.getBroadcastIntents();
        assertEquals(1, eventIntents.size());
        Intent eventIntent = eventIntents.get(0);
        ArrayList extras = (ArrayList) eventIntent.getExtras().get("swrve_wakeful_events");
        assertEquals(2, extras.size());
        JSONObject event1 = new JSONObject((String) extras.get(0));
        assertEquals(EVENT_TYPE_GENERIC_CAMPAIGN, event1.get("type"));
        assertEquals(2, event1.get("id"));
        assertEquals(GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, event1.get(GENERIC_EVENT_CAMPAIGN_TYPE_KEY));
        assertEquals(GENERIC_EVENT_ACTION_TYPE_INFLUENCED, event1.get(GENERIC_EVENT_ACTION_TYPE_KEY));
        JSONObject payload1 = event1.getJSONObject("payload");
        assertEquals("540", payload1.get("delta"));

        JSONObject event2 = new JSONObject((String) extras.get(1));
        assertEquals(3, event2.get("id"));
    }

    private void assertNumberOfNotification(int numberOfNotifications) {
        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(numberOfNotifications, notifications.size());
    }

    private void assertTickerText(String tickerText) {
        assertNumberOfNotification(1); // there can only be one notification for this test
        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(tickerText, notifications.get(0).tickerText);
    }

    private void sendSimpleBundleToPushServiceManager() {
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        String json = "{\n" +
                "\"title\": \"title\",\n" +
                "\"subtitle\": \"subtitle\",\n" +
                "\"version\": 1,\n" +
                "\"expanded\": {" +
                "\"title\": \"expanded title\",\n" +
                "\"body\": \"expanded body\"}" +
                "}";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "plain text");
        bundle.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, "some custom values");
        bundle.putString("sound", "default");

        SwrvePushServiceManager pushServiceManager = new SwrvePushServiceManager(mActivity);
        pushServiceManager.processMessage(bundle);
    }

    // this class is because Mockito cannot mock anonymous classes
    class MySwrveNotificationCustomFilter implements SwrveNotificationCustomFilter {
        @Override
        public Notification filterNotification(NotificationCompat.Builder builder, int id, String jsonPayload) {
            return builder.build();
        }
    }
}
