package com.swrve.sdk.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.common.base.Optional;
import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.qa.SwrveQAUser;
import com.swrve.sdk.test.BuildConfig;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowPendingIntent;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
public class SwrveGcmHandlerTest {

    private SwrveGcmIntentService swrveGcmService;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        swrveGcmService = new SwrveGcmIntentService();
        swrveGcmService.onCreate();
    }

    @Test
    public void testOnHandleIntentService() throws Exception {
        SwrveGcmHandler handler = new SwrveGcmHandler(RuntimeEnvironment.application, swrveGcmService);
        Bundle validBundle = new Bundle();
        validBundle.putString("text", "validBundle");
        validBundle.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1");
        Intent intent = new Intent();
        intent.putExtras(validBundle);
        handler.onHandleIntent(intent, null);
        assertNotification("validBundle", null, validBundle);
    }

    @Test
    public void testOnMessageReceivedService() throws Exception {
        Bundle validBundle = new Bundle();
        validBundle.putString("text", "validBundle");
        validBundle.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1");
        swrveGcmService.onMessageReceived("1234", validBundle);
        assertNotification("validBundle", null, validBundle);
    }

    @Test
    public void testOnMessageReceivedHandler() throws Exception {
        SwrveGcmHandler handler = new SwrveGcmHandler(RuntimeEnvironment.application, swrveGcmService);

        Bundle nullBundle = null;
        boolean gcmHandled = handler.onMessageReceived("1234", nullBundle);
        assertFalse(gcmHandled);

        Bundle missingTrackingKey = new Bundle();
        missingTrackingKey.putString("text", "");
        gcmHandled = handler.onMessageReceived("1234", missingTrackingKey);
        assertFalse(gcmHandled);

        Bundle validBundle = new Bundle();
        validBundle.putString("text", "validBundle");
        validBundle.putString("sound", "default");
        validBundle.putString("customData", "some custom values");
        validBundle.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1");
        gcmHandled = handler.onMessageReceived("1234", validBundle);
        assertTrue(gcmHandled);
        assertNotification("validBundle", "content://settings/system/notification_sound", validBundle);
    }

    @Test
    public void testOnMessageReceivedHandlerSilent() throws Exception {
        SwrveGcmHandler handler = new SwrveGcmHandler(RuntimeEnvironment.application, swrveGcmService);

        Bundle validBundle = new Bundle();
        validBundle.putString("customData", "some custom values");
        validBundle.putString(SwrveGcmConstants.SWRVE_SILENT_TRACKING_KEY, "1");
        boolean gcmHandled = handler.onMessageReceived("1234", validBundle);
        assertTrue(gcmHandled);
        assertNoNotification();
    }

    @Test
    public void testOnMessageReceivedCustomSound() throws Exception {
        SwrveGcmHandler handler = new SwrveGcmHandler(RuntimeEnvironment.application, swrveGcmService);
        Bundle validBundleCustomSound = new Bundle();
        validBundleCustomSound.putString("text", "validBundleCustomSound");
        validBundleCustomSound.putString("sound", "customSound");
        validBundleCustomSound.putString("customData", "some custom values");
        validBundleCustomSound.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1");
        boolean gcmHandled = handler.onMessageReceived("1234", validBundleCustomSound);
        assertTrue(gcmHandled);
        assertNotification("validBundleCustomSound", "android.resource://com.swrve.sdk.test/raw/customSound", validBundleCustomSound);
    }

    @Test
    public void testOnMessageReceivedWithQaUser() throws Exception {
        SwrveBase swrve = (SwrveBase)SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        JSONObject jsonObject = new JSONObject("{\"campaigns\":[],\"logging\":true,\"reset_device_state\":true,\"logging_url\":\"http:\\/\\/1031.qa-log.swrve.com\"}");
        SwrveQAUser swrveQAUser = new SwrveQAUser(swrve, jsonObject);
        swrveQAUser.bindToServices();

        SwrveGcmHandler handler = new SwrveGcmHandler(RuntimeEnvironment.application, swrveGcmService);
        Bundle validBundle = new Bundle();
        validBundle.putString("text", "hello there");
        validBundle.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1");
        boolean gcmHandled = handler.onMessageReceived("1234", validBundle);
        assertTrue(gcmHandled);
        assertNotification("hello there", null, validBundle);
    }

    @Test
    public void testWithDeeplink() throws Exception {
        SwrveGcmHandler handler = new SwrveGcmHandler(RuntimeEnvironment.application, swrveGcmService);
        swrveGcmService.handler = handler;
        Bundle deeplinkBundle = new Bundle();
        deeplinkBundle.putString("text", "deeplinkBundle");
        deeplinkBundle.putString(SwrveGcmConstants.DEEPLINK_KEY, "swrve://deeplink/config");
        deeplinkBundle.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1");
        boolean gcmHandled = handler.onMessageReceived("1234", deeplinkBundle);
        assertTrue(gcmHandled);
        assertNotification("deeplinkBundle", null, deeplinkBundle);
    }

    private void assertNotification(String tickerText, String sound, Bundle extras)  {
        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(tickerText, notification.tickerText);
        assertEquals(sound, notification.sound == null ? null : notification.sound.toString());
        PendingIntent pendingIntent = notification.contentIntent;
        ShadowPendingIntent shadowPendingIntent = shadowOf(pendingIntent);
        assertNotNull(shadowPendingIntent);
        assertTrue(shadowPendingIntent.isActivityIntent());
        assertEquals(1, shadowPendingIntent.getSavedIntents().length);
        Intent intent = shadowPendingIntent.getSavedIntents()[0];
        assertEquals("openActivity", intent.getAction());
        assertEquals("com.swrve.sdk.test.MainActivity", intent.getComponent().getClassName());
        Bundle intentExtras = intent.getBundleExtra(SwrveGcmConstants.GCM_BUNDLE);
        assertNotNull(intentExtras);
        for (String key : extras.keySet()) {
            assertTrue(intentExtras.containsKey(key));
            assertEquals(extras.get(key), intentExtras.getString(key));
        }
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("Android Test App", shadowNotification.getContentTitle());
    }

    private void assertNoNotification() {
        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(0, notifications.size());
    }
}
