package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_INFLUENCED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_MSG_ID;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_SENT_TIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.test.MainActivity;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SwrvePushManagerTest extends SwrveBaseTest {

    private int dummyIconResource = 12345;
    private NotificationChannel dummyChannel = null;
    private ISwrveCommon swrveCommonSpy;
    private Swrve swrveSpy;
    private SwrvePushManagerImp pushManagerSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        shadowApplication.grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        swrveCommonSpy = mock(ISwrveCommon.class);
        Mockito.doReturn("some_app_version").when(swrveCommonSpy).getAppVersion();
        Mockito.doReturn("some_device_id").when(swrveCommonSpy).getDeviceId();
        Mockito.doReturn("some_session_key").when(swrveCommonSpy).getSessionKey();
        Mockito.doReturn("some_endpoint").when(swrveCommonSpy).getEventsServer();
        Mockito.doReturn(1).when(swrveCommonSpy).getNextSequenceNumber();
        Mockito.doReturn("testUserId").when(swrveCommonSpy).getUserId();
        SwrveNotificationConfig notificationConfig = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, com.swrve.sdk.test.R.drawable.ic_launcher, null)
                .activityClass(MainActivity.class)
                .build();
        Mockito.doReturn(notificationConfig).when(swrveCommonSpy).getNotificationConfig();
        SwrveCommon.setSwrveCommon(swrveCommonSpy);
    }

    @Test
    public void testNotificationConfigAccentColorHex() {

        String colorHexMocked = "#217913";
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel)
                .accentColorHex(colorHexMocked);
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);

        assertNumberOfNotification(0); // 0 to begin with because nothing has been processed
        sendSimpleBundleToPushManager();
        assertNumberOfNotification(1); // there can only be one notification for this test

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(Color.parseColor(colorHexMocked), notifications.get(0).color);
    }

    @Test
    public void testNotificationConfigAccentColorInvalidHex() {

        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).accentColorHex("SomeInvalidHeColor123");;
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);

        assertNumberOfNotification(0); // 0 to begin with because nothing has been processed
        sendSimpleBundleToPushManager();
        assertNumberOfNotification(1); // there can only be one notification for this test

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(0, notifications.get(0).color); // if we don't set color the default there is 0;
    }

    @Test
    public void testNotificationFilterSuppress() {

        // create pointer to customFilter and change it throughout the test.
        SwrveNotificationFilter notificationFilter = null; // default is null
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationFilter(notificationFilter);

        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);

        assertNumberOfNotification(0); // 0 to begin with because nothing has been processed

        sendSimpleBundleToPushManager();
        assertNumberOfNotification(1); // +1 because customFilter is null and default implementation is to return same notification

        notificationFilter = (builder, id, notificationDetails, jsonPayload) -> null; // returning null here will suppress it
        notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationFilter(notificationFilter);
        SwrveSDK.getConfig().setNotificationConfig(notificationConfig.build());
        sendSimpleBundleToPushManager();
        assertNumberOfNotification(1); // still 1 because new customFilter has suppressed it

        notificationFilter = (builder, id, notificationDetails, jsonPayload) -> builder.build();
        notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationFilter(notificationFilter);
        SwrveSDK.getConfig().setNotificationConfig(notificationConfig.build());
        sendSimpleBundleToPushManager();
        assertNumberOfNotification(2); // +1, so total is 2 because new customFilter
    }

    @Test
    public void testNotificationFilterModify() {

        // create pointer to customFilter and change it throughout the test.
        SwrveNotificationFilter notificationFilter = null; // default is null
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationFilter(notificationFilter);

        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);

        assertNumberOfNotification(0); // 0 to begin with because nothing has been processed

        sendSimpleBundleToPushManager();
        assertTickerText("plain text");

        // clear all and test modifying a notification
        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        assertNumberOfNotification(0);

        notificationFilter = (builder, id, notificationDetails, jsonPayload) -> builder.setTicker("modified ticker text").build();
        notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationFilter(notificationFilter);
        SwrveSDK.getConfig().setNotificationConfig(notificationConfig.build());
        sendSimpleBundleToPushManager();
        assertTickerText("modified ticker text");
    }

    @Test
    public void testSimpleNotificationFilterDetails() {

        SwrveNotificationFilter notificationFilter = (builder, id, notificationDetails, jsonPayload) -> {
            assertEquals("title", notificationDetails.getTitle());
            assertEquals("expanded title", notificationDetails.getExpandedTitle());
            assertEquals("plain text", notificationDetails.getBody());
            assertEquals("expanded body", notificationDetails.getExpandedBody());
            assertNull(notificationDetails.getMediaUrl());
            assertNull(notificationDetails.getMediaBitmap());
            return builder.setTicker("modified ticker text").build();
        };

        SwrveNotificationConfig notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationFilter(notificationFilter).build();
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig);
        SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);

        sendSimpleBundleToPushManager();

        assertTickerText("modified ticker text");
    }

    @Test
    public void testRichNotificationFilterDetails() {

        SwrveNotificationFilter notificationFilter = (builder, id, notificationDetails, jsonPayload) -> {
            assertEquals("title", notificationDetails.getTitle());
            assertEquals("expanded title", notificationDetails.getExpandedTitle());
            assertEquals("body", notificationDetails.getBody());
            assertEquals("expanded body", notificationDetails.getExpandedBody());
            assertEquals("https://media.jpg", notificationDetails.getMediaUrl());
            assertNotNull(notificationDetails.getMediaBitmap());
            return builder.setTicker("modified ticker text").build();
        };

        SwrveNotificationConfig notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationFilter(notificationFilter).build();
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig);
        SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);

        sendRichBundleToPushManager(notificationConfig);

        assertTickerText("modified ticker text");
    }

    @Test
    public void testNotificationFilterJsonPayload() {

        SwrveNotificationFilter notificationFilterSpy = spy(new MySwrveNotificationFilter());
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationFilter(notificationFilterSpy);
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);

        assertNumberOfNotification(0); // 0 to begin with because nothing has been processed

        sendSimpleBundleToPushManager();

        // verify the custom payload is delivered
        verify(notificationFilterSpy, atLeastOnce())
                .filterNotification(any(NotificationCompat.Builder.class), anyInt(), any(SwrveNotificationDetails.class), eq("{\"customKey\":\"customValues\"}"));
    }

    @Test
    public void testNotificationAuthenticatedSavedToDB() throws Exception {

        createSwrveSpy();
        assertNumberOfNotification(0);

        // send authenticated message which should display notification and save it
        sendMessage(true, true);

        assertSendPushDeliveredEvent(true, "");
        verify(pushManagerSpy, times(1)).processNotification(any(Bundle.class), anyString());
        assertNumberOfNotification(1);
        verify(swrveSpy).saveNotificationAuthenticated(123);
    }

    @Test
    public void testNotificationAuthenticatedTrackingStateStopped() throws Exception {

        createSwrveSpy();
        assertNumberOfNotification(0);
        SwrveSDK.stopTracking();

        // send authenticated message in stopped state which should not display notification
        sendMessage(true, true);

        assertSendPushDeliveredEvent(false, "stopped");
        verify(pushManagerSpy, never()).processNotification(any(Bundle.class), anyString());
        assertNumberOfNotification(0);
        verify(swrveSpy, never()).saveNotificationAuthenticated(anyInt());
    }

    @Test
    public void testNotificationTrackingStateStopped() throws Exception {

        createSwrveSpy();
        assertNumberOfNotification(0);
        SwrveSDK.stopTracking();

        // send non authenticated message in stopped state which should display notification
        sendMessage(false, true);

        assertSendPushDeliveredEvent(true, "");
        verify(pushManagerSpy, times(1)).processNotification(any(Bundle.class), anyString());
        assertNumberOfNotification(1);
        verify(swrveSpy, never()).saveNotificationAuthenticated(anyInt());
    }

    @Test
    public void testNotificationPermissionDenied() throws Exception {

        createSwrveSpy();
        assertNumberOfNotification(0);

        // send message in with no notification permission which should not display notification
        sendMessage(false, false);

        assertSendPushDeliveredEvent(false, "permission_denied");
        verify(pushManagerSpy, never()).processNotification(any(Bundle.class), anyString());
        assertNumberOfNotification(0);
        verify(swrveSpy, never()).saveNotificationAuthenticated(anyInt());
        
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> events = ArgumentCaptor.forClass(ArrayList.class);
        Mockito.verify(swrveSpy, Mockito.atLeastOnce()).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), events.capture());

        List<ArrayList> capturedProperties = events.getAllValues();
        String jsonString = capturedProperties.get(0).get(0).toString();
        JSONObject jsonObject = new JSONObject(jsonString);
        assertTrue(jsonObject.has("time"));
        assertTrue(jsonObject.has("seqnum"));
        assertEquals("device_update", jsonObject.get("type"));
        assertEquals("false", jsonObject.get("user_initiated"));
        assertTrue(jsonObject.has("attributes"));
        JSONObject attributes = (JSONObject) jsonObject.get("attributes");
        assertTrue(attributes.length() == 2);
        assertEquals("denied", attributes.get("swrve.permission.android.notification"));
        assertTrue(attributes.has("swrve.permission.notifications_enabled"));
    }

    @Test
    public void testNonNotificationAuthenticatedNotSavedToDB() throws Exception {

        createSwrveSpy();
        assertNumberOfNotification(0);

        // send non authenticated message which should display notification
        sendMessage(false, true);

        assertNumberOfNotification(1);
        verify(swrveSpy, never()).saveNotificationAuthenticated(anyInt());
    }

    @Test
    public void testSilentPush() throws Exception {

        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        Swrve swrveSpy = spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        doNothing().when(swrveSpy).sendEventsInBackground(any(Context.class), anyString(), any(ArrayList.class));

        SwrveSilentPushListener silentPushListenerMock = Mockito.mock(SwrveSilentPushListener.class);
        ISwrveCommon swrveCommonSpy = mock(ISwrveCommon.class);
        SwrveCommon.setSwrveCommon(swrveCommonSpy);
        doReturn(silentPushListenerMock).when(swrveCommonSpy).getSilentPushListener();
        doNothing().when(swrveCommonSpy).sendEventsInBackground(any(Context.class), anyString(), any(ArrayList.class));

        SwrvePushManagerImp pushManagerSpy = Mockito.spy(new SwrvePushManagerImp(mActivity));
        doReturn(Mockito.mock(CampaignDeliveryManager.class)).when(pushManagerSpy).getCampaignDeliveryManager();

        // Send some valid silent pushes
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value1\"}");
        doReturn(SwrveTestUtils.parseDate("2017/01/01 0:00")).when(pushManagerSpy).getNow();
        pushManagerSpy.processMessage(bundle);

        ArgumentCaptor<JSONObject> payloadCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(silentPushListenerMock, atLeastOnce()).onSilentPush(any(Context.class), payloadCaptor.capture());
        assertEquals("{\"custom\":\"value1\"}", payloadCaptor.getValue().toString());

        // Silent push 2
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "2");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value2\"}");
        doReturn(SwrveTestUtils.parseDate("2017/01/01 10:00")).when(pushManagerSpy).getNow();
        pushManagerSpy.processMessage(bundle);


        // Silent push 3
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "3");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value3\"}");
        doReturn(SwrveTestUtils.parseDate("2017/01/01 11:00")).when(pushManagerSpy).getNow();
        pushManagerSpy.processMessage(bundle);

        assertNumberOfNotification(0);

        // Init the SDK, should read influence data
        swrveSpy.campaignInfluence = spy(new SwrveCampaignInfluence());
        when(swrveSpy.campaignInfluence.getNow()).thenReturn(SwrveTestUtils.parseDate("2017/01/01 13:00"));
        swrveSpy.init(mActivity);
        swrveSpy.onPause();
        swrveSpy.onResume(mActivity);
        swrveSpy.onPause();
        swrveSpy.onResume(mActivity);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> arrayListCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, Mockito.atLeast(1)).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), arrayListCaptor.capture());

        ArrayList engagementEvents = (ArrayList) arrayListCaptor.getAllValues().get(0);
        JSONObject event1 = new JSONObject((String) engagementEvents.get(0));
        assertEquals(EVENT_TYPE_GENERIC_CAMPAIGN, event1.get("type"));
        assertEquals(2, event1.get("id"));
        assertEquals(GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, event1.get(GENERIC_EVENT_CAMPAIGN_TYPE_KEY));
        assertEquals(GENERIC_EVENT_ACTION_TYPE_INFLUENCED, event1.get(GENERIC_EVENT_ACTION_TYPE_KEY));
        JSONObject payload1 = event1.getJSONObject("payload");
        assertEquals("540", payload1.get("delta"));

        JSONObject event2 = new JSONObject((String) engagementEvents.get(1));
        assertEquals(3, event2.get("id"));
    }

    private void assertNumberOfNotification(int numberOfNotifications) {
        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(numberOfNotifications, notifications.size());
    }

    private void assertTickerText(String tickerText) {
        assertNumberOfNotification(1); // there can only be one notification for this test
        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(tickerText, notifications.get(0).tickerText);
    }

    private void sendSimpleBundleToPushManager() {
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
        bundle.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, "{ 'customKey': 'customValues' }");
        bundle.putString("sound", "default");

        SwrvePushManagerImp pushManagerSpy = Mockito.spy(new SwrvePushManagerImp(mActivity));
        doReturn(Mockito.mock(CampaignDeliveryManager.class)).when(pushManagerSpy).getCampaignDeliveryManager();
        pushManagerSpy.processMessage(bundle);
    }

    private void sendRichBundleToPushManager(SwrveNotificationConfig notificationConfig) {
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        String json = "{\n" +
                "\"title\": \"title\",\n" +
                "\"subtitle\": \"subtitle\",\n" +
                "\"version\": 1,\n" +
                "\"expanded\": {" +
                    "\"title\": \"expanded title\",\n" +
                    "\"body\": \"expanded body\"}," +
                " \"media\": { \"title\": \"title\",\n" +
                    "\"subtitle\": \"subtitle\",\n" +
                    "\"body\": \"body\",\n" +
                    "\"type\": \"image\",\n" +
                    "\"url\": \"https://media.jpg\",\n" +
                    "\"fallback_type\": \"image\",\n" +
                    "\"fallback_url\": \"https://valid-image.png\",\n" +
                    "\"fallback_sd\": \"https://video.com\"\n }" +
                "}";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "plain text");
        bundle.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, "{ 'customKey': 'customValues' }");
        bundle.putString("sound", "default");

        SwrvePushManagerImp pushManagerSpy = Mockito.spy(new SwrvePushManagerImp(mActivity));
        SwrveNotificationBuilder notificationBuilderSpy = Mockito.spy(new SwrveNotificationBuilder(mActivity, notificationConfig));
        Mockito.doReturn(notificationBuilderSpy).when(pushManagerSpy).getSwrveNotificationBuilder();
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        Mockito.doReturn(bmp).when(notificationBuilderSpy).getImageFromUrl(anyString());
        doReturn(Mockito.mock(CampaignDeliveryManager.class)).when(pushManagerSpy).getCampaignDeliveryManager();

        pushManagerSpy.processMessage(bundle);
    }

    // this class is because Mockito cannot mock anonymous classes
    class MySwrveNotificationFilter implements SwrveNotificationFilter {
        @Override
        public Notification filterNotification(NotificationCompat.Builder builder, int id, SwrveNotificationDetails notificationDetails, String jsonPayload) {
            return builder.build();
        }
    }

    @Test
    public void testInternalKey() throws Exception {
        // If you add an internal root key make sure to add it to PUSH_INTERNAL_KEYS

        ArrayList internalKeys = new ArrayList(SwrveNotificationInternalPayloadConstants.PUSH_INTERNAL_KEYS);
        Field[] fields = SwrveNotificationInternalPayloadConstants.class.getDeclaredFields();

        // Add any internal key to PUSH_INTERNAL_KEYS
        assertEquals(internalKeys.size(), fields.length - 1);

        for (Field f : fields) {
            String fieldName = f.getName();
            if (!fieldName.equals("PUSH_INTERNAL_KEYS")) {
                String fieldValue = (String) f.get(null);
                if (!internalKeys.contains(fieldValue)) {
                    throw new Exception("Please add the field '" + fieldName + "' to SwrveNotificationInternalPayloadConstants.PUSH_INTERNAL_KEYS");
                }
            }
        }
    }

    @Test
    public void testNotificationCustomFilterJsonPayloadCleaned() {
        SwrveNotificationFilter notificationFilterSpy = spy(new MySwrveNotificationFilter());
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel).notificationFilter(notificationFilterSpy);
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);
        
        Bundle bundle = new Bundle();
        String payloadJson = "{}";
        for (String internalKey : SwrveNotificationInternalPayloadConstants.PUSH_INTERNAL_KEYS) {
            if (!internalKey.equals(SwrveNotificationInternalPayloadConstants.SWRVE_AUTH_USER_KEY)) {
                bundle.putString(internalKey, "456");
            }
        }

        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, payloadJson);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "plain text");
        bundle.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, "{ 'customKey': 'customValues' }");
        bundle.putString("customKey", "youShallNotPass");
        bundle.putString("passKey", "passValue");

        SwrvePushManagerImp pushManagerSpy = Mockito.spy(new SwrvePushManagerImp(mActivity));
        doReturn(Mockito.mock(CampaignDeliveryManager.class)).when(pushManagerSpy).getCampaignDeliveryManager();
        pushManagerSpy.processMessage(bundle);

        // verify the custom payload is delivered
        verify(notificationFilterSpy, atLeastOnce())
                .filterNotification(any(NotificationCompat.Builder.class), anyInt(), any(SwrveNotificationDetails.class), eq("{\"customKey\":\"customValues\",\"passKey\":\"passValue\"}"));
    }

    @Test
    public void testSendPushDeliveryDisplayedTrue() {
        Bundle pushBundle = new Bundle();
        pushBundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        pushBundle.putString(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY, "888");
        pushBundle.putString(GENERIC_EVENT_PAYLOAD_MSG_ID, "777");
        pushBundle.putString(GENERIC_EVENT_PAYLOAD_SENT_TIME, "999");
        SwrvePushManagerImp pushManagerSpy = Mockito.spy(new SwrvePushManagerImp(mActivity));
        doReturn(9876l).when(pushManagerSpy).getTime();
        CampaignDeliveryManager campaignDeliveryManagerSpy = Mockito.mock(CampaignDeliveryManager.class);
        doReturn(campaignDeliveryManagerSpy).when(pushManagerSpy).getCampaignDeliveryManager();

        pushManagerSpy.sendPushDeliveredEvent(pushBundle, true, "");

        // @formatter:off
        String expectedJson = "{" +
                    "\"user\":\"testUserId\"," +
                    "\"session_token\":\"some_session_key\"," +
                    "\"version\":\"3\"," +
                    "\"app_version\":\"some_app_version\"," +
                    "\"unique_device_id\":\"some_device_id\"," +
                    "\"data\":[" +
                        "{" +
                            "\"type\":\"generic_campaign_event\"," +
                            "\"time\":9876," +
                            "\"seqnum\":1," +
                            "\"actionType\":\"delivered\"," +
                            "\"campaignType\":\"push\"," +
                            "\"id\":\"123\"," +
                            "\"payload\":{" +
                                "\"displayed\":\"true\"," +
                                "\"silent\":\"false\"," +
                                "\"additional_info\":" +
                                "{" +
                                    "\"provider.message_id\":\"777\"," +
                                    "\"provider.sent_time\":\"999\"," +
                                    "\"_sid\":\"888\"" +
                                "}" +
                            "}" +
                        "}" +
                    "]" +
                "}";
        // @formatter:on

        verify(campaignDeliveryManagerSpy, atLeastOnce()).sendCampaignDelivery("CampaignDeliveryWork_888", "some_endpoint/1/batch", expectedJson);
    }

    @Test
    public void testSendPushDeliveryDisplayedFalse() {
        Bundle pushBundle = new Bundle();
        pushBundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        SwrvePushManagerImp pushManagerSpy = Mockito.spy(new SwrvePushManagerImp(mActivity));
        doReturn(9876l).when(pushManagerSpy).getTime();
        CampaignDeliveryManager campaignDeliveryManagerSpy = Mockito.mock(CampaignDeliveryManager.class);
        doReturn(campaignDeliveryManagerSpy).when(pushManagerSpy).getCampaignDeliveryManager();

        pushManagerSpy.sendPushDeliveredEvent(pushBundle, false, "some_reason");

        // @formatter:off
        String expectedJson = "{" +
                    "\"user\":\"testUserId\"," +
                    "\"session_token\":\"some_session_key\"," +
                    "\"version\":\"3\"," +
                    "\"app_version\":\"some_app_version\"," +
                    "\"unique_device_id\":\"some_device_id\"," +
                    "\"data\":[" +
                        "{" +
                            "\"type\":\"generic_campaign_event\"," +
                            "\"time\":9876," +
                            "\"seqnum\":1," +
                            "\"actionType\":\"delivered\"," +
                            "\"campaignType\":\"push\"," +
                            "\"id\":\"123\"," +
                            "\"payload\":{" +
                                "\"displayed\":\"false\"," +
                                "\"reason\":\"some_reason\"," +
                                "\"silent\":\"false\"" +
                            "}" +
                        "}" +
                    "]" +
                "}";
        // @formatter:on

        verify(campaignDeliveryManagerSpy, atLeastOnce()).sendCampaignDelivery("CampaignDeliveryWork_123","some_endpoint/1/batch", expectedJson);
    }

    @Test
    public void testSendPushDeliverySilent()  {
        Bundle pushBundle = new Bundle();
        pushBundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "456");
        pushBundle.putString(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY, "789");
        SwrvePushManagerImp pushManagerSpy = Mockito.spy(new SwrvePushManagerImp(mActivity));
        doReturn(9876l).when(pushManagerSpy).getTime();
        CampaignDeliveryManager campaignDeliveryManagerSpy = Mockito.mock(CampaignDeliveryManager.class);
        doReturn(campaignDeliveryManagerSpy).when(pushManagerSpy).getCampaignDeliveryManager();

        pushManagerSpy.sendPushDeliveredEvent(pushBundle, false, "");

        // @formatter:off
        String expectedJson = "{" +
                    "\"user\":\"testUserId\"," +
                    "\"session_token\":\"some_session_key\"," +
                    "\"version\":\"3\"," +
                    "\"app_version\":\"some_app_version\"," +
                    "\"unique_device_id\":\"some_device_id\"," +
                    "\"data\":[" +
                        "{" +
                            "\"type\":\"generic_campaign_event\"," +
                            "\"time\":9876," +
                            "\"seqnum\":1," +
                            "\"actionType\":\"delivered\"," +
                            "\"campaignType\":\"push\"," +
                            "\"id\":\"456\"," +
                            "\"payload\":{" +
                                "\"displayed\":\"false\"," +
                                "\"silent\":\"true\"," +
                                "\"additional_info\":" +
                                    "{" +
                                        "\"_sid\":\"789\"" +
                                    "}" +
                            "}" +
                        "}" +
                    "]" +
                "}";
        // @formatter:on

        verify(campaignDeliveryManagerSpy, atLeastOnce()).sendCampaignDelivery("CampaignDeliveryWork_789","some_endpoint/1/batch", expectedJson);
    }

    @Test
    public void testSendPushDeliveryNotSwrvePush() {
        Bundle pushBundle = new Bundle();
        pushBundle.putString("not_a_swrve_push", "456");
        SwrvePushManagerImp pushManagerSpy = Mockito.spy(new SwrvePushManagerImp(mActivity));
        doReturn(9876l).when(pushManagerSpy).getTime();
        CampaignDeliveryManager campaignDeliveryManagerSpy = Mockito.spy(new CampaignDeliveryManager(mActivity));
        doReturn(campaignDeliveryManagerSpy).when(pushManagerSpy).getCampaignDeliveryManager();

        pushManagerSpy.sendPushDeliveredEvent(pushBundle, false, "some_reason");

        verify(campaignDeliveryManagerSpy, never()).sendCampaignDelivery(anyString(), anyString(), anyString());
    }

    @Test
    public void testSilentAuthPush_TargetUserNotCurrentOne() {

        SwrvePushManagerImp pushManagerSpy = Mockito.spy(new SwrvePushManagerImp(mActivity));

        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value1\"}");
        bundle.putString(SwrveNotificationConstants.SWRVE_AUTH_USER_KEY, "SomeOtherUserId");
        doReturn(SwrveTestUtils.parseDate("2017/01/01 0:00")).when(pushManagerSpy).getNow();
        doReturn(Mockito.mock(CampaignDeliveryManager.class)).when(pushManagerSpy).getCampaignDeliveryManager();
        pushManagerSpy.processMessage(bundle);
        assertNumberOfNotification(0);

        SharedPreferences sharedPreferences = mActivity.getSharedPreferences(SwrveCampaignInfluence.INFLUENCED_PREFS, Context.MODE_PRIVATE);
        List<SwrveCampaignInfluence.InfluenceData> influenceData = new SwrveCampaignInfluence().getSavedInfluencedData(sharedPreferences);
        assertTrue(influenceData.size() == 0);
    }

    @Test
    public void testSilentAuthPush_TargetedUser() {

        SwrvePushManagerImp pushManagerSpy = Mockito.spy(new SwrvePushManagerImp(mActivity));
        doReturn(Mockito.mock(CampaignDeliveryManager.class)).when(pushManagerSpy).getCampaignDeliveryManager();

        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value\"}");
        bundle.putString(SwrveNotificationConstants.SWRVE_AUTH_USER_KEY, "testUserId");
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
        bundle.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, "{\"custom\":\"values\"}");
        bundle.putString("sound", "default");
        doReturn(SwrveTestUtils.parseDate("2017/01/01 0:00")).when(pushManagerSpy).getNow();
        pushManagerSpy.processMessage(bundle);
        assertNumberOfNotification(1);

        SharedPreferences sharedPreferences = mActivity.getSharedPreferences(SwrveCampaignInfluence.INFLUENCED_PREFS, Context.MODE_PRIVATE);
        List<SwrveCampaignInfluence.InfluenceData> influenceData = new SwrveCampaignInfluence().getSavedInfluencedData(sharedPreferences);
        assertTrue(influenceData.size() == 1);
    }

    private void createSwrveSpy() throws Exception {
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(dummyIconResource, dummyIconResource, dummyChannel);
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig.build());
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveCommon.setSwrveCommon(swrveSpy);
    }

    private void sendMessage(boolean authenticated, boolean grantNotificationPermission) {

        Bundle bundle = new Bundle();
        if (authenticated) {
            bundle.putString(SwrveNotificationConstants.SWRVE_AUTH_USER_KEY, SwrveSDK.getUserId());
        }
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        String json = "{\n" +
                "\"notification_id\": 123,\n" +
                "\"title\": \"title\",\n" +
                "\"subtitle\": \"subtitle\",\n" +
                "\"version\": 1,\n" +
                "\"expanded\": {" +
                "\"title\": \"expanded title\",\n" +
                "\"body\": \"expanded body\"}" +
                "}";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "plain text");
        bundle.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, "{ 'customKey': 'customValues' }");
        bundle.putString("sound", "default");

        pushManagerSpy = Mockito.spy(new SwrvePushManagerImp(mActivity));
        doReturn(Mockito.mock(CampaignDeliveryManager.class)).when(pushManagerSpy).getCampaignDeliveryManager();
        if (grantNotificationPermission == false) {
            doReturn(Build.VERSION_CODES.TIRAMISU).when(pushManagerSpy).getOSBuildVersion();
            shadowApplication.denyPermissions(Manifest.permission.POST_NOTIFICATIONS);
        }
        pushManagerSpy.processMessage(bundle);
    }

    private void assertSendPushDeliveredEvent(boolean displayed, String reason) {
        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        ArgumentCaptor<Boolean> displayedCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(pushManagerSpy, times(1)).sendPushDeliveredEvent(bundleCaptor.capture(), displayedCaptor.capture(), reasonCaptor.capture());
        assertEquals(displayed, displayedCaptor.getValue());
        assertEquals(reason, reasonCaptor.getValue());
    }
}
