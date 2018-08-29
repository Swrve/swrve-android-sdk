package com.swrve.sdk;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.notifications.model.SwrveNotification;
import com.swrve.sdk.notifications.model.SwrveNotificationButton;
import com.swrve.sdk.push.SwrvePushServiceDefault;
import com.swrve.sdk.qa.SwrveQAUser;
import com.swrve.sdk.rest.RESTClient;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowPendingIntent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_INFLUENCED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class SwrvePushSDKTest extends SwrveBaseTest {

    private SwrvePushServiceDefault serviceDefault;
    private SwrvePushSDK swrvePushSDKSpy;
    private final int DEFAULT_PUSH_ID_CACHE_SIZE = 16;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        SwrvePushSDK swrvePushSDK = new SwrvePushSDK(RuntimeEnvironment.application);
        swrvePushSDKSpy = spy(swrvePushSDK);
        SwrvePushSDK.instance = swrvePushSDKSpy;
        serviceDefault = new SwrvePushServiceDefault();
    }

    private int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }

    @Test
    public void testService() throws Exception {
        //Check null scenario
        swrvePushSDKSpy.processRemoteNotification(null, true);
        verify(swrvePushSDKSpy, never()).processNotification(any(Bundle.class));

        //Check no payload scenario
        Bundle missingTrackingKey = new Bundle();
        missingTrackingKey.putString(SwrveNotificationConstants.TEXT_KEY, "");
        swrvePushSDKSpy.processRemoteNotification(missingTrackingKey, true);
        verify(swrvePushSDKSpy, never()).processNotification(any(Bundle.class));

        //Check no timestamp scenario
        Bundle noTimestamp = new Bundle();
        noTimestamp.putString(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        noTimestamp.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        swrvePushSDKSpy.processRemoteNotification(noTimestamp, true);
        verify(swrvePushSDKSpy, never()).processNotification(any(Bundle.class));
        assertNumberOfNotifications(0);

        //Check good scenario
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        extras.putString("customData", "some custom values");
        extras.putString("sound", "default");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        int firstTimestamp = generateTimestampId();
        extras.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        swrvePushSDKSpy.processRemoteNotification(extras, true);
        assertNotification("validBundle", "content://settings/system/notification_sound", extras);
        assertNumberOfNotifications(1);

        //Try sending duplicate
        swrvePushSDKSpy.processRemoteNotification(extras, true);
        assertNumberOfNotifications(1);

        //Now send another 16 unique notifications to overfill the buffer
        int newTimestamp = generateTimestampId();
        for (int i=0; i<DEFAULT_PUSH_ID_CACHE_SIZE; ++i) {
            extras.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(newTimestamp));
            swrvePushSDKSpy.processRemoteNotification(extras, true);
            newTimestamp++;

            //Let the notification manager do its thing
            Thread.sleep(100);
        }

        //Assert there are 17 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 1);

        //Now we should be able to reuse the first notification timestamp
        extras.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        swrvePushSDKSpy.processRemoteNotification(extras, true);
        //Let the notification manager do its thing
        Thread.sleep(100);

        //Assert there are 18 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 2);

        //Now make use of the ability to configure the duplication cache size
        extras.putInt(SwrveNotificationConstants.PUSH_ID_CACHE_SIZE_KEY, 1);
        swrvePushSDKSpy.processRemoteNotification(extras, true);
        //Let the notification manager do its thing
        Thread.sleep(100);

        //Assert there are 18 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 2);

        //Send again to make sure dedupe is happening with newest item.
        swrvePushSDKSpy.processRemoteNotification(extras, true);
        //Let the notification manager do its thing
        Thread.sleep(100);

        //Assert there are 18 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 2);

        //Change the key to new timestamp
        extras.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(newTimestamp));
        swrvePushSDKSpy.processRemoteNotification(extras, true);
        //Let the notification manager do its thing
        Thread.sleep(100);

        //Assert there are 19 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 3);

        //Use the previous timestamp - but now expect an extra notification since the cache is only 1.
        extras.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        swrvePushSDKSpy.processRemoteNotification(extras, true);
        //Let the notification manager do its thing
        Thread.sleep(100);

        //Assert there are 20 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 4);
    }

    @Test
    public void testServiceCustomSound() throws Exception {
        Bundle validBundleCustomSound = new Bundle();
        validBundleCustomSound.putString(SwrveNotificationConstants.TEXT_KEY, "validBundleCustomSound");
        validBundleCustomSound.putString("sound", "customSound");
        validBundleCustomSound.putString("customData", "some custom values");
        validBundleCustomSound.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        validBundleCustomSound.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        serviceDefault.processNotification(validBundleCustomSound);
        assertNotification("validBundleCustomSound", "android.resource://com.swrve.sdk.test/raw/customSound", validBundleCustomSound);
    }

    @Test
    public void testServiceWithQaUser() throws Exception {
        JSONObject jsonObject = new JSONObject("{\"campaigns\":[],\"logging\":true,\"reset_device_state\":true,\"logging_url\":\"http:\\/\\/1031.qa-log.swrve.com\"}");
        SwrveQAUser swrveQAUser = new SwrveQAUser(1, "apiKey", "userId", new RESTClient(5000), jsonObject);
        swrveQAUser.bindToServices();

        Intent intent = new Intent();
        Bundle validBundle = new Bundle();
        validBundle.putString(SwrveNotificationConstants.TEXT_KEY, "hello there");
        validBundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        validBundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        serviceDefault.processNotification(validBundle);
        assertNotification("hello there", null, validBundle);
    }

    @Test
    public void testWithDeeplink() {
        Bundle deeplinkBundle = new Bundle();
        deeplinkBundle.putString(SwrveNotificationConstants.TEXT_KEY, "deeplinkBundle");
        deeplinkBundle.putString(SwrveNotificationConstants.DEEPLINK_KEY, "swrve://deeplink/config");
        deeplinkBundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        deeplinkBundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        serviceDefault.processNotification(deeplinkBundle);
        assertNotification("deeplinkBundle", null, deeplinkBundle);
    }

    @Test
    public void testSilentPush() throws Exception {
        SwrveSilentPushListener silentPushListenerMock = Mockito.mock(SwrveSilentPushListener.class);
        swrvePushSDKSpy.setSilentPushListener(silentPushListenerMock);

        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        Swrve swrveSpy = spy(swrveReal);

        // Send some valid silent pushes
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value1\"}");
        when(swrvePushSDKSpy.getNow()).thenReturn(SwrveTestUtils.parseDate("2017/01/01 0:00"));
        serviceDefault.processNotification(bundle);
        ArgumentCaptor<JSONObject> payloadCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(silentPushListenerMock, atLeastOnce()).onSilentPush(any(Context.class), payloadCaptor.capture());
        assertEquals("{\"custom\":\"value1\"}", payloadCaptor.getValue().toString());

        // Silent push 2
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "2");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value2\"}");
        when(swrvePushSDKSpy.getNow()).thenReturn(SwrveTestUtils.parseDate("2017/01/01 10:00"));
        serviceDefault.processNotification(bundle);

        // Silent push 3
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "3");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value3\"}");
        when(swrvePushSDKSpy.getNow()).thenReturn(SwrveTestUtils.parseDate("2017/01/01 11:00"));
        serviceDefault.processNotification(bundle);

        assertNumberOfNotifications(0);

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

    @Test
    public void testAdvancedBigTextPush() {
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\"}}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "text");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        serviceDefault.processNotification(bundle);

        assertNotification("text", "content://settings/system/notification_sound", bundle);
        verify(swrvePushSDKSpy, times(1)).processNotification(bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("text", notification.tickerText);
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());
        assertEquals("[expanded body]", shadowNotification.getBigText());
        // Robolectric does not give the ability to check the subtitle yet
        // assertEquals("subtitle", shadowNotification.getSubText());
    }

    @Test
    public void testAlternateAccentColorPush() throws Exception {
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"accent\": \"#00FF0000\",\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\"}}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "text");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        serviceDefault.processNotification(bundle);

        assertNotification("text", "content://settings/system/notification_sound", bundle);
        verify(swrvePushSDKSpy, times(1)).processNotification(bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(Color.RED , notification.color);
    }

    @Test
    public void testAdvancedUpdateExisting() throws Exception {
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"notification_id\": 12,\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\"}}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        serviceDefault.processNotification(bundle);

        String updateJson = "{\n" +
                " \"title\": \"update title\",\n" +
                " \"subtitle\": \"update subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"notification_id\": 12,\n" +
                "\"expanded\": { \"title\": \"[expanded updated title]\",\n" +
                "                \"body\": \"[expanded update body]\"}}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, updateJson);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "update body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int secondTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(secondTimestamp));

        serviceDefault.processNotification(bundle);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);

        assertEquals("update body", notification.tickerText);
        assertEquals("[expanded updated title]", shadowNotification.getBigContentTitle());
        assertEquals("[expanded update body]", shadowNotification.getBigText());

        // Robolectric does not give the ability to check the subtitle yet
        //assertEquals("update subtitle", shadowNotification.getSubText());
    }

    @Test
    public void testAdvancedUnknownVersion() throws Exception {
        /*
         * Check if the SDK will revert to original notification if the version is wrong
         **/
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"fallback title\",\n" +
                " \"subtitle\": \"fallback subtitle\",\n" +
                " \"icon_url\": \"https://valid-image.png\",\n" +
                " \"version\": 99,\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n"+
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://media.jpg\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"https://valid-image.png\",\n" +
                "              \"fallback_sd\": \"https://video.com\"\n }," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://valid-image.png\"} \n"+
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "original push notification");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, null));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        when(builderSpy.downloadBitmapImageFromUrl(anyString())).thenReturn(bmp);
        when(swrvePushSDKSpy.getSwrveNotificationBuilder()).thenReturn(builderSpy);

        serviceDefault.processNotification(bundle);

        assertNotification("original push notification", "content://settings/system/notification_sound", bundle);
        verify(swrvePushSDKSpy, times(1)).processNotification(bundle);
        assertNumberOfNotifications(1);
    }

    @Test
    public void testAdvancedUnknownType() throws Exception {
        /*
         * This test is to check if the SDK can handle new types of media before it's added
         * This should default back to BigText and parse what it can.
         */
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"fallback title\",\n" +
                " \"subtitle\": \"fallback subtitle\",\n" +
                " \"icon_url\": \"https://valid-image.png\",\n" +
                " \"version\": 1,\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n"+
                "              \"type\": \"unknown\",\n" +
                "              \"url\": \"https://media.jpg\"},\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://valid-image.png\"} \n"+
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "original push notification");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, null));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        when(builderSpy.downloadBitmapImageFromUrl(anyString())).thenReturn(bmp);
        when(swrvePushSDKSpy.getSwrveNotificationBuilder()).thenReturn(builderSpy);

        serviceDefault.processNotification(bundle);

        assertNotification("original push notification", "content://settings/system/notification_sound", bundle);
        verify(swrvePushSDKSpy, times(1)).processNotification(bundle);
        assertNumberOfNotifications(1);
    }

    @Test
    public void testAdvancedPushLockScreenMessage() throws Exception {
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"visibility\": \"private\",\n" +
                " \"lock_screen_msg\": \"lock screen test message\",\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\"}}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "original push notification");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        serviceDefault.processNotification(bundle);

        assertNotification("original push notification", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(Notification.VISIBILITY_PRIVATE ,notification.visibility);
        assertEquals("lock screen test message", notification.publicVersion.tickerText);
    }

    @Test
    public void testAdvancedPushLockScreenMessageNoMedia() throws Exception {
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"visibility\": \"private\",\n" +
                " \"lock_screen_msg\": \"lock screen test message\",\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\"}}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "original push notification");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        serviceDefault.processNotification(bundle);

        assertNotification("original push notification", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(Notification.VISIBILITY_PRIVATE ,notification.visibility);
        assertEquals("lock screen test message", notification.publicVersion.tickerText);
    }

    @Test
    public void testAdvancedBigImagePush() {

        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"fallback title\",\n" +
                " \"subtitle\": \"fallback subtitle\",\n" +
                " \"icon_url\": \"https://valid-image.png\",\n" +
                " \"version\": 1,\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n"+
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://media.jpg\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"https://valid-image.png\",\n" +
                "              \"fallback_sd\": \"https://video.com\"\n }," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://valid-image.png\"} \n"+
                "}\n";

        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, null));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        when(builderSpy.downloadBitmapImageFromUrl("https://valid-image.png")).thenReturn(bmp);
        when(swrvePushSDKSpy.getSwrveNotificationBuilder()).thenReturn(builderSpy);

        serviceDefault.processNotification(bundle);

        assertNotification("[rich body]", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("[rich body]",  notification.tickerText);
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());
        // Robolectric does not give the ability to check the subtitle yet
        // assertEquals("[rich subtitle]", shadowNotification.getSubText());
        assertNotNull(shadowNotification.getBigPicture());
        assertEquals(bmp, shadowNotification.getBigPicture());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Config(sdk = Build.VERSION_CODES.M)
    @Test
    public void testAdvancedBigImagePushVideoFallback() {

        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"fallback title\",\n" +
                " \"subtitle\": \"fallback subtitle\",\n" +
                " \"icon_url\": \"https://valid-image.png\",\n" +
                " \"version\": 1,\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n"+
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://media.jpg\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"https://valid-image.png\",\n" +
                "              \"fallback_sd\": \"https://fallback_sd\"\n }," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\":  \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://valid-image.png\"} \n"+
                "}\n";

        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, null));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        when(builderSpy.downloadBitmapImageFromUrl(anyString())).thenReturn(null);
        when(builderSpy.downloadBitmapImageFromUrl("https://fail-image.png")).thenReturn(null);
        when(builderSpy.downloadBitmapImageFromUrl("https://valid-image.png")).thenReturn(bmp);
        when(swrvePushSDKSpy.getSwrveNotificationBuilder()).thenReturn(builderSpy);

        serviceDefault.processNotification(bundle);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);

        assertEquals("[rich body]", notification.tickerText);
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());
        // Robolectric does not give the ability to check the subtitle yet
        //assertEquals("[rich subtitle]", shadowNotification.getSubText());
        assertNotNull(shadowNotification.getBigPicture());

        PendingIntent pendingIntent = notification.contentIntent;
        ShadowPendingIntent shadowPendingIntent = shadowOf(pendingIntent);
        assertNotNull(shadowPendingIntent);
        assertTrue(shadowPendingIntent.isBroadcastIntent());
        assertEquals(1, shadowPendingIntent.getSavedIntents().length);
        Intent shadowIntent = shadowPendingIntent.getSavedIntents()[0];
        assertEquals("com.swrve.sdk.SwrveNotificationEngageReceiver", shadowIntent.getComponent().getClassName());
        Bundle intentExtras = shadowIntent.getBundleExtra(SwrveNotificationConstants.PUSH_BUNDLE);
        assertEquals("https://fallback_sd", intentExtras.get(SwrveNotificationConstants.DEEPLINK_KEY));

        // Although they are the same, equals returns false, must fix
        // Icon icon = Icon.createWithBitmap(bmp);
        // assertEquals(icon, notification.getLargeIcon());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Config(sdk = Build.VERSION_CODES.N)
    @Test
    public void testAdvancedBigImagePushBigTextFallback() throws Exception {
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"fallback title\",\n" +
                " \"subtitle\": \"fallback subtitle\",\n" +
                " \"icon_url\": \"https://valid-image.png\",\n" +
                " \"version\": 1,\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n"+
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://fail-image.png\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"https://fail-image.png\",\n" +
                "              \"fallback_sd\": \"https://fallback_sd\"\n }," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\":  \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://valid-image.png\"} \n"+
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "fallback body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, null));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        when(builderSpy.downloadBitmapImageFromUrl(anyString())).thenReturn(null);
        when(builderSpy.downloadBitmapImageFromUrl("https://fail-image.png")).thenReturn(null);
        when(builderSpy.downloadBitmapImageFromUrl("https://valid-image.png")).thenReturn(bmp);
        when(swrvePushSDKSpy.getSwrveNotificationBuilder()).thenReturn(builderSpy);

        serviceDefault.processNotification(bundle);

        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("fallback body", shadowNotification.getContentText());
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());
        // Robolectric does not give the ability to check the subtitle yet
        //assertEquals("[rich subtitle]", shadowNotification.getSubTitle());

        // Big text should be set instead
        assertEquals("[expanded body]", shadowNotification.getBigText());
        assertNull(shadowNotification.getBigPicture());

        PendingIntent pendingIntent = notification.contentIntent;
        ShadowPendingIntent shadowPendingIntent = shadowOf(pendingIntent);
        assertNotNull(shadowPendingIntent);
        assertTrue(shadowPendingIntent.isBroadcastIntent());
        assertEquals(1, shadowPendingIntent.getSavedIntents().length);
        Intent shadowIntent = shadowPendingIntent.getSavedIntents()[0];
        assertEquals("com.swrve.sdk.SwrveNotificationEngageReceiver", shadowIntent.getComponent().getClassName());
        Bundle intentExtras = shadowIntent.getBundleExtra(SwrveNotificationConstants.PUSH_BUNDLE);
        assertNull(intentExtras.get(SwrveNotificationConstants.DEEPLINK_KEY)); // this should not be set
    }

    @Test
    public void testBadImageAndNoFallback() throws Exception {
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"fallback title\",\n" +
                " \"subtitle\": \"fallback subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n"+
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://fail-image.png\" }" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "fallback body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, null));
        when(builderSpy.downloadBitmapImageFromUrl(anyString())).thenReturn(null);
        when(builderSpy.downloadBitmapImageFromUrl("https://fail-image.png")).thenReturn(null);
        when(swrvePushSDKSpy.getSwrveNotificationBuilder()).thenReturn(builderSpy);

        serviceDefault.processNotification(bundle);

        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals("fallback body", notification.tickerText);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("fallback title", shadowNotification.getBigContentTitle());
        // Robolectric does not give the ability to check the subtitle yet
        // assertEquals("[rich subtitle]", shadowNotification.getSubText());

        assertNull(shadowNotification.getBigPicture());
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testAdvancedBigTextPushWith1Action() throws Exception {
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"notification_id\": 221,\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\"},\n" +
                "\"buttons\": [{\"title\": \"[button text 1]\",\n" +
                "                 \"action_type\": \"open_url\",\n" +
                "                 \"action\": \"https://lovelyURL\"\n }]\n" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "text");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        serviceDefault.processNotification(bundle);

        assertNotification(SwrveNotificationConstants.TEXT_KEY, "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals(SwrveNotificationConstants.TEXT_KEY, notification.tickerText);
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());
        assertEquals("[expanded body]", shadowNotification.getBigText());// Robolectric does not give the ability to check the subtitle yet
        // assertEquals("[rich subtitle]", shadowNotification.getSubText());

        Notification.Action[] actions = notification.actions;
        assertEquals(actions.length, 1);
        Notification.Action testAction = actions[0];
        assertEquals(testAction.title, "[button text 1]");
        Intent testIntent = getIntent(testAction.actionIntent);
        Bundle extras = testIntent.getExtras();
        assertEquals("0", extras.getString(SwrveNotificationConstants.CONTEXT_ID_KEY));
        assertEquals(SwrveNotificationButton.ActionType.OPEN_URL, extras.get(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY));
        assertEquals("https://lovelyURL", extras.getString(SwrveNotificationConstants.PUSH_ACTION_URL_KEY));
        assertEquals(221, extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testAdvancedBigImagePushWith2Actions() throws Exception {
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"icon_url\": \"https://valid-image.png\",\n" +
                " \"version\": 1,\n" +
                " \"notification_id\": 222,\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n"+
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://valid-image.png\"}," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://valid-image.png\"}, \n" +
                "\"buttons\": [{\"title\": \"[button text 1]\",\n" +
                "                 \"action_type\": \"open_url\",\n" +
                "                 \"action\": \"https://lovelyURL\"},\n" +
                "                {\"title\": \"[button text 2]\",\n" +
                "                 \"action_type\": \"open_app\"}]\n" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, null));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        when(builderSpy.downloadBitmapImageFromUrl(anyString())).thenReturn(null);
        when(builderSpy.downloadBitmapImageFromUrl("https://valid-image.png")).thenReturn(bmp);
        when(swrvePushSDKSpy.getSwrveNotificationBuilder()).thenReturn(builderSpy);

        serviceDefault.processNotification(bundle);

        assertNotification("[rich body]", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("[rich body]", notification.tickerText);
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());
        // Robolectric does not give the ability to check the subtitle yet
        // assertEquals("[rich subtitle]", shadowNotification.getSubText());
        assertNotNull(shadowNotification.getBigPicture());
        assertEquals(bmp, shadowNotification.getBigPicture());

        Notification.Action[] actions = notification.actions;
        assertEquals(actions.length, 2);
        Notification.Action testAction = actions[0];
        assertEquals(testAction.title, "[button text 1]");
        Intent testIntent = getIntent(testAction.actionIntent);
        Bundle extras = testIntent.getExtras();
        assertEquals("0", extras.getString(SwrveNotificationConstants.CONTEXT_ID_KEY));
        assertEquals(SwrveNotificationButton.ActionType.OPEN_URL, extras.get(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY));
        assertEquals("https://lovelyURL", extras.getString(SwrveNotificationConstants.PUSH_ACTION_URL_KEY));
        assertEquals(222, extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID));

        testAction = actions[1];
        assertEquals(testAction.title, "[button text 2]");
        testIntent = getIntent(testAction.actionIntent);
        extras = testIntent.getExtras();
        assertEquals("1", extras.getString(SwrveNotificationConstants.CONTEXT_ID_KEY));
        assertEquals(SwrveNotificationButton.ActionType.OPEN_APP, extras.get(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY));
        assertNull(extras.get(SwrveNotificationConstants.PUSH_ACTION_URL_KEY));
        assertEquals(222, extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testAdvancedBigTextPushWith3Actions() throws Exception {
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"notification_id\": 223,\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\"}, \n" +
                "\"buttons\": [{\"title\": \"[button text 1]\",\n" +
                "                 \"action_type\": \"open_url\",\n" +
                "                 \"action\": \"https://lovelyURL\"},\n" +
                "                {\"title\": \"[button text 2]\",\n" +
                "                 \"action_type\": \"open_app\"},\n" +
                "                {\"title\": \"[button text 3]\",\n" +
                "                 \"action_type\": \"dismiss\"}]\n" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "text");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        serviceDefault.processNotification(bundle);

        assertNotification("text", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("text", notification.tickerText);
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());
        assertEquals("[expanded body]", shadowNotification.getBigText());
        // Robolectric does not give the ability to check the subtitle yet
        // assertEquals("[rich subtitle]", shadowNotification.getSubText());

        Notification.Action[] actions = notification.actions;
        assertEquals(actions.length, 3);
        Notification.Action testAction = actions[0];
        assertEquals(testAction.title, "[button text 1]");
        Intent testIntent = getIntent(testAction.actionIntent);
        Bundle extras = testIntent.getExtras();
        assertEquals("0",extras.getString(SwrveNotificationConstants.CONTEXT_ID_KEY));
        assertEquals(SwrveNotificationButton.ActionType.OPEN_URL, extras.get(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY));
        assertEquals("https://lovelyURL", extras.getString(SwrveNotificationConstants.PUSH_ACTION_URL_KEY));
        assertEquals(223, extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID));

        testAction = actions[1];
        assertEquals(testAction.title, "[button text 2]");
        testIntent = getIntent(testAction.actionIntent);
        extras = testIntent.getExtras();
        assertEquals("1",extras.getString(SwrveNotificationConstants.CONTEXT_ID_KEY));
        assertEquals(SwrveNotificationButton.ActionType.OPEN_APP, extras.get(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY));
        assertNull(extras.get(SwrveNotificationConstants.PUSH_ACTION_URL_KEY));
        assertEquals(223, extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID));

        testAction = actions[2];
        assertEquals(testAction.title, "[button text 3]");
        testIntent = getIntent(testAction.actionIntent);
        extras = testIntent.getExtras();
        assertEquals("2",extras.getString(SwrveNotificationConstants.CONTEXT_ID_KEY));
        assertEquals(SwrveNotificationButton.ActionType.DISMISS, extras.get(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY));
        assertNull(extras.get(SwrveNotificationConstants.PUSH_ACTION_URL_KEY));
        assertEquals(223, extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testButtonClickEvents() throws Exception {
        SwrveConfig config = new SwrveConfig();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        swrveReal.init(mActivity);

        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                "\"buttons\": [{\"title\": \"[button text 1]\",\n" +
                "                 \"action_type\": \"open_url\",\n" +
                "                 \"action\": \"https://lovelyURL\"},\n" +
                "                {\"title\": \"[button text 2]\",\n" +
                "                 \"action_type\": \"open_app\"},\n" +
                "                {\"title\": \"[button text 3]\",\n" +
                "                 \"action_type\": \"dismiss\"}]\n" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "text");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        serviceDefault.processNotification(bundle);

        Notification notification = assertNotification("text", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        Notification.Action[] actions = notification.actions;
        assertEquals(actions.length, 3);
        Notification.Action testAction = actions[2];
        assertEquals(testAction.title, "[button text 3]");
        Intent buttonClickIntent = getIntent(testAction.actionIntent);
        SwrveNotificationEngageReceiver receiver = new SwrveNotificationEngageReceiver();
        receiver.onReceive(mActivity, buttonClickIntent);

        List<Intent> broadcastIntents = mShadowActivity.getBroadcastIntents();
        assertEquals(2, broadcastIntents.size());

        // Should send a button click event and an engagement event
        List<String> events = SwrveTestUtils.getEventsQueued(mShadowActivity);
        assertEquals(2, events.size());
        for(String event : events) { // can't guarantee which event is in list first so just iterate through them
            if(event.contains("generic_campaign_event")) {
                SwrveTestUtils.assertGenericEvent(event, "2", GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK, null);
            } else {
                assertEngagedEvent(event, "Swrve.Messages.Push-1.engaged");
            }
        }

        // Should not send an influence event when the app is opened (emulate app start/resume)
        swrveReal.onResume(mActivity);
        // No new events should be queued
        events = SwrveTestUtils.getEventsQueued(mShadowActivity);
        assertEquals(2, events.size());
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testEngagedEvents() throws Exception {
        SwrveConfig config = new SwrveConfig();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        swrveReal.init(mActivity);

        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        serviceDefault.processNotification(bundle);

        Notification notification = assertNotification("body", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        notification.contentIntent.send();

        // Launch SwrveNotificationEngageReceiver (imitate single engagement with notification)
        List<Intent> broadcastIntents = mShadowActivity.getBroadcastIntents();
        assertEquals(1, broadcastIntents.size());
        Intent engageEventIntent = broadcastIntents.get(0);
        SwrveNotificationEngageReceiver engageReceiver = new SwrveNotificationEngageReceiver();
        // Clear pending intents
        mShadowActivity.getBroadcastIntents().clear();
        engageReceiver.onReceive(mActivity, engageEventIntent);

        broadcastIntents = mShadowActivity.getBroadcastIntents();
        assertEquals(2, broadcastIntents.size());
        assertEquals("android.intent.action.CLOSE_SYSTEM_DIALOGS", broadcastIntents.get(1).getAction());

        // Should send a button click event and an engagement event
        List<String> events = SwrveTestUtils.getEventsQueued(mShadowActivity);
        assertEquals(1, events.size());
        assertEngagedEvent(events.get(0), "Swrve.Messages.Push-1.engaged");

        // Should not send an influence event when the app is opened (emulate app start/resume)
        swrveReal.onResume(mActivity);
        // No new events should be queued
        events = SwrveTestUtils.getEventsQueued(mShadowActivity);
        assertEquals(1, events.size());
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Test
    public void testNotificationChannelFromConfig() throws Exception {
        NotificationChannel channel = new NotificationChannel("swrve_channel", "Swrve channel", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("All the news from Swrve");
        ISwrveCommon mockSwrveCommon = Mockito.mock(ISwrveCommon.class);
        when(mockSwrveCommon.getDefaultNotificationChannel()).thenReturn(channel);
        SwrveCommon.setSwrveCommon(mockSwrveCommon);

        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1\n" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);

        serviceDefault.processNotification(bundle);

        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);

        assertEquals("swrve_channel", notification.getChannelId());
        // Check that the channel was created by our SDK
        assertNotNull(shadowOf(notificationManager).getNotificationChannel("swrve_channel"));
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Test
    public void testNotificationChannelIdFromPayload() throws Exception {

        String channelId = "my_channel_id";
        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(new NotificationChannel(channelId, "some channel", NotificationManager.IMPORTANCE_DEFAULT));

        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"channel_id\": \"" + channelId + "\",\n" +
                " \"version\": 1\n" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);

        serviceDefault.processNotification(bundle);

        assertNumberOfNotifications(1);

        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);

        assertEquals(channelId, notification.getChannelId());
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Test
    public void testNotificationChannelFromPayload() throws Exception {

        ISwrveCommon mockSwrveCommon = Mockito.mock(ISwrveCommon.class);
        SwrveCommon.setSwrveCommon(mockSwrveCommon);

        String channelId = "my_channel_id";
        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"channel_id\": \"not_found\",\n" +
                " \"channel\": { \"id\": \"" + channelId + "\",\n" +
                " \"name\": \"channel_name\",\n" +
                " \"importance\": \"high\" },\n" +
                " \"version\": 1\n" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);

        serviceDefault.processNotification(bundle);

        assertNumberOfNotifications(1);

        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);

        assertEquals(channelId, notification.getChannelId());

        // Check the created notification channel from the payload
        NotificationChannel newChannel = notificationManager.getNotificationChannel(channelId);
        assertEquals("channel_name", newChannel.getName());
        assertEquals(NotificationManager.IMPORTANCE_HIGH, newChannel.getImportance());
    }

    private Notification assertNotification(String tickerText, String sound, Bundle extras)  {
        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(tickerText, notification.tickerText);
        assertEquals(sound, notification.sound == null ? null : notification.sound.toString());
        PendingIntent pendingIntent = notification.contentIntent;
        ShadowPendingIntent shadowPendingIntent = shadowOf(pendingIntent);
        assertNotNull(shadowPendingIntent);
        assertTrue(shadowPendingIntent.isBroadcastIntent());
        assertEquals(1, shadowPendingIntent.getSavedIntents().length);
        Intent intent = shadowPendingIntent.getSavedIntents()[0];
        assertEquals("com.swrve.sdk.SwrveNotificationEngageReceiver", intent.getComponent().getClassName());
        Bundle intentExtras = intent.getBundleExtra(SwrveNotificationConstants.PUSH_BUNDLE);
        assertNotNull(intentExtras);
        for (String key : extras.keySet()) {
            assertTrue(intentExtras.containsKey(key));
            assertEquals(extras.get(key), intentExtras.getString(key));
        }
        ShadowNotification shadowNotification = shadowOf(notification);
        // In the case of a rich push, we should expect the title to be set to something else
        if (!intentExtras.containsKey(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY)) {
            assertEquals("Android Test App", shadowNotification.getContentTitle());
        } else {
            SwrveNotification extrasPPayload = SwrveNotification.fromJson(extras.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY));
            if(extrasPPayload != null) {
                if(extrasPPayload.getVersion() == SwrveNotificationConstants.SWRVE_PUSH_VERSION) {
                    if(extrasPPayload.getMedia() != null && extrasPPayload.getMedia().getType() != null){
                        assertEquals(extrasPPayload.getMedia().getTitle(), shadowNotification.getContentTitle());
                        // Robolectric still has to be give the ability to check for subText and body separately
                        //assertEquals(extrasPPayload.getMedia().getSubtitle(), shadowNotification.getSubText());
                        assertEquals(extrasPPayload.getMedia().getBody(), notification.tickerText);
                    }
                }
            }
        }

        return notification;
    }

    private void assertNumberOfNotifications(int expectedNumberOfNotifications)  {
        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(expectedNumberOfNotifications, notifications.size());
    }

    private Intent getIntent(PendingIntent pendingIntent) {
        return ((ShadowPendingIntent) Shadow.extract(pendingIntent)).getSavedIntent();
    }

    public void assertEngagedEvent(String eventJson, String eventName) {
        Gson gson = new Gson(); // eg: {"type":"event","time":1466519995192,"name":"Swrve.Messages.Push-1.engaged"}
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, String> event = gson.fromJson(eventJson, type);
        assertEquals(4, event.size());
        assertTrue(event.containsKey("type"));
        assertEquals("event", event.get("type"));
        assertTrue(event.containsKey("name"));
        assertEquals(eventName, event.get("name"));
        assertTrue(event.containsKey("time"));
        assertTrue(event.containsKey("seqnum"));
    }
}
