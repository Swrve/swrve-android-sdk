package com.swrve.sdk.adm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowPendingIntent;

import com.google.common.base.Optional;
import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.SwrveTestUtils;
import com.swrve.sdk.qa.SwrveQAUser;
import com.swrve.sdk.test.BuildConfig;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
public class SwrveAdmIntentServiceTest {

    private TestableSwrveAdmIntentService swrveAdmService;
    private final int MAX_ID_CACHE_ITEMS = 16; //Insider knowledge

    @Before
    public void setUp() throws Exception {
        SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        ShadowLog.stream = System.out;
        swrveAdmService = new TestableSwrveAdmIntentService();
        swrveAdmService.onCreate();
    }

    @After
    public void tearDown() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    private int generateTimestampId() {
        return (int)(new Date().getTime() % Integer.MAX_VALUE);
    }

    @Test
    public void testOnMessageService() throws Exception {
        //Check null scenario
        swrveAdmService.onMessage(null);
        assertFalse(swrveAdmService.isSwrveRemoteNotificationReturn.isPresent());

        //Check no payload scenario
        Intent intent = new Intent();
        Bundle missingTrackingKey = new Bundle();
        missingTrackingKey.putString("text", "");
        intent.putExtras(missingTrackingKey);
        swrveAdmService.onMessage(intent);

        assertTrue(swrveAdmService.isSwrveRemoteNotificationReturn.isPresent());
        assertFalse(swrveAdmService.isSwrveRemoteNotificationReturn.get());

        //Check no timestamp scenario
        Bundle noTimestamp = new Bundle();
        noTimestamp.putString("text", "validBundle");
        noTimestamp.putString(SwrveAdmConstants.SWRVE_TRACKING_KEY, "1");
        intent.putExtras(noTimestamp);
        swrveAdmService.onMessage(intent);

        assertTrue(swrveAdmService.isSwrveRemoteNotificationReturn.isPresent());
        assertTrue(swrveAdmService.isSwrveRemoteNotificationReturn.get());
        assertNumberOfNotifications(0);

        //Check good scenario
        Bundle extras = new Bundle();
        extras.putString("text", "validBundle");
        extras.putString("customData", "some custom values");
        extras.putString("sound", "default");
        extras.putString(SwrveAdmConstants.SWRVE_TRACKING_KEY, "1");
        int firstTimestamp = generateTimestampId();
        extras.putString(SwrveAdmConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(extras);
        swrveAdmService.onMessage(intent);

        assertNotification("validBundle", "content://settings/system/notification_sound", extras);
        assertTrue(swrveAdmService.isSwrveRemoteNotificationReturn.get());
        assertNumberOfNotifications(1);

        //Try sending duplicate
        swrveAdmService.onMessage(intent);
        assertNumberOfNotifications(1);

        //Now send another 16 unique notifications to overfill the buffer
        int newTimestamp = generateTimestampId();
        for (int i=0; i<MAX_ID_CACHE_ITEMS; ++i) {
            extras.putString(SwrveAdmConstants.TIMESTAMP_KEY, Integer.toString(newTimestamp));
            intent.putExtras(extras);
            swrveAdmService.onMessage(intent);
            newTimestamp++;

            //Let the notification manager do its thing
            Thread.sleep(50);
        }

        //Assert there are 17 notifications
        assertNumberOfNotifications(MAX_ID_CACHE_ITEMS + 1);

        //Now we should be able to reuse the first notification timestamp
        extras.putString(SwrveAdmConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(extras);
        swrveAdmService.onMessage(intent);

        //Assert there are 18 notifications
        assertNumberOfNotifications(MAX_ID_CACHE_ITEMS + 2);
    }

    @Test
    public void testOnMessageReceivedCustomSound() throws Exception {
        Intent intent = new Intent();
        Bundle validBundleCustomSound = new Bundle();
        validBundleCustomSound.putString("text", "validBundleCustomSound");
        validBundleCustomSound.putString("sound", "customSound");
        validBundleCustomSound.putString("customData", "some custom values");
        validBundleCustomSound.putString(SwrveAdmConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        validBundleCustomSound.putString(SwrveAdmConstants.SWRVE_TRACKING_KEY, "1");
        intent.putExtras(validBundleCustomSound);
        swrveAdmService.onMessage(intent);
        assertTrue(swrveAdmService.isSwrveRemoteNotificationReturn.isPresent());
        assertTrue(swrveAdmService.isSwrveRemoteNotificationReturn.get());
        assertNotification("validBundleCustomSound", "android.resource://com.swrve.sdk.test/raw/customSound", validBundleCustomSound);
    }

    @Test
    public void testOnMessageReceivedWithQaUser() throws Exception {
        SwrveBase swrve = (SwrveBase)SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        JSONObject jsonObject = new JSONObject("{\"campaigns\":[],\"logging\":true,\"reset_device_state\":true,\"logging_url\":\"http:\\/\\/1031.qa-log.swrve.com\"}");
        SwrveQAUser swrveQAUser = new SwrveQAUser(swrve, jsonObject);
        swrveQAUser.bindToServices();

        Intent intent = new Intent();
        Bundle validBundle = new Bundle();
        validBundle.putString("text", "hello there");
        validBundle.putString(SwrveAdmConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        validBundle.putString(SwrveAdmConstants.SWRVE_TRACKING_KEY, "1");
        intent.putExtras(validBundle);
        swrveAdmService.onMessage(intent);
        assertTrue(swrveAdmService.isSwrveRemoteNotificationReturn.isPresent());
        assertTrue(swrveAdmService.isSwrveRemoteNotificationReturn.get());
        assertNotification("hello there", null, validBundle);
    }

    @Test
    public void testWithDeeplink() throws Exception {
        Intent intent = new Intent();
        Bundle deeplinkBundle = new Bundle();
        deeplinkBundle.putString("text", "deeplinkBundle");
        deeplinkBundle.putString(SwrveAdmConstants.DEEPLINK_KEY, "swrve://deeplink/config");
        deeplinkBundle.putString(SwrveAdmConstants.SWRVE_TRACKING_KEY, "1");
        deeplinkBundle.putString(SwrveAdmConstants.TIMESTAMP_KEY, Integer.toString(generateTimestampId()));
        intent.putExtras(deeplinkBundle);
        swrveAdmService.onMessage(intent);
        assertTrue(swrveAdmService.isSwrveRemoteNotificationReturn.isPresent());
        assertTrue(swrveAdmService.isSwrveRemoteNotificationReturn.get());
        assertNotification("deeplinkBundle", null, deeplinkBundle);
    }

    @Test
    public void testRegistrationError() throws Exception {
        swrveAdmService.onRegistrationError("Some error message, should not crash.");
    }

    @Test
    public void testUnregistered() throws Exception {
        swrveAdmService.onUnregistered("Some text relating to being unregistered. Should not crash.");
    }

    @Test
    public void testRegistered() throws Exception {
        swrveAdmService.onRegistered("MyFakeADMToken");
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
        Bundle intentExtras = intent.getBundleExtra(SwrveAdmConstants.ADM_BUNDLE);
        assertNotNull(intentExtras);
        for (String key : extras.keySet()) {
            assertTrue(intentExtras.containsKey(key));
            assertEquals(extras.get(key), intentExtras.getString(key));
        }
        ShadowNotification shadowNotification = shadowOf(notification);
        assertEquals("Android Test App", shadowNotification.getContentTitle());
    }

    private void assertNumberOfNotifications(int expectedNumberOfNotifications)  {
        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(expectedNumberOfNotifications, notifications.size());
    }

    class TestableSwrveAdmIntentService extends SwrveAdmIntentService {

        Optional<Boolean> isSwrveRemoteNotificationReturn = Optional.absent();

        @Override
        protected boolean isSwrveRemoteNotification(final Bundle msg) {
            isSwrveRemoteNotificationReturn = Optional.of(super.isSwrveRemoteNotification(msg));
            return isSwrveRemoteNotificationReturn.get();
        }
    }
}

