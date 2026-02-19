package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import com.swrve.sdk.notifications.model.SwrveNotificationButton;
import com.swrve.sdk.test.MainActivity;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowPendingIntent;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class SwrveNotificationBuilderTest extends SwrveBaseTest {

    private SwrvePushManagerImp pushServiceManagerSpy;
    private SwrveNotificationConfig notificationConfig = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, null)
            .activityClass(MainActivity.class)
            .build();
    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        shadowApplication.grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.disableSwrveBackgroundEventSender(swrveSpy);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        doNothing().when(swrveSpy).checkForCampaignAndResourcesUpdates();
        doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
        SwrveCommon.setSwrveCommon(swrveSpy);
        doReturn(notificationConfig).when(swrveSpy).getNotificationConfig();
        doReturn(ApplicationProvider.getApplicationContext().getCacheDir()).when(swrveSpy).getCacheDir(ApplicationProvider.getApplicationContext());
        pushServiceManagerSpy = spy(new SwrvePushManagerImp(ApplicationProvider.getApplicationContext()));
        doReturn(mock(CampaignDeliveryManager.class)).when(pushServiceManagerSpy).getCampaignDeliveryManager();
    }

    @Test
    public void testServiceCustomSound() {
        Bundle validBundleCustomSound = new Bundle();
        validBundleCustomSound.putString(SwrveNotificationConstants.TEXT_KEY, "validBundleCustomSound");
        validBundleCustomSound.putString("sound", "customSound");
        validBundleCustomSound.putString("customData", "some custom values");
        validBundleCustomSound.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        validBundleCustomSound.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        pushServiceManagerSpy.processMessage(validBundleCustomSound);
        assertNotification("validBundleCustomSound", "android.resource://com.swrve.sdk.test/raw/customSound", validBundleCustomSound);
    }

    @Test
    public void testWithDeeplink() {
        Bundle deeplinkBundle = new Bundle();
        deeplinkBundle.putString(SwrveNotificationConstants.TEXT_KEY, "deeplinkBundle");
        deeplinkBundle.putString(SwrveNotificationConstants.DEEPLINK_KEY, "swrve://deeplink/config");
        deeplinkBundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        deeplinkBundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        pushServiceManagerSpy.processMessage(deeplinkBundle);
        assertNotification("deeplinkBundle", null, deeplinkBundle);
    }

    @Config(sdk = Build.VERSION_CODES.R)
    @Test
    public void testWithDeeplink_api30() {
        Bundle deeplinkBundle = new Bundle();
        deeplinkBundle.putString(SwrveNotificationConstants.TEXT_KEY, "deeplinkBundle");
        deeplinkBundle.putString(SwrveNotificationConstants.DEEPLINK_KEY, "swrve://deeplink/config");
        deeplinkBundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        deeplinkBundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        pushServiceManagerSpy.processMessage(deeplinkBundle);
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
        pushServiceManagerSpy.processMessage(bundle);

        assertNotification("text", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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

        pushServiceManagerSpy.processMessage(bundle);

        assertNotification("text", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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

        pushServiceManagerSpy.processMessage(bundle);

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

        pushServiceManagerSpy.processMessage(bundle);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        mockAllImageDownloads(builderSpy);
        displayNotification(builderSpy, bundle);

        assertNumberOfNotifications(1);
        assertNotification("original push notification - testAdvancedPushLockScreenMessage", "content://settings/system/notification_sound", bundle);


        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        mockAllImageDownloads(builderSpy);
        displayNotification(builderSpy, bundle);

        assertNotification("original push notification - testAdvancedPushLockScreenMessageNoMedia", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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
                "                \"icon_url\": \"https://valid-image.png\"},\n" +
                " \"notification_id\": 123" +
                "}\n";

        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        doReturn(bmp).when(builderSpy).getImageFromUrl("https://valid-image.png");
        NotificationMediaManager mediaManagerSpy = Mockito.spy(new NotificationMediaManager(mActivity));
        NotificationMediaManager.BigPictureFetchResult bigPictureFetchResult = new NotificationMediaManager.BigPictureFetchResult();
        bigPictureFetchResult.bitmap = bmp;
        doReturn(bigPictureFetchResult).when(mediaManagerSpy).downloadBigPictureImage("https://valid-image.png", 123);
        doReturn(null).when(mediaManagerSpy).downloadBigPictureImage("https://fail-image.png", 123);
        builderSpy.mediaManager = mediaManagerSpy;

        displayNotification(builderSpy, bundle);

        assertNotification("[rich body]", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("[rich body]", notification.tickerText);
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());
        assertNotNull(shadowNotification.getBigPicture());
    }

    @Test
    public void testAdvancedBigImagePushGif() throws Exception {

        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"fallback title\",\n" +
                " \"subtitle\": \"fallback subtitle\",\n" +
                " \"icon_url\": \"\",\n" +
                " \"version\": 1,\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n" +
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://valid-image.gif\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"\",\n" +
                "              \"fallback_sd\": \"\"\n }," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\",\n" +
                "                \"icon_url\": \"\"},\n" +
                " \"notification_id\": 123" +
                "}\n";

        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        doReturn(bmp).when(builderSpy).getImageFromUrl("https://valid-image.png");
        NotificationMediaManager mediaManagerSpy = Mockito.spy(new NotificationMediaManager(mActivity));
        NotificationMediaManager.BigPictureFetchResult bigPictureFetchResult = new NotificationMediaManager.BigPictureFetchResult();
        bigPictureFetchResult.mediaUri = Uri.parse("content://media/external/downloads/123");
        doReturn(bigPictureFetchResult).when(mediaManagerSpy).downloadBigPictureImage("https://valid-image.gif", 123);
        builderSpy.mediaManager = mediaManagerSpy;

        displayNotification(builderSpy, bundle);

        assertNotification("[rich body]", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("[rich body]", notification.tickerText);
        assertEquals("[expanded title]", shadowNotification.getBigContentTitle());
        assertNull(shadowNotification.getBigPicture()); // bitmap should be null....there's no icon api to check

        SwrveNotificationDetails notificationDetails = builderSpy.getNotificationDetails();
        assertEquals("[rich body]", notificationDetails.getBody());
        assertEquals("[expanded body]", notificationDetails.getExpandedBody());
        assertEquals("[expanded title]", notificationDetails.getExpandedTitle());
        assertNull(notificationDetails.getMediaBitmap());
        assertEquals("content://media/external/downloads/123", notificationDetails.getMediaContentUri());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Config(sdk = Build.VERSION_CODES.M)
    @Test
    public void testAdvancedBigImagePushVideoFallback() {

        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{" +
                "\"title\":\"fallback title\"," +
                "\"subtitle\":\"fallback subtitle\"," +
                "\"icon_url\":\"https://valid-image.png\"," +
                "\"version\":1," +
                "\"media\":{" +
                    "\"title\":\"[rich title]\"," +
                    "\"subtitle\":\"[rich subtitle]\"," +
                    "\"body\":\"[rich body]\"," +
                    "\"type\":\"image\"," +
                    "\"url\":\"https://fail-image.png\"," +
                    "\"fallback_type\":\"image\"," +
                    "\"fallback_url\":\"https://valid-image.png\"," +
                    "\"fallback_sd\":\"https://fallback_sd\"" +
                "}," +
                "\"expanded\":{" +
                    "\"title\":\"[expanded title]\"," +
                    "\"body\":\"[expanded body]\"," +
                    "\"icon_url\":\"https://valid-image.png\"" +
                "}," +
                "\"notification_id\":123" +
                "}";

        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        doReturn(null).when(builderSpy).getImageFromUrl(anyString());
        doReturn(null).when(builderSpy).getImageFromUrl("https://fail-image.png");
        doReturn(bmp).when(builderSpy).getImageFromUrl("https://valid-image.png");
        NotificationMediaManager mediaManagerSpy = Mockito.spy(new NotificationMediaManager(mActivity));
        NotificationMediaManager.BigPictureFetchResult bigPictureFetchResult = new NotificationMediaManager.BigPictureFetchResult();
        bigPictureFetchResult.bitmap = bmp;
        doReturn(bigPictureFetchResult).when(mediaManagerSpy).downloadBigPictureImage("https://valid-image.png", 123);
        doReturn(null).when(mediaManagerSpy).downloadBigPictureImage("https://media.jpg", 123);
        doReturn(null).when(mediaManagerSpy).downloadBigPictureImage("https://fail-image.png", 123);
        builderSpy.mediaManager = mediaManagerSpy;

        displayNotification(builderSpy, bundle);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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
        assertTrue(shadowPendingIntent.isActivity());
        assertEquals(1, shadowPendingIntent.getSavedIntents().length);
        Intent shadowIntent = shadowPendingIntent.getSavedIntents()[0];
        assertEquals("com.swrve.sdk.SwrveNotificationEngageActivity", shadowIntent.getComponent().getClassName());
        Bundle intentExtras = shadowIntent.getBundleExtra(SwrveNotificationConstants.PUSH_BUNDLE);
        assertEquals("https://fallback_sd", intentExtras.get(SwrveNotificationConstants.DEEPLINK_KEY));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        doReturn(null).when(builderSpy).getImageFromUrl(anyString());
        doReturn(null).when(builderSpy).getImageFromUrl("https://fail-image.png");
        doReturn(bmp).when(builderSpy).getImageFromUrl("https://valid-image.png");

        displayNotification(builderSpy, bundle);

        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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
        assertTrue(shadowPendingIntent.isActivity());
        assertEquals(1, shadowPendingIntent.getSavedIntents().length);
        Intent shadowIntent = shadowPendingIntent.getSavedIntents()[0];
        assertEquals("com.swrve.sdk.SwrveNotificationEngageActivity", shadowIntent.getComponent().getClassName());
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        when(builderSpy.getImageFromUrl(anyString())).thenReturn(null);
        when(builderSpy.getImageFromUrl("https://fail-image.png")).thenReturn(null);

        displayNotification(builderSpy, bundle);

        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals("fallback body", notification.tickerText);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("fallback title", shadowNotification.getContentTitle());

        assertNull(shadowNotification.getBigPicture());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Config(sdk = Build.VERSION_CODES.M)
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        displayNotification(builderSpy, bundle);

        assertNotification(SwrveNotificationConstants.TEXT_KEY, "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Config(sdk = Build.VERSION_CODES.M)
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(200, 300, conf);
        doReturn(null).when(builderSpy).getImageFromUrl(anyString());
        doReturn(bmp).when(builderSpy).getImageFromUrl("https://valid-image.png");
        NotificationMediaManager mediaManagerSpy = Mockito.spy(new NotificationMediaManager(mActivity));
        NotificationMediaManager.BigPictureFetchResult bigPictureFetchResult = new NotificationMediaManager.BigPictureFetchResult();
        bigPictureFetchResult.bitmap = bmp;
        doReturn(bigPictureFetchResult).when(mediaManagerSpy).downloadBigPictureImage("https://valid-image.png", 222);
        doReturn(null).when(mediaManagerSpy).downloadBigPictureImage("https://fail-image.png", 222);
        builderSpy.mediaManager = mediaManagerSpy;

        displayNotification(builderSpy, bundle);

        assertNotification("[rich body]", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Config(sdk = Build.VERSION_CODES.M)
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        displayNotification(builderSpy, bundle);

        assertNotification("text", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        int requestCodeStart = builderSpy.requestCode;

        displayNotification(builderSpy, bundle);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();

        Notification notification = notifications.get(0);
        Notification.Action[] actions = notification.actions;
        assertEquals(requestCodeStart, shadowOf(actions[0].actionIntent).getRequestCode());
        assertEquals(requestCodeStart + 1, shadowOf(actions[1].actionIntent).getRequestCode());
        assertEquals(requestCodeStart + 2, shadowOf(actions[2].actionIntent).getRequestCode());
        assertEquals(requestCodeStart + 3, shadowOf(notification.contentIntent).getRequestCode());
        assertEquals(requestCodeStart + 4, builderSpy.requestCode); // final value of requestCode
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Config(sdk = Build.VERSION_CODES.M)
    @Test
    public void testButtonClickEvents() throws Exception {
        swrveSpy.init(mActivity);

        // Send a valid Rich Payload
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.TRACKING_DATA_KEY, "5ea0fb1b8a24b8f9f76f675b7350200f314312fa");
        bundle.putString(SwrveNotificationConstants.PLATFORM_KEY, "android");
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        displayNotification(builderSpy, bundle);

        Notification notification = assertNotification("text", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        Notification.Action[] actions = notification.actions;
        assertEquals(actions.length, 3);
        Notification.Action testAction = actions[2];
        assertEquals(testAction.title, "[button text 3]");

        Intent buttonClickIntent = getIntent(testAction.actionIntent);
        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);
        notificationEngage.processIntent(buttonClickIntent);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> arrayListCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, Mockito.atLeast(2)).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), arrayListCaptor.capture());

        // Should send an engagement event
        ArrayList engagementEvents = (ArrayList) arrayListCaptor.getAllValues().get(0);

        Map<String, String> expectedEngagedPayload  = new HashMap<>();
        expectedEngagedPayload.put("trackingData","5ea0fb1b8a24b8f9f76f675b7350200f314312fa");
        expectedEngagedPayload.put("platform","android");

        SwrveNotificationTestUtils.assertEngagedEvent((String) engagementEvents.get(0), "Swrve.Messages.Push-1.engaged", expectedEngagedPayload);

        // Should send a button click event
        ArrayList buttonClickEvents= (ArrayList) arrayListCaptor.getAllValues().get(1);

        Map<String, String> expectedButtonPayload  = new HashMap<>();
        expectedButtonPayload.put("buttonText","[button text 3]");
        expectedButtonPayload.put("trackingData","5ea0fb1b8a24b8f9f76f675b7350200f314312fa");
        expectedButtonPayload.put("platform","android");

        SwrveTestUtils.assertGenericEvent((String)buttonClickEvents.get(0), "2", GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK, expectedButtonPayload);

        reset(swrveSpy); // reset so verify sendEventsInBackground is not called when resumed

        // Should not send an influence event when the app is opened (emulate app start/resume)
        swrveSpy.onResume(mActivity);
        // No new events should be queued
        verify(swrveSpy, never()).sendEventsInBackground(any(Context.class), anyString(), any(ArrayList.class));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Config(sdk = Build.VERSION_CODES.M)
    @Test
    public void testEngagedEvents() throws Exception {
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        displayNotification(builderSpy, bundle);

        Notification notification = assertNotification("body", "content://settings/system/notification_sound", bundle);
        assertNumberOfNotifications(1);

        notification.contentIntent.send();

        // Launch SwrveNotificationEngage (imitate single engagement with notification)
        Intent engageEventIntent = mShadowActivity.getNextStartedActivity();
        SwrveNotificationEngage notificationEngage = new SwrveNotificationEngage(mActivity);

        notificationEngage.processIntent(engageEventIntent);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> arrayListCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, Mockito.atLeast(1)).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), arrayListCaptor.capture());

        // Should send an engagement event
        ArrayList engagementEvents = (ArrayList) arrayListCaptor.getAllValues().get(0);
        SwrveNotificationTestUtils.assertEngagedEvent((String) engagementEvents.get(0), "Swrve.Messages.Push-1.engaged", null);

        reset(swrveSpy); // reset so verify sendEventsInBackground is not called when resumed

        // Should not send an influence event when the app is opened (emulate app start/resume)
        swrveSpy.onResume(mActivity);
        // No new events should be queued
        verify(swrveSpy, never()).sendEventsInBackground(any(Context.class), anyString(), any(ArrayList.class));
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public void testNotificationChannelFromConfig() {
        NotificationChannel channel = new NotificationChannel("swrve_channel", "Swrve channel", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("All the news from Swrve");
        ISwrveCommon mockSwrveCommon = mock(ISwrveCommon.class);
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

        SwrveNotificationConfig notificationConfigWithChannel = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, channel)
                .activityClass(MainActivity.class)
                .build();
        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfigWithChannel));
        displayNotification(builderSpy, bundle);

        assertNumberOfNotifications(1);

        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);

        assertEquals("swrve_channel", notification.getChannelId());
        // Check that the channel was created by our SDK
        assertNotNull(notificationManager.getNotificationChannel("swrve_channel"));
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public void testNotificationChannelIdFromPayload() {

        String channelId = "my_channel_id";
        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        displayNotification(builderSpy, bundle);

        assertNumberOfNotifications(1);

        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);

        assertEquals(channelId, notification.getChannelId());
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public void testNotificationChannelFromPayload() {

        ISwrveCommon mockSwrveCommon = mock(ISwrveCommon.class);
        SwrveCommon.setSwrveCommon(mockSwrveCommon);

        String channelId = "my_channel_id";
        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

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

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
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
    public void testGetPendingIntent() {
        SwrveNotificationBuilder builder = new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig);

        PendingIntent pendingIntent;
        ShadowPendingIntent shadowPendingIntent;
        Intent shadowIntent;

        // dismiss action == false --> use SwrveNotificationEngageActivity
        Intent intentApi = new Intent(mActivity, builder.getIntentClass(false));
        pendingIntent = builder.getPendingIntent(intentApi, PendingIntent.FLAG_CANCEL_CURRENT, false);
        shadowPendingIntent = shadowOf(pendingIntent);
        assertTrue(shadowPendingIntent.isActivity());
        shadowIntent = shadowPendingIntent.getSavedIntents()[0];
        assertEquals("com.swrve.sdk.SwrveNotificationEngageActivity", shadowIntent.getComponent().getClassName());

        // dismiss action == true --> use SwrveNotificationEngageReceiver
        Intent intentApiDismiss = new Intent(mActivity, builder.getIntentClass(true));
        pendingIntent = builder.getPendingIntent(intentApiDismiss, PendingIntent.FLAG_CANCEL_CURRENT, true);
        shadowPendingIntent = shadowOf(pendingIntent);
        assertTrue(shadowPendingIntent.isBroadcast());
        shadowIntent = shadowPendingIntent.getSavedIntents()[0];
        assertEquals("com.swrve.sdk.SwrveNotificationEngageReceiver", shadowIntent.getComponent().getClassName());
    }

    @Test
    public void testGetIntentClass() {
        SwrveNotificationBuilder builder = new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig);
        assertEquals("com.swrve.sdk.SwrveNotificationEngageReceiver", builder.getIntentClass(true).getName());
        assertEquals("com.swrve.sdk.SwrveNotificationEngageActivity", builder.getIntentClass(false).getName());
    }

    @Test
    public void testNotificationButtonIntentUseEngagementProxyTrue() {
        String pushId = "1";
        Intent intent = getNotificationButtonIntent(true, SwrveNotificationButton.ActionType.OPEN_APP, null, pushId, null);
        assertNotNull(intent);
        assertEquals("com.swrve.sdk.SwrveNotificationEngageActivity", intent.getComponent().getClassName());

        // use ActivityScenario to start the SwrveNotificationEngageActivity activity.
        ActivityScenario.launch(intent);
        await().until(pushEngagedSent(pushId, null));
        await().until(pushButtonClickSent(pushId, null));
        assertTrue(swrveSpy.processedSids.contains(pushId));
    }

    @Test
    public void testNotificationButtonIntentUseEngagementProxyFalseOpenApp() {
        String pushId = "2";
        Intent intent = getNotificationButtonIntent(false, SwrveNotificationButton.ActionType.OPEN_APP, null, pushId, null);
        assertNotNull(intent);
        assertEquals("com.swrve.sdk.test.MainActivity", intent.getComponent().getClassName());

        // use ActivityScenario to start the activity which triggers Application.LifecycleCallbacks.
        ActivityScenario.launch(intent);
        await().until(pushEngagedSent(pushId, null));
        await().until(pushButtonClickSent(pushId, null));
        assertTrue(swrveSpy.processedSids.contains(pushId));
    }

    @Test
    public void testNotificationButtonIntentUseEngagementProxyFalseOpenDeeplink() {
        String pushId = "3";
        String deeplink = "swrve://deeplink";
        Intent intent = getNotificationButtonIntent(false, SwrveNotificationButton.ActionType.OPEN_URL, deeplink, pushId, null);
        assertNotNull(intent);
        assertEquals("android.intent.action.VIEW", intent.getAction());
        assertEquals(deeplink, intent.getData().toString());

        // use ActivityScenario to start the activity which triggers Application.LifecycleCallbacks.
        ActivityScenario.launch(intent);
        await().until(pushEngagedSent(pushId, deeplink));
        await().until(pushButtonClickSent(pushId, deeplink));
        assertTrue(swrveSpy.processedSids.contains(pushId));
    }

    @Test
    public void testNotificationButtonIntentUseEngagementProxyFalseOpenExternalDeeplink() {
        // When deeplinks that cannot be handled by the app are clicked, the UseEngagementProxy setting might be overridden (if false) and SwrveNotificationEngageActivity should be launched.
        String pushId = "4";
        String deeplink = "https://www.swrve.com";
        Intent intent = getNotificationButtonIntent(false, SwrveNotificationButton.ActionType.OPEN_URL, deeplink, pushId, null);
        assertNotNull(intent);
        assertEquals("com.swrve.sdk.SwrveNotificationEngageActivity", intent.getComponent().getClassName());

        // use ActivityScenario to start the SwrveNotificationEngageActivity activity.
        ActivityScenario.launch(intent);
        await().until(pushEngagedSent(pushId, deeplink));
        await().until(pushButtonClickSent(pushId, deeplink));
        assertTrue(swrveSpy.processedSids.contains(pushId));
    }

    @Test
    public void testNotificationButtonIntentUseEngagementProxyFalseOpenAppFromNotificationIntentListener() {
        // Its possible with open app action, that external deeplink is opened instead via notification intent listener. When this scenario happens and the
        // UseEngagementProxy is false, then SwrveNotificationEngageActivity should be launched.
        // This is the case when the notification intent listener opens an external deeplink.
        String pushId = "5";
        SwrveNotificationIntentListener intentListener = (pushBundle, deeplink1) -> {
            return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://www.swrve.com")); // opening an external deeplink even though its supposed to be open app
        };
        Intent intent = getNotificationButtonIntent(false, SwrveNotificationButton.ActionType.OPEN_APP, null, pushId, intentListener);
        assertNotNull(intent);
        assertEquals("com.swrve.sdk.SwrveNotificationEngageActivity", intent.getComponent().getClassName());

        // use ActivityScenario to start the SwrveNotificationEngageActivity activity.
        ActivityScenario.launch(intent);
        await().until(pushEngagedSent(pushId, null));
        assertTrue(swrveSpy.processedSids.contains(pushId));
    }

    @Test
    public void testNotificationButtonIntentUseEngagementProxyFalseOpenCampaign() {
        String pushId = "6";
        Intent intent = getNotificationButtonIntent(false, SwrveNotificationButton.ActionType.OPEN_CAMPAIGN, null, pushId, null);
        assertNotNull(intent);
        assertEquals("com.swrve.sdk.test.MainActivity", intent.getComponent().getClassName()); // open campaign opens the MainActivity

        // use ActivityScenario to start the activity which triggers Application.LifecycleCallbacks.
        ActivityScenario.launch(intent);
        await().until(pushEngagedSent(pushId, null));
        await().until(pushButtonClickSent(pushId, null));
        assertTrue(swrveSpy.processedSids.contains(pushId));
    }

    private Intent getNotificationButtonIntent(boolean useEngagementProxy, SwrveNotificationButton.ActionType actionType, String actionUrl, String pushId, SwrveNotificationIntentListener intentListener) {

        SwrveNotificationConfig.Builder notificationConfigBuilder = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, null)
                .activityClass(MainActivity.class)
                .useEngagementProxy(useEngagementProxy);
        if (intentListener != null) {
            notificationConfigBuilder.notificationIntentListener(intentListener);
        }
        notificationConfig = notificationConfigBuilder.build();
        doReturn(notificationConfig).when(swrveSpy).getNotificationConfig();
        swrveSpy.config.setNotificationConfig(notificationConfig);

        // create a dummy builder and call the method directly that creates the a notification action
        SwrveNotificationBuilder builder = new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig);
        Bundle dummyBundle = dummyBundle(pushId, null);
        builder.build("dummy text", dummyBundle, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null);
        NotificationCompat.Action notificationAction = builder.createNotificationAction("buttonText", 0, "0", actionType, actionUrl);
        assertNotNull(notificationAction);
        return getIntent(notificationAction.getActionIntent());
    }

    @Test
    public void testNotificationMainIntentUseEngagementProxyTrue() {
        String pushId = "7";
        Intent intent = getNotificationMainIntent(true, pushId, null, null, null);
        assertNotNull(intent);
        assertEquals("com.swrve.sdk.SwrveNotificationEngageActivity", intent.getComponent().getClassName());

        // use ActivityScenario to start the SwrveNotificationEngageActivity activity.
        ActivityScenario.launch(intent);
        await().until(pushEngagedSent(pushId, null));
        assertTrue(swrveSpy.processedSids.contains(pushId));
    }

    @Test
    public void testNotificationMainIntentUseEngagementProxyFalseOpenApp() {
        String pushId = "8";
        Intent intent = getNotificationMainIntent(false, pushId, null, null, null);
        assertNotNull(intent);
        assertEquals("com.swrve.sdk.test.MainActivity", intent.getComponent().getClassName());

        // use ActivityScenario to start the activity which triggers Application.LifecycleCallbacks.
        ActivityScenario.launch(intent);
        await().until(pushEngagedSent(pushId, null));
        assertTrue(swrveSpy.processedSids.contains(pushId));
    }

    @Test
    public void testNotificationMainIntentUseEngagementProxyFalseOpenDeeplink() {
        String pushId = "9";
        String deeplink = "swrve://deeplink";
        Intent intent = getNotificationMainIntent(false, pushId, null, deeplink, null);
        assertNotNull(intent);
        assertEquals("android.intent.action.VIEW", intent.getAction());
        assertEquals(deeplink, intent.getData().toString());

        // use ActivityScenario to start the activity which triggers Application.LifecycleCallbacks.
        ActivityScenario.launch(intent);
        await().until(pushEngagedSent(pushId, deeplink));
        assertTrue(swrveSpy.processedSids.contains(pushId));
    }

    @Test
    public void testNotificationMainIntentUseEngagementProxyFalseOpenAppFromNotificationIntentListener() {
        // Its possible with open app action, that external deeplink is opened instead via notification intent listener. When this scenario happens and the
        // UseEngagementProxy is false, then SwrveNotificationEngageActivity should be launched.
        // This is the case when the notification intent listener opens an external deeplink.
        String pushId = "10";
        SwrveNotificationIntentListener intentListener = (pushBundle, deeplink1) -> {
            return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://www.swrve.com")); // opening an external deeplink even though its supposed to be open app
        };
        Intent intent = getNotificationMainIntent(false, pushId, null, null, intentListener);
        assertNotNull(intent);
        assertEquals("com.swrve.sdk.SwrveNotificationEngageActivity", intent.getComponent().getClassName());

        // use ActivityScenario to start the SwrveNotificationEngageActivity activity.
        ActivityScenario.launch(intent);
        await().until(pushEngagedSent(pushId, null));
        assertTrue(swrveSpy.processedSids.contains(pushId));
    }

    @Test
    public void testNotificationMainIntentUseEngagementProxyFalseOpenCampaign() {

        String pushId = "11";
        String campaignId = "1234";
        Intent intent = getNotificationMainIntent(false, pushId, campaignId, null, null);
        assertNotNull(intent);
        assertEquals("com.swrve.sdk.test.MainActivity", intent.getComponent().getClassName()); // open campaign opens the MainActivity

        // use ActivityScenario to start the activity which triggers Application.LifecycleCallbacks.
        ActivityScenario.launch(intent);
        await().until(pushEngagedSent(pushId, null));
        assertTrue(swrveSpy.processedSids.contains(pushId));
    }

    private Intent getNotificationMainIntent(boolean useEngagementProxy, String pushId, String iamCampaign, String deeplink, SwrveNotificationIntentListener intentListener) {

        SwrveNotificationConfig.Builder notificationConfigBuilder = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, null)
                .activityClass(MainActivity.class)
                .useEngagementProxy(useEngagementProxy);
        if (intentListener != null) {
            notificationConfigBuilder.notificationIntentListener(intentListener);
        }
        notificationConfig = notificationConfigBuilder.build();
        doReturn(notificationConfig).when(swrveSpy).getNotificationConfig();
        swrveSpy.config.setNotificationConfig(notificationConfig);

        // create a dummy builder and call the method directly that creates the main pending intent
        SwrveNotificationBuilder builder = new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig);
        Bundle dummyBundle = dummyBundle(pushId, iamCampaign);
        if (SwrveHelper.isNotNullOrEmpty(deeplink)) {
            dummyBundle.putString(SwrveNotificationConstants.DEEPLINK_KEY, deeplink);
        }
        builder.build("dummy text", dummyBundle, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null);
        PendingIntent pendingIntent = builder.createPendingIntent(dummyBundle, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null);
        assertNotNull(pendingIntent);
        return getIntent(pendingIntent);
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

    private Bundle dummyBundle(String pushId, String iamCampaign) {
        // A basic bundle
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, pushId);
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n";
        if (iamCampaign != null) {
            json += " \"campaign\": { \"id\": \"" + iamCampaign + "\" },\n";
        }
        json += " \"version\": 1\n" +
                "}\n";
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json);
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "should be rich");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = generateTimestampId();
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        bundle.putString(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY, pushId);
        return bundle;
    }

    private Callable<Boolean> pushEngagedSent(String pushId, String deeplink) {
        return () -> {
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<ArrayList> events = ArgumentCaptor.forClass(ArrayList.class);
            try {
                verify(swrveSpy, atLeastOnce()).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), events.capture());
            } catch (Throwable t) {
                return false; // Verification failed, try again
            }
            List<ArrayList> capturedProperties = events.getAllValues();
            String escapedDeeplink = JSONObject.quote(deeplink);
            escapedDeeplink = escapedDeeplink.substring(1, escapedDeeplink.length() - 1); // Remove quotes
            int found = 0;
            for (ArrayList event : capturedProperties) {
                String jsonString = event.get(0).toString();
                if (SwrveHelper.isNullOrEmpty(deeplink)) {
                    if (jsonString.contains("\"name\":\"Swrve.Messages.Push-" + pushId + ".engaged\"}")) {
                        //{"type":"event","time":1740138936022,"seqnum":1,"name":"Swrve.Messages.Push-2.engaged"}
                        found++;
                    }
                } else if (jsonString.contains("\"name\":\"Swrve.Messages.Push-" + pushId + ".engaged\",\"payload\":{\"deeplink\":\"" + escapedDeeplink + "\"}}")) {
                    //{"type":"event","time":1741962282622,"seqnum":1,"name":"Swrve.Messages.Push-7.engaged","payload":{"deeplink":"swrve://deeplink"}}
                    found++;
                }
            }
            return found == 1; // Only one push event should be found
        };
    }

    private Callable<Boolean> pushButtonClickSent(String pushId, String deeplink) {
        return () -> {
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<ArrayList> events = ArgumentCaptor.forClass(ArrayList.class);
            try {
                verify(swrveSpy, atLeastOnce()).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), events.capture());
            } catch (Throwable t) {
                return false; // Verification failed, try again
            }
            List<ArrayList> capturedProperties = events.getAllValues();
            String escapedDeeplink = JSONObject.quote(deeplink);
            escapedDeeplink = escapedDeeplink.substring(1, escapedDeeplink.length() - 1); // Remove quotes
            int found = 0;
            for (ArrayList event : capturedProperties) {
                String jsonString = event.get(0).toString();
                if (SwrveHelper.isNullOrEmpty(deeplink)) {
                    if (jsonString.contains("\"actionType\":\"button_click\",\"campaignType\":\"push\",\"contextId\":\"0\",\"id\":\"" + pushId + "\",\"payload\":{\"buttonText\":\"buttonText\"}}")) {
                        //{"type":"generic_campaign_event","time":1740138785306,"seqnum":2,"actionType":"button_click","campaignType":"push","contextId":"0","id":"2","payload":{"buttonText":"buttonText"}}
                        found++;
                    }
                } else if (jsonString.contains("\"actionType\":\"button_click\",\"campaignType\":\"push\",\"contextId\":\"0\",\"id\":\"" + pushId + "\",\"payload\":{\"buttonText\":\"buttonText\",\"deeplink\":\"" + escapedDeeplink + "\"}}")) {
                    //{"type":"generic_campaign_event","time":1741956519852,"seqnum":2,"actionType":"button_click","campaignType":"push","contextId":"0","id":"3","payload":{"buttonText":"buttonText","deeplink":"swrve:\/\/deeplink"}}
                    found++;
                }
            }
            return found == 1; // Only one push event should be found
        };
    }

}
