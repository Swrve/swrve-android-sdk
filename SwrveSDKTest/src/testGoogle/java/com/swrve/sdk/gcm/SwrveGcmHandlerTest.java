package com.swrve.sdk.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.SwrveTestUtils;
import com.swrve.sdk.qa.SwrveQAUser;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowPendingIntent;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;


public class SwrveGcmHandlerTest extends SwrveBaseTest {

    private SwrveGcmIntentService swrveGcmService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveGcmService = new SwrveGcmIntentService();
        swrveGcmService.onCreate();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testOnHandleIntentService() throws Exception {
        TestableSwrveGcmHandler handler = new TestableSwrveGcmHandler(RuntimeEnvironment.application, swrveGcmService);
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
        TestableSwrveGcmHandler handler = new TestableSwrveGcmHandler(RuntimeEnvironment.application, swrveGcmService);

        Bundle nullBundle = null;
        boolean gcmHandled = handler.onMessageReceived("1234", nullBundle);
        assertFalse(gcmHandled);
        assertEquals(0, handler.isSwrveRemoteNotificationExecuted);

        Bundle missingTrackingKey = new Bundle();
        missingTrackingKey.putString("text", "");
        gcmHandled = handler.onMessageReceived("1234", missingTrackingKey);
        assertFalse(gcmHandled);
        assertEquals(2, handler.isSwrveRemoteNotificationExecuted);

        Bundle validBundle = new Bundle();
        validBundle.putString("text", "validBundle");
        validBundle.putString("sound", "default");
        validBundle.putString("customData", "some custom values");
        validBundle.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1");
        gcmHandled = handler.onMessageReceived("1234", validBundle);
        assertTrue(gcmHandled);
        assertEquals(1, handler.isSwrveRemoteNotificationExecuted);
        assertNotification("validBundle", "content://settings/system/notification_sound", validBundle);
    }

    @Test
    public void testOnMessageReceivedCustomSound() throws Exception {
        TestableSwrveGcmHandler handler = new TestableSwrveGcmHandler(RuntimeEnvironment.application, swrveGcmService);
        Bundle validBundleCustomSound = new Bundle();
        validBundleCustomSound.putString("text", "validBundleCustomSound");
        validBundleCustomSound.putString("sound", "customSound");
        validBundleCustomSound.putString("customData", "some custom values");
        validBundleCustomSound.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1");
        boolean gcmHandled = handler.onMessageReceived("1234", validBundleCustomSound);
        assertTrue(gcmHandled);
        assertEquals(1, handler.isSwrveRemoteNotificationExecuted);
        assertNotification("validBundleCustomSound", "android.resource://com.swrve.sdk.test/raw/customSound", validBundleCustomSound);
    }

    @Test
    public void testOnMessageReceivedWithQaUser() throws Exception {
        SwrveBase swrve = (SwrveBase)SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        JSONObject jsonObject = new JSONObject("{\"campaigns\":[],\"logging\":true,\"reset_device_state\":true,\"logging_url\":\"http:\\/\\/1031.qa-log.swrve.com\"}");
        SwrveQAUser swrveQAUser = new SwrveQAUser(swrve, jsonObject);
        swrveQAUser.bindToServices();

        TestableSwrveGcmHandler handler = new TestableSwrveGcmHandler(RuntimeEnvironment.application, swrveGcmService);
        Bundle validBundle = new Bundle();
        validBundle.putString("text", "hello there");
        validBundle.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1");
        boolean gcmHandled = handler.onMessageReceived("1234", validBundle);
        assertTrue(gcmHandled);
        assertNotification("hello there", null, validBundle);
    }

    @Test
    public void testWithDeeplink() throws Exception {
        TestableSwrveGcmHandler handler = new TestableSwrveGcmHandler(RuntimeEnvironment.application, swrveGcmService);
        swrveGcmService.handler = handler;
        Bundle deeplinkBundle = new Bundle();
        deeplinkBundle.putString("text", "deeplinkBundle");
        deeplinkBundle.putString(SwrveGcmConstants.DEEPLINK_KEY, "swrve://deeplink/config");
        deeplinkBundle.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1");
        boolean gcmHandled = handler.onMessageReceived("1234", deeplinkBundle);
        assertTrue(gcmHandled);
        assertEquals(1, handler.isSwrveRemoteNotificationExecuted);
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
        assertTrue(shadowPendingIntent.isBroadcastIntent());
        assertEquals(1, shadowPendingIntent.getSavedIntents().length);
        Intent intent = shadowPendingIntent.getSavedIntents()[0];
        assertEquals("com.swrve.sdk.SwrvePushEngageReceiver", intent.getComponent().getClassName());
        Bundle intentExtras = intent.getBundleExtra(SwrveGcmConstants.GCM_BUNDLE);
        assertNotNull(intentExtras);
        for (String key : extras.keySet()) {
            assertTrue(intentExtras.containsKey(key));
            assertEquals(extras.get(key), intentExtras.getString(key));
        }
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("Android Test App", shadowNotification.getContentTitle());
    }

    class TestableSwrveGcmHandler extends SwrveGcmHandler {

        int isSwrveRemoteNotificationExecuted = 0;

        TestableSwrveGcmHandler(Context context, SwrveGcmIntentService swrveGcmService) {
            super(context, swrveGcmService);
        }

        @Override
        protected boolean isSwrveRemoteNotification(final Bundle msg) {
            boolean isSwrveRemoteNotification = super.isSwrveRemoteNotification(msg);
            isSwrveRemoteNotificationExecuted = isSwrveRemoteNotification ? 1 : 2;
            return isSwrveRemoteNotification;
        }
    }
}
