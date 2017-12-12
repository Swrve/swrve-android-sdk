package com.swrve.sdk;

import android.app.IntentService;
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
import android.support.v4.app.NotificationCompat;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.model.PushPayload;
import com.swrve.sdk.model.PushPayloadButton;
import com.swrve.sdk.qa.SwrveQAUser;
import com.swrve.sdk.rest.RESTClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowPendingIntent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class SwrvePushSDKTest extends SwrveBaseTest {

    private GenericSwrvePushService service;
    private TestableSwrvePushSDK swrvePushSDK;
    private final int DEFAULT_PUSH_ID_CACHE_SIZE = 16;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrvePushSDK = new TestableSwrvePushSDK(RuntimeEnvironment.application);
        setSwrvePushSDKInstance(swrvePushSDK);
        service = new GenericSwrvePushService("TEST");
        service.onCreate();
    }

    // TODO is this needed, or can we use the teardown in the super class
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
        SwrveTestUtils.removeSingleton(SwrvePushNotificationConfig.class, "instance");
    }

    private int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }

    @Test
    public void testService() throws Exception {
        service.checkDupes = true;

        //Check null scenario
        service.onHandleIntent(null);
        assertEquals(0, swrvePushSDK.isSwrveRemoteNotificationExecuted);

        //Check no payload scenario
        Intent intent = new Intent();
        Bundle missingTrackingKey = new Bundle();
        missingTrackingKey.putString("text", "");
        intent.putExtras(missingTrackingKey);
        service.onHandleIntent(intent);
        assertEquals(2, swrvePushSDK.isSwrveRemoteNotificationExecuted);

        //Check no timestamp scenario
        Bundle noTimestamp = new Bundle();
        noTimestamp.putString("text", "validBundle");
        noTimestamp.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        intent.putExtras(noTimestamp);
        service.onHandleIntent(intent);

        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
        assertNumberOfNotifications(0);

        //Check good scenario
        Bundle extras = new Bundle();
        extras.putString("text", "validBundle");
        extras.putString("customData", "some custom values");
        extras.putString("sound", "default");
        extras.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        int firstTimestamp = generateTimestampId();
        extras.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(extras);
        service.onHandleIntent(intent);

        assertNotification("validBundle", "content://settings/system/notification_sound", extras);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
        assertNumberOfNotifications(1);

        //Try sending duplicate
        service.onHandleIntent(intent);
        assertNumberOfNotifications(1);

        //Now send another 16 unique notifications to overfill the buffer
        int newTimestamp = generateTimestampId();
        for (int i=0; i<DEFAULT_PUSH_ID_CACHE_SIZE; ++i) {
            extras.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(newTimestamp));
            intent.putExtras(extras);
            service.onHandleIntent(intent);
            newTimestamp++;

            //Let the notification manager do its thing
            Thread.sleep(100);
        }

        //Assert there are 17 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 1);

        //Now we should be able to reuse the first notification timestamp
        extras.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(extras);
        service.onHandleIntent(intent);
        //Let the notification manager do its thing
        Thread.sleep(100);

        //Assert there are 18 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 2);

        //Now make use of the ability to configure the duplication cache size
        extras.putInt(SwrvePushConstants.PUSH_ID_CACHE_SIZE_KEY, 1);
        intent.putExtras(extras);
        service.onHandleIntent(intent);
        //Let the notification manager do its thing
        Thread.sleep(100);

        //Assert there are 18 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 2);

        //Send again to make sure dedupe is happening with newest item.
        service.onHandleIntent(intent);
        //Let the notification manager do its thing
        Thread.sleep(100);

        //Assert there are 18 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 2);

        //Change the key to new timestamp
        extras.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(newTimestamp));
        intent.putExtras(extras);
        service.onHandleIntent(intent);
        //Let the notification manager do its thing
        Thread.sleep(100);

        //Assert there are 19 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 3);

        //Use the previous timestamp - but now expect an extra notification since the cache is only 1.
        extras.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(extras);
        service.onHandleIntent(intent);
        //Let the notification manager do its thing
        Thread.sleep(100);

        //Assert there are 20 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 4);
    }

    @Test
    public void testServiceCustomSound() throws Exception {
        Intent intent = new Intent();
        Bundle validBundleCustomSound = new Bundle();
        validBundleCustomSound.putString("text", "validBundleCustomSound");
        validBundleCustomSound.putString("sound", "customSound");
        validBundleCustomSound.putString("customData", "some custom values");
        validBundleCustomSound.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        validBundleCustomSound.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        intent.putExtras(validBundleCustomSound);
        service.onHandleIntent(intent);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
        assertNotification("validBundleCustomSound", "android.resource://com.swrve.sdk.test/raw/customSound", validBundleCustomSound);
    }

    @Test
    public void testServiceWithQaUser() throws Exception {
        JSONObject jsonObject = new JSONObject("{\"campaigns\":[],\"logging\":true,\"reset_device_state\":true,\"logging_url\":\"http:\\/\\/1031.qa-log.swrve.com\"}");
        SwrveQAUser swrveQAUser = new SwrveQAUser(1, "apiKey", "userId", new RESTClient(5000), jsonObject);
        swrveQAUser.bindToServices();

        Intent intent = new Intent();
        Bundle validBundle = new Bundle();
        validBundle.putString("text", "hello there");
        validBundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        validBundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        intent.putExtras(validBundle);
        service.onHandleIntent(intent);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
        assertNotification("hello there", null, validBundle);
    }

    @Test
    public void testWithDeeplink() throws Exception {
        Intent intent = new Intent();
        Bundle deeplinkBundle = new Bundle();
        deeplinkBundle.putString("text", "deeplinkBundle");
        deeplinkBundle.putString(SwrvePushConstants.DEEPLINK_KEY, "swrve://deeplink/config");
        deeplinkBundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        deeplinkBundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        intent.putExtras(deeplinkBundle);
        service.onHandleIntent(intent);
        assertNotification("deeplinkBundle", null, deeplinkBundle);
    }

    private boolean listenerCalledWithRightParams;
    @Test
    public void testSilentPush() throws Exception {
        Intent intent = new Intent();
        listenerCalledWithRightParams = false;

        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        Swrve swrveSpy = spy(swrveReal);

        swrvePushSDK.setSilentPushListener(new SwrveSilentPushListener() {
            @Override
            public void onSilentPush(Context context, JSONObject payload) {
                if (!listenerCalledWithRightParams) {
                    try {
                        listenerCalledWithRightParams = payload.getString("custom").equals("value1");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // Send some valid silent pushes
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_SILENT_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrvePushConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value1\"}");
        intent.putExtras(bundle);
        swrvePushSDK.dateNow = SwrveTestUtils.parseDate("2017/01/01 0:00");
        service.onHandleIntent(intent);
        // Silent push 2
        bundle.putString(SwrvePushConstants.SWRVE_SILENT_TRACKING_KEY, "2");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrvePushConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value2\"}");
        intent.putExtras(bundle);
        swrvePushSDK.dateNow = SwrveTestUtils.parseDate("2017/01/01 10:00");
        service.onHandleIntent(intent);
        // Silent push 3
        bundle.putString(SwrvePushConstants.SWRVE_SILENT_TRACKING_KEY, "3");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrvePushConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value3\"}");
        intent.putExtras(bundle);
        swrvePushSDK.dateNow = SwrveTestUtils.parseDate("2017/01/01 11:00");
        service.onHandleIntent(intent);

        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
        assertNumberOfNotifications(0);
        assertTrue(listenerCalledWithRightParams);

        // Init the SDK, should read influence data
        swrvePushSDK.dateNow = SwrveTestUtils.parseDate("2017/01/01 13:00");
        swrveReal.init(mActivity);
        swrveReal.onPause();
        swrveReal.onResume(mActivity);
        assertEquals(1, swrvePushSDK.processInfluenceDataCallCount);
        swrveReal.onPause();
        swrveReal.onResume(mActivity);
        assertEquals(2, swrvePushSDK.processInfluenceDataCallCount);

        List<Intent> eventIntents = shadowApplication.getBroadcastIntents();
        assertEquals(1, eventIntents.size());
        Intent eventIntent = eventIntents.get(0);
        ArrayList extras = (ArrayList) eventIntent.getExtras().get("swrve_wakeful_events");
        assertEquals(2, extras.size());
        JSONObject event1 = new JSONObject((String) extras.get(0));
        assertEquals("generic_campaign_event", event1.get("type"));
        assertEquals(2, event1.get("id"));
        assertEquals("push", event1.get("campaignType"));
        assertEquals("influenced", event1.get("actionType"));
        JSONObject payload1 = event1.getJSONObject("payload");
        assertEquals("540", payload1.get("delta"));

        JSONObject event2 = new JSONObject((String) extras.get(1));
        assertEquals(3, event2.get("id"));

        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testAdvancedBigTextPush() throws Exception {
        service.checkDupes = true;
        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\"}}\n";
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "text");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);
        service.onHandleIntent(intent);

        assertNotification("text", "content://settings/system/notification_sound", bundle);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
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
        service.checkDupes = true;
        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"accent\": \"#00FF0000\",\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\"}}\n";
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "text");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);
        service.onHandleIntent(intent);

        assertNotification("text", "content://settings/system/notification_sound", bundle);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(Color.RED , notification.color);
    }

    @Test
    public void testAdvancedUpdateExisting() throws Exception {
        service.checkDupes = true;
        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"notification_id\": 12,\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\"}}\n";
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);
        service.onHandleIntent(intent);

        String updateJson = "{\n" +
                " \"title\": \"update title\",\n" +
                " \"subtitle\": \"update subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"notification_id\": 12,\n" +
                "\"expanded\": { \"title\": \"[expanded updated title]\",\n" +
                "                \"body\": \"[expanded update body]\"}}\n";
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, updateJson);
        bundle.putString("text", "update body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int secondTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(secondTimestamp));
        intent.putExtras(bundle);
        service.onHandleIntent(intent);

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
        /**
         * Check if the SDK will revert to original notification if the version is wrong
         * **/
        service.checkDupes = true;
        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
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
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "original push notification");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);
        service.onHandleIntent(intent);

        assertNotification("original push notification", "content://settings/system/notification_sound", bundle);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
        assertNumberOfNotifications(1);
    }

    @Test
    public void testAdvancedUnknownType() throws Exception {
        /**
         * This test is to check if the SDK can handle new types of media before it's added
         * This should default back to BigText and parse what it can.
         * **/
        service.checkDupes = true;
        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
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
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "original push notification");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);
        service.onHandleIntent(intent);

        assertNotification("original push notification", "content://settings/system/notification_sound", bundle);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
        assertNumberOfNotifications(1);
    }

    @Test
    public void testAdvancedPushLockScreenMessage() throws Exception {
        service.checkDupes = true;
        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"visibility\": \"private\",\n" +
                " \"lock_screen_msg\": \"lock screen test message\",\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\"}}\n";
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "original push notification");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);
        service.onHandleIntent(intent);

        assertNotification("original push notification", "content://settings/system/notification_sound", bundle);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
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
        service.checkDupes = true;
        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
                " \"visibility\": \"private\",\n" +
                " \"lock_screen_msg\": \"lock screen test message\",\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\"}}\n";
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "original push notification");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);
        service.onHandleIntent(intent);

        assertNotification("original push notification", "content://settings/system/notification_sound", bundle);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(Notification.VISIBILITY_PRIVATE ,notification.visibility);
        assertEquals("lock screen test message", notification.publicVersion.tickerText);
    }

    @Test
    public void testAdvancedBigImagePush() throws Exception {
        service.checkDupes = true;

        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
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

        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);


        SwrvePushNotificationConfig config = SwrvePushNotificationConfig.getInstance(RuntimeEnvironment.application);
        SwrvePushMediaHelper mockMedia = Mockito.mock(SwrvePushMediaHelper.class);
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        when(mockMedia.downloadBitmapImageFromUrl("https://valid-image.png")).thenReturn(bmp);
        config.mediaHelper = mockMedia;

        Field instanceField = SwrvePushNotificationConfig.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, config);

        service.onHandleIntent(intent);

        assertNotification("[rich body]", "content://settings/system/notification_sound", bundle);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
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
    public void testAdvancedBigImagePushVideoFallback() throws Exception {
        service.checkDupes = true;

        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
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

        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);


        SwrvePushNotificationConfig config = SwrvePushNotificationConfig.getInstance(RuntimeEnvironment.application);
        SwrvePushMediaHelper mockMedia = Mockito.mock(SwrvePushMediaHelper.class);
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        when(mockMedia.downloadBitmapImageFromUrl("https://fail-image.png")).thenReturn(null);
        when(mockMedia.downloadBitmapImageFromUrl("https://valid-image.png")).thenReturn(bmp);
        config.mediaHelper = mockMedia;

        Field instanceField = SwrvePushNotificationConfig.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, config);

        service.onHandleIntent(intent);

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
        assertEquals("com.swrve.sdk.SwrvePushEngageReceiver", shadowIntent.getComponent().getClassName());
        Bundle intentExtras = shadowIntent.getBundleExtra(SwrvePushConstants.PUSH_BUNDLE);
        assertEquals("https://fallback_sd", intentExtras.get(SwrvePushConstants.DEEPLINK_KEY));

        // Although they are the same, equals returns false, must fix
        // Icon icon = Icon.createWithBitmap(bmp);
        // assertEquals(icon, notification.getLargeIcon());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Config(sdk = Build.VERSION_CODES.N)
    @Test
    public void testAdvancedBigImagePushBigTextFallback() throws Exception {
        service.checkDupes = true;

        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
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
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "fallback body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);

        SwrvePushNotificationConfig config = SwrvePushNotificationConfig.getInstance(RuntimeEnvironment.application);
        SwrvePushMediaHelper mockMedia = Mockito.mock(SwrvePushMediaHelper.class);
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        when(mockMedia.downloadBitmapImageFromUrl("https://fail-image.png")).thenReturn(null);
        when(mockMedia.downloadBitmapImageFromUrl("https://valid-image.png")).thenReturn(bmp);
        config.mediaHelper = mockMedia;

        Field instanceField = SwrvePushNotificationConfig.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, config);

        service.onHandleIntent(intent);

        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
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
        assertEquals("com.swrve.sdk.SwrvePushEngageReceiver", shadowIntent.getComponent().getClassName());
        Bundle intentExtras = shadowIntent.getBundleExtra(SwrvePushConstants.PUSH_BUNDLE);
        assertNull(intentExtras.get(SwrvePushConstants.DEEPLINK_KEY)); // this should not be set
    }

    @Test
    public void testBadImageAndNoFallback() throws Exception {
        service.checkDupes = true;

        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
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
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "fallback body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);

        SwrvePushNotificationConfig config = SwrvePushNotificationConfig.getInstance(RuntimeEnvironment.application);
        SwrvePushMediaHelper mockMedia = Mockito.mock(SwrvePushMediaHelper.class);
        when(mockMedia.downloadBitmapImageFromUrl("https://fail-image.png")).thenReturn(null);
        config.mediaHelper = mockMedia;

        Field instanceField = SwrvePushNotificationConfig.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, config);

        service.onHandleIntent(intent);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
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
        service.checkDupes = true;
        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
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
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "text");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);
        service.onHandleIntent(intent);

        assertNotification("text", "content://settings/system/notification_sound", bundle);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("text", notification.tickerText);
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());
        assertEquals("[expanded body]", shadowNotification.getBigText());// Robolectric does not give the ability to check the subtitle yet
        // assertEquals("[rich subtitle]", shadowNotification.getSubText());

        Notification.Action[] actions = notification.actions;
        assertEquals(actions.length, 1);
        Notification.Action testAction = actions[0];
        assertEquals(testAction.title, "[button text 1]");
        Intent testIntent = getIntent(testAction.actionIntent);
        Bundle extras = testIntent.getExtras();
        assertEquals("0", extras.getString(SwrvePushConstants.PUSH_ACTION_KEY));
        assertEquals(PushPayloadButton.ActionType.OPEN_URL, extras.get(SwrvePushConstants.PUSH_ACTION_TYPE_KEY));
        assertEquals("https://lovelyURL", extras.getString(SwrvePushConstants.PUSH_ACTION_URL_KEY));
        assertEquals(221, extras.getInt(SwrvePushConstants.PUSH_NOTIFICATION_ID));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testAdvancedBigImagePushWith2Actions() throws Exception {
        service.checkDupes = true;

        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
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
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);

        SwrvePushNotificationConfig config = SwrvePushNotificationConfig.getInstance(RuntimeEnvironment.application);
        SwrvePushMediaHelper mockMedia = Mockito.mock(SwrvePushMediaHelper.class);
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        when(mockMedia.downloadBitmapImageFromUrl("https://valid-image.png")).thenReturn(bmp);
        config.mediaHelper = mockMedia;

        Field instanceField = SwrvePushNotificationConfig.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, config);

        service.onHandleIntent(intent);

        assertNotification("[rich body]", "content://settings/system/notification_sound", bundle);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
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
        assertEquals("0", extras.getString(SwrvePushConstants.PUSH_ACTION_KEY));
        assertEquals(PushPayloadButton.ActionType.OPEN_URL, extras.get(SwrvePushConstants.PUSH_ACTION_TYPE_KEY));
        assertEquals("https://lovelyURL", extras.getString(SwrvePushConstants.PUSH_ACTION_URL_KEY));
        assertEquals(222, extras.getInt(SwrvePushConstants.PUSH_NOTIFICATION_ID));

        testAction = actions[1];
        assertEquals(testAction.title, "[button text 2]");
        testIntent = getIntent(testAction.actionIntent);
        extras = testIntent.getExtras();
        assertEquals("1", extras.getString(SwrvePushConstants.PUSH_ACTION_KEY));
        assertEquals(PushPayloadButton.ActionType.OPEN_APP, extras.get(SwrvePushConstants.PUSH_ACTION_TYPE_KEY));
        assertNull(extras.get(SwrvePushConstants.PUSH_ACTION_URL_KEY));
        assertEquals(222, extras.getInt(SwrvePushConstants.PUSH_NOTIFICATION_ID));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testAdvancedBigTextPushWith3Actions() throws Exception {
        service.checkDupes = true;

        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
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
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "text");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);
        service.onHandleIntent(intent);

        assertNotification("text", "content://settings/system/notification_sound", bundle);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
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
        assertEquals("0",extras.getString(SwrvePushConstants.PUSH_ACTION_KEY));
        assertEquals(PushPayloadButton.ActionType.OPEN_URL, extras.get(SwrvePushConstants.PUSH_ACTION_TYPE_KEY));
        assertEquals("https://lovelyURL", extras.getString(SwrvePushConstants.PUSH_ACTION_URL_KEY));
        assertEquals(223, extras.getInt(SwrvePushConstants.PUSH_NOTIFICATION_ID));

        testAction = actions[1];
        assertEquals(testAction.title, "[button text 2]");
        testIntent = getIntent(testAction.actionIntent);
        extras = testIntent.getExtras();
        assertEquals("1",extras.getString(SwrvePushConstants.PUSH_ACTION_KEY));
        assertEquals(PushPayloadButton.ActionType.OPEN_APP, extras.get(SwrvePushConstants.PUSH_ACTION_TYPE_KEY));
        assertNull(extras.get(SwrvePushConstants.PUSH_ACTION_URL_KEY));
        assertEquals(223, extras.getInt(SwrvePushConstants.PUSH_NOTIFICATION_ID));

        testAction = actions[2];
        assertEquals(testAction.title, "[button text 3]");
        testIntent = getIntent(testAction.actionIntent);
        extras = testIntent.getExtras();
        assertEquals("2",extras.getString(SwrvePushConstants.PUSH_ACTION_KEY));
        assertEquals(PushPayloadButton.ActionType.DISMISS, extras.get(SwrvePushConstants.PUSH_ACTION_TYPE_KEY));
        assertNull(extras.get(SwrvePushConstants.PUSH_ACTION_URL_KEY));
        assertEquals(223, extras.getInt(SwrvePushConstants.PUSH_NOTIFICATION_ID));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testButtonClickEvents() throws Exception {
        SwrveConfig config = new SwrveConfig();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        swrveReal.init(mActivity);

        try {
            service.checkDupes = true;
            Intent intent = new Intent();
            // Send a valid Rich Payload
            Bundle bundle = new Bundle();
            bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
            bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
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
            bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
            bundle.putString("text", "text");
            bundle.putString("customData", "some custom values");
            bundle.putString("sound", "default");
            int firstTimestamp = generateTimestampId();
            bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
            intent.putExtras(bundle);
            service.onHandleIntent(intent);

            Notification notification = assertNotification("text", "content://settings/system/notification_sound", bundle);
            assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
            assertNumberOfNotifications(1);

            Notification.Action[] actions = notification.actions;
            assertEquals(actions.length, 3);
            Notification.Action testAction = actions[2];
            assertEquals(testAction.title, "[button text 3]");
            Intent buttonClickIntent = getIntent(testAction.actionIntent);
            SwrvePushEngageReceiver receiver = new SwrvePushEngageReceiver();
            receiver.onReceive(mActivity, buttonClickIntent);

            // Resolve SwrveEngageEventSender intent
            List<Intent> broadcastIntents = mShadowActivity.getBroadcastIntents();
            assertEquals(1, broadcastIntents.size());
            Intent engageEventIntent = broadcastIntents.get(0);
            // Clear pending intents
            mShadowActivity.getBroadcastIntents().clear();
            SwrveEngageEventSender engageEventSender = new SwrveEngageEventSender();
            engageEventSender.onReceive(mActivity, engageEventIntent);

            // Should send a button click event and an engagement event
            List<String> events = SwrveEngageEventSenderTest.assertEventCount(mShadowActivity, 2, 2);
            SwrveEngageEventSenderTest.assertButtonClickedEvent(events.get(0), "2");
            SwrveEngageEventSenderTest.assertEngagedEvent(events.get(1), "Swrve.Messages.Push-1.engaged");

            // Should not send an influence event when the app is opened (emulate app start/resume)
            swrveReal.onResume(mActivity);
            SwrveEngageEventSenderTest.assertEventCount(mShadowActivity, 2, 2); // No new events should be queued
        } catch(Exception exp) {
            throw exp;
        } finally {
            swrveReal.shutdown();
            SwrveTestUtils.removeSwrveSDKSingletonInstance();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testEngagedEvents() throws Exception {
        SwrveConfig config = new SwrveConfig();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        swrveReal.init(mActivity);

        try {
            service.checkDupes = true;
            Intent intent = new Intent();
            // Send a valid Rich Payload
            Bundle bundle = new Bundle();
            bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
            bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
            bundle.putString("text", "body");
            bundle.putString("customData", "some custom values");
            bundle.putString("sound", "default");
            int firstTimestamp = generateTimestampId();
            bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
            intent.putExtras(bundle);
            service.onHandleIntent(intent);

            Notification notification = assertNotification("body", "content://settings/system/notification_sound", bundle);
            assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
            assertNumberOfNotifications(1);

            notification.contentIntent.send();

            // Launch SwrvePushEngageReceiver (imitate single engagement with notification)
            List<Intent> broadcastIntents = mShadowActivity.getBroadcastIntents();
            assertEquals(1, broadcastIntents.size());
            Intent engageEventIntent = broadcastIntents.get(0);
            SwrvePushEngageReceiver engageReceiver = new SwrvePushEngageReceiver();
            // Clear pending intents
            mShadowActivity.getBroadcastIntents().clear();
            engageReceiver.onReceive(mActivity, engageEventIntent);

            // Resolve SwrveEngageEventSender intent
            broadcastIntents = mShadowActivity.getBroadcastIntents();
            assertEquals(2, broadcastIntents.size());
            assertEquals("com.swrve.sdk.SwrveEngageEventSender", broadcastIntents.get(0).getComponent().getShortClassName());
            assertEquals("android.intent.action.CLOSE_SYSTEM_DIALOGS", broadcastIntents.get(1).getAction());

            Intent engageEventSendIntent = broadcastIntents.get(0);
            // Clear pending intents
            mShadowActivity.getBroadcastIntents().clear();
            SwrveEngageEventSender engageEventSender = new SwrveEngageEventSender();
            engageEventSender.onReceive(mActivity, engageEventSendIntent);

            // Should send a button click event and an engagement event
            List<String> events = SwrveEngageEventSenderTest.assertEventCount(mShadowActivity, 1, 1);
            SwrveEngageEventSenderTest.assertEngagedEvent(events.get(0), "Swrve.Messages.Push-1.engaged");

            // Should not send an influence event when the app is opened (emulate app start/resume)
            swrveReal.onResume(mActivity);
            SwrveEngageEventSenderTest.assertEventCount(mShadowActivity, 1, 1); // No new events should be queued
        } catch(Exception exp) {
            throw exp;
        } finally {
            swrveReal.shutdown();
            SwrveTestUtils.removeSwrveSDKSingletonInstance();
        }
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Test
    public void testNotificationChannelFromConfig() throws Exception {
        service.checkDupes = true;

        NotificationChannel channel = new NotificationChannel("swrve_channel", "Swrve channel", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("All the news from Swrve");
        ISwrveCommon mockSwrveCommon = Mockito.mock(ISwrveCommon.class);
        when(mockSwrveCommon.getDefaultNotificationChannel()).thenReturn(channel);
        SwrveCommon.setSwrveCommon(mockSwrveCommon);

        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1\n" +
                "}\n";
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);

        SwrvePushNotificationConfig config = SwrvePushNotificationConfig.getInstance(RuntimeEnvironment.application);
        Field instanceField = SwrvePushNotificationConfig.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, config);

        service.onHandleIntent(intent);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
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
        notificationManager.createNotificationChannel(new NotificationChannel(channelId, "some channel", NotificationManager.IMPORTANCE_DEFAULT) );

        service.checkDupes = true;

        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"channel_id\": \"" + channelId + "\",\n" +
                " \"version\": 1\n" +
                "}\n";
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);

        SwrvePushNotificationConfig config = SwrvePushNotificationConfig.getInstance(RuntimeEnvironment.application);
        Field instanceField = SwrvePushNotificationConfig.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, config);

        service.onHandleIntent(intent);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
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

        service.checkDupes = true;

        Intent intent = new Intent();
        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"channel_id\": \"not_found\",\n" +
                " \"channel\": { \"id\": \"" + channelId + "\",\n" +
                " \"name\": \"channel_name\",\n" +
                " \"importance\": \"high\" },\n" +
                " \"version\": 1\n" +
                "}\n";
        bundle.putString(SwrvePushConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString("text", "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(bundle);

        SwrvePushNotificationConfig config = SwrvePushNotificationConfig.getInstance(RuntimeEnvironment.application);
        Field instanceField = SwrvePushNotificationConfig.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, config);

        service.onHandleIntent(intent);
        assertEquals(1, swrvePushSDK.isSwrveRemoteNotificationExecuted);
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
        assertEquals("com.swrve.sdk.SwrvePushEngageReceiver", intent.getComponent().getClassName());
        Bundle intentExtras = intent.getBundleExtra(SwrvePushConstants.PUSH_BUNDLE);
        assertNotNull(intentExtras);
        for (String key : extras.keySet()) {
            assertTrue(intentExtras.containsKey(key));
            assertEquals(extras.get(key), intentExtras.getString(key));
        }
        ShadowNotification shadowNotification = shadowOf(notification);
        // In the case of a rich push, we should expect the title to be set to something else
        if (!intentExtras.containsKey(SwrvePushConstants.SWRVE_PAYLOAD_KEY)) {
            assertEquals("Android Test App", shadowNotification.getContentTitle());
        } else {
            PushPayload extrasPPayload = PushPayload.fromJson(extras.getString(SwrvePushConstants.SWRVE_PAYLOAD_KEY));
            if(extrasPPayload != null) {
                if(extrasPPayload.getVersion() == SwrvePushConstants.SWRVE_PUSH_VERSION) {
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

    class TestableSwrvePushSDK extends SwrvePushSDK {
        public Date dateNow;
        int isSwrveRemoteNotificationExecuted = 0;
        int processInfluenceDataCallCount = 0;

        public TestableSwrvePushSDK(Context context) {
            super(context);
            // Set as the main instance
            instance = this;
        }

        @Override
        public void processRemoteNotification(Bundle msg, boolean checkDupes) {
            super.processRemoteNotification(msg, checkDupes);
            isSwrveRemoteNotificationExecuted = SwrvePushSDK.isSwrveRemoteNotification(msg)? 1 : 2;
        }

        @Override
        void processInfluenceData(ISwrveCommon sdk) {
            processInfluenceDataCallCount++;
            super.processInfluenceData(sdk);
        }

        @Override
        protected Date getNow() {
            if (dateNow != null) {
                return dateNow;
            }
            return super.getNow();
        }
    }

    public void setSwrvePushSDKInstance(SwrvePushSDK instance) throws Exception {
        Field hack = SwrvePushSDK.class.getDeclaredField("instance");
        hack.setAccessible(true);
        hack.set(null, instance);
    }

    public class GenericSwrvePushService extends IntentService implements SwrvePushService {

        private SwrvePushSDK pushSDK;
        public boolean checkDupes = false;

        public GenericSwrvePushService(String name) {
            super(name);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            pushSDK = SwrvePushSDK.getInstance();
            pushSDK.setService(this);
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            if(intent != null) {
                pushSDK.processRemoteNotification(intent.getExtras(), checkDupes);
            }
        }

        @Override
        public void processNotification(final Bundle msg) {
            pushSDK.processNotification(msg);
        }

        @Override
        public boolean mustShowNotification() {
            return true;
        }

        @Override
        public int showNotification(NotificationManager notificationManager, Notification notification) {
            return pushSDK.showNotification(notificationManager, notification);
        }

        @Override
        public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
            return pushSDK.createNotificationBuilder(msgText, msg);
        }

        @Override
        public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
            return pushSDK.createNotification(msg, contentIntent);
        }

        @Override
        public PendingIntent createPendingIntent(Bundle msg) {
            return pushSDK.createPendingIntent(msg);
        }

        @Override
        public Intent createIntent(Bundle msg) {
            return pushSDK.createIntent(msg);
        }
    }

    public static Intent getIntent(PendingIntent pendingIntent) {
        return ((ShadowPendingIntent) Shadow.extract(pendingIntent)).getSavedIntent();
    }
}
