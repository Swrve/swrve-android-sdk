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
import android.support.v4.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.notifications.model.SwrveNotification;
import com.swrve.sdk.notifications.model.SwrveNotificationButton;
import com.swrve.sdk.qa.SwrveQAUser;
import com.swrve.sdk.rest.RESTClient;
import com.swrve.sdk.test.MainActivity;

import org.json.JSONObject;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class SwrveNotificationBuilderTest extends SwrveBaseTest {

    private SwrvePushServiceManager pushServiceManager;
    private SwrveNotificationConfig notificationConfig = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, com.swrve.sdk.test.R.drawable.ic_launcher, null)
            .activityClass(MainActivity.class)
            .build();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ISwrveCommon swrveCommonSpy = mock(ISwrveCommon.class);
        SwrveCommon.setSwrveCommon(swrveCommonSpy);
        doReturn(notificationConfig).when(swrveCommonSpy).getNotificationConfig();
        doReturn(RuntimeEnvironment.application.getCacheDir()).when(swrveCommonSpy).getCacheDir(RuntimeEnvironment.application);
        pushServiceManager = new SwrvePushServiceManager(RuntimeEnvironment.application);
    }

    @Test
    public void testServiceCustomSound() {
        Bundle validBundleCustomSound = new Bundle();
        validBundleCustomSound.putString(SwrveNotificationConstants.TEXT_KEY, "validBundleCustomSound");
        validBundleCustomSound.putString("sound", "customSound");
        validBundleCustomSound.putString("customData", "some custom values");
        validBundleCustomSound.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        validBundleCustomSound.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        pushServiceManager.processMessage(validBundleCustomSound);
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
        pushServiceManager.processMessage(validBundle);
        assertNotification("hello there", null, validBundle);
    }

    @Test
    public void testWithDeeplink() {
        Bundle deeplinkBundle = new Bundle();
        deeplinkBundle.putString(SwrveNotificationConstants.TEXT_KEY, "deeplinkBundle");
        deeplinkBundle.putString(SwrveNotificationConstants.DEEPLINK_KEY, "swrve://deeplink/config");
        deeplinkBundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        deeplinkBundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        pushServiceManager.processMessage(deeplinkBundle);
        assertNotification("deeplinkBundle", null, deeplinkBundle);
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
        pushServiceManager.processMessage(bundle);

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
    }

    @Test
    public void testAlternateAccentColorPush() {
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

        pushServiceManager.processMessage(bundle);

        assertNotification("text", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(Color.RED, notification.color);
    }

    @Test
    public void testAdvancedUpdateExisting() {
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

        pushServiceManager.processMessage(bundle);

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

        pushServiceManager.processMessage(bundle);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);

        assertEquals("update body", notification.tickerText);
        assertEquals("[expanded updated title]", shadowNotification.getBigContentTitle());
        assertEquals("[expanded update body]", shadowNotification.getBigText());
    }

    @Test
    public void testAdvancedUnknownVersion() {
        // Check if the SDK will revert to original notification if the version is wrong
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
                "              \"body\": \"[rich body]\",\n" +
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://media.jpg\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"https://valid-image.png\",\n" +
                "              \"fallback_sd\": \"https://video.com\"\n }," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://valid-image.png\"} \n" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "original push notification - testAdvancedUnknownVersion");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        mockAllImageDownloads(builderSpy);
        displayNotification(builderSpy, bundle);

        assertNumberOfNotifications(1);
        assertNotification("original push notification - testAdvancedUnknownVersion", "content://settings/system/notification_sound", bundle);
    }

    @Test
    public void testAdvancedUnknownType() {
        // This test is to check if the SDK can handle new types of media before it's added
        // This should default back to BigText and parse what it can.
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
                "              \"body\": \"[rich body]\",\n" +
                "              \"type\": \"unknown\",\n" +
                "              \"url\": \"https://media.jpg\"},\n" +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://valid-image.png\"} \n" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "original push notification - testAdvancedUnknownType");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        mockAllImageDownloads(builderSpy);
        displayNotification(builderSpy, bundle);

        assertNumberOfNotifications(1);
        assertNotification("original push notification - testAdvancedUnknownType", "content://settings/system/notification_sound", bundle);
    }

    private void mockAllImageDownloads(SwrveNotificationBuilder builderSpy) {
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        doReturn(bmp).when(builderSpy).getImageFromUrl(anyString());
    }

    private void displayNotification(SwrveNotificationBuilder builderSpy, Bundle bundle) {
        SwrveNotificationTestUtils.displayNotification(mActivity, builderSpy, bundle);
    }

    @Test
    public void testAdvancedPushLockScreenMessage() {
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
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "original push notification - testAdvancedPushLockScreenMessage");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        mockAllImageDownloads(builderSpy);
        displayNotification(builderSpy, bundle);

        assertNumberOfNotifications(1);
        assertNotification("original push notification - testAdvancedPushLockScreenMessage", "content://settings/system/notification_sound", bundle);


        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility);
        assertEquals("lock screen test message", notification.publicVersion.tickerText);
    }

    @Test
    public void testAdvancedPushLockScreenMessageNoMedia() {
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
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "original push notification - testAdvancedPushLockScreenMessageNoMedia");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        mockAllImageDownloads(builderSpy);
        displayNotification(builderSpy, bundle);

        assertNotification("original push notification - testAdvancedPushLockScreenMessageNoMedia", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility);
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
                "              \"body\": \"[rich body]\",\n" +
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://valid-image.png\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"https://valid-image.png\",\n" +
                "              \"fallback_sd\": \"https://video.com\"\n }," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://valid-image.png\"} \n" +
                "}\n";

        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        doReturn(bmp).when(builderSpy).getImageFromUrl("https://valid-image.png");

        displayNotification(builderSpy, bundle);

        assertNotification("[rich body]", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("[rich body]", notification.tickerText);
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());
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
                "              \"body\": \"[rich body]\",\n" +
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://media.jpg\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"https://valid-image.png\",\n" +
                "              \"fallback_sd\": \"https://fallback_sd\"\n }," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\":  \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://valid-image.png\"} \n" +
                "}\n";

        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        doReturn(null).when(builderSpy).getImageFromUrl(anyString());
        doReturn(null).when(builderSpy).getImageFromUrl("https://fail-image.png");
        doReturn(bmp).when(builderSpy).getImageFromUrl("https://valid-image.png");

        displayNotification(builderSpy, bundle);

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
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Config(sdk = Build.VERSION_CODES.N)
    @Test
    public void testAdvancedBigImagePushBigTextFallback() {
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
                "              \"body\": \"[rich body]\",\n" +
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://fail-image.png\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"https://fail-image.png\",\n" +
                "              \"fallback_sd\": \"https://fallback_sd\"\n }," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\":  \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://valid-image.png\"} \n" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "fallback body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        doReturn(null).when(builderSpy).getImageFromUrl(anyString());
        doReturn(null).when(builderSpy).getImageFromUrl("https://fail-image.png");
        doReturn(bmp).when(builderSpy).getImageFromUrl("https://valid-image.png");

        displayNotification(builderSpy, bundle);

        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("fallback body", shadowNotification.getContentText());
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());

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
    public void testBadImageAndNoFallback() {
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
                "              \"body\": \"[rich body]\",\n" +
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://fail-image.png\" }" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "fallback body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        when(builderSpy.getImageFromUrl(anyString())).thenReturn(null);
        when(builderSpy.getImageFromUrl("https://fail-image.png")).thenReturn(null);

        displayNotification(builderSpy, bundle);

        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals("fallback body", notification.tickerText);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("fallback title", shadowNotification.getBigContentTitle());

        assertNull(shadowNotification.getBigPicture());
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testAdvancedBigTextPushWith1Action() {
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        displayNotification(builderSpy, bundle);

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
    public void testAdvancedBigImagePushWith2Actions() {
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
                "              \"body\": \"[rich body]\",\n" +
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        doReturn(null).when(builderSpy).getImageFromUrl(anyString());
        doReturn(bmp).when(builderSpy).getImageFromUrl("https://valid-image.png");

        displayNotification(builderSpy, bundle);

        assertNotification("[rich body]", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("[rich body]", notification.tickerText);
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());
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
    public void testAdvancedBigTextPushWith3Actions() {
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        displayNotification(builderSpy, bundle);

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

        Notification.Action[] actions = notification.actions;
        assertEquals(actions.length, 3);
        Notification.Action testAction = actions[0];
        assertEquals(testAction.title, "[button text 1]");
        Intent testIntent = getIntent(testAction.actionIntent);
        Bundle extras = testIntent.getExtras();
        assertEquals("0", extras.getString(SwrveNotificationConstants.CONTEXT_ID_KEY));
        assertEquals(SwrveNotificationButton.ActionType.OPEN_URL, extras.get(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY));
        assertEquals("https://lovelyURL", extras.getString(SwrveNotificationConstants.PUSH_ACTION_URL_KEY));
        assertEquals(223, extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID));

        testAction = actions[1];
        assertEquals(testAction.title, "[button text 2]");
        testIntent = getIntent(testAction.actionIntent);
        extras = testIntent.getExtras();
        assertEquals("1", extras.getString(SwrveNotificationConstants.CONTEXT_ID_KEY));
        assertEquals(SwrveNotificationButton.ActionType.OPEN_APP, extras.get(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY));
        assertNull(extras.get(SwrveNotificationConstants.PUSH_ACTION_URL_KEY));
        assertEquals(223, extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID));

        testAction = actions[2];
        assertEquals(testAction.title, "[button text 3]");
        testIntent = getIntent(testAction.actionIntent);
        extras = testIntent.getExtras();
        assertEquals("2", extras.getString(SwrveNotificationConstants.CONTEXT_ID_KEY));
        assertEquals(SwrveNotificationButton.ActionType.DISMISS, extras.get(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY));
        assertNull(extras.get(SwrveNotificationConstants.PUSH_ACTION_URL_KEY));
        assertEquals(223, extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID));
    }

    @Test
    public void testUniqueRequestCodes() {

        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"version\": 1,\n" +
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        int requestCodeStart = builderSpy.requestCode;

        displayNotification(builderSpy, bundle);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();

        Notification notification = notifications.get(0);
        Notification.Action[] actions = notification.actions;
        assertEquals(requestCodeStart, shadowOf(actions[0].actionIntent).getRequestCode());
        assertEquals(requestCodeStart + 1, shadowOf(actions[1].actionIntent).getRequestCode());
        assertEquals(requestCodeStart + 2, shadowOf(actions[2].actionIntent).getRequestCode());
        assertEquals(requestCodeStart + 3, shadowOf(notification.contentIntent).getRequestCode());
        assertEquals(requestCodeStart + 4, builderSpy.requestCode); // final value of requestCode
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testButtonClickEvents() throws Exception {
        SwrveConfig config = new SwrveConfig();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        Swrve swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class));
        swrveSpy.init(mActivity);

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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        displayNotification(builderSpy, bundle);

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
        for (String event : events) { // can't guarantee which event is in list first so just iterate through them
            if (event.contains("generic_campaign_event")) {
                SwrveTestUtils.assertGenericEvent(event, "2", GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK, null);
            } else {
                SwrveNotificationTestUtils.assertEngagedEvent(event, "Swrve.Messages.Push-1.engaged");
            }
        }

        // Should not send an influence event when the app is opened (emulate app start/resume)
        swrveSpy.onResume(mActivity);
        // No new events should be queued
        events = SwrveTestUtils.getEventsQueued(mShadowActivity);
        assertEquals(2, events.size());
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testEngagedEvents() throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.setNotificationConfig(notificationConfig);
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        Swrve swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class));
        swrveSpy.init(mActivity);

        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        displayNotification(builderSpy, bundle);

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
        SwrveNotificationTestUtils.assertEngagedEvent(events.get(0), "Swrve.Messages.Push-1.engaged");

        // Should not send an influence event when the app is opened (emulate app start/resume)
        swrveSpy.onResume(mActivity);
        // No new events should be queued
        events = SwrveTestUtils.getEventsQueued(mShadowActivity);
        assertEquals(1, events.size());
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Test
    public void testNotificationChannelFromConfig() {
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

        SwrveNotificationConfig notificationConfigWithChannel = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, com.swrve.sdk.test.R.drawable.ic_launcher, channel)
                .activityClass(MainActivity.class)
                .build();
        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfigWithChannel));
        displayNotification(builderSpy, bundle);

        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);

        assertEquals("swrve_channel", notification.getChannelId());
        // Check that the channel was created by our SDK
        assertNotNull(notificationManager.getNotificationChannel("swrve_channel"));
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Test
    public void testNotificationChannelIdFromPayload() {

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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        displayNotification(builderSpy, bundle);

        assertNumberOfNotifications(1);

        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);

        assertEquals(channelId, notification.getChannelId());
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Test
    public void testNotificationChannelFromPayload() {

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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        displayNotification(builderSpy, bundle);

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

    @Test
    public void testGetImageFromUrl() throws Exception {

        String image1Url = "https://someimage.com/image_blah";
        String image1FileName = "fc5d3fd9bc7b8bc9960d91851da9dc48";
        File cacheDir = SwrveCommon.getInstance().getCacheDir(RuntimeEnvironment.application);

        // save a file so bitmap is returned
        File file = new File(cacheDir, image1FileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("");
        writer.close();

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        doReturn(null).when(builderSpy).downloadBitmapImageFromUrl(anyString());

        Bitmap bitmap = builderSpy.getImageFromUrl(image1Url);

        assertNotNull(bitmap);
        Mockito.verify(builderSpy, Mockito.times(1)).getImageFromCache(image1Url);
    }

    @Test
    public void testGetImageFromUrl_notInCache() {

        String image1Url = "https://someimage.com/image_blah";
        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(RuntimeEnvironment.application, notificationConfig));
        doReturn(null).when(builderSpy).downloadBitmapImageFromUrl(anyString());

        Bitmap bitmap = builderSpy.getImageFromUrl(image1Url);

        assertNull(bitmap);
        Mockito.verify(builderSpy, Mockito.times(1)).getImageFromCache(image1Url);
    }

    // HELPER METHODS

    private int generateTimestampId() {
        return (int) (new Date().getTime() % Integer.MAX_VALUE);
    }

    private Notification assertNotification(String tickerText, String sound, Bundle extras) {
        return SwrveNotificationTestUtils.assertNotification(tickerText, sound, extras);
    }

    private void assertNumberOfNotifications(int expectedNumberOfNotifications) {
        SwrveNotificationTestUtils.assertNumberOfNotifications(expectedNumberOfNotifications);
    }

    private Intent getIntent(PendingIntent pendingIntent) {
        return ((ShadowPendingIntent) Shadow.extract(pendingIntent)).getSavedIntent();
    }
}
