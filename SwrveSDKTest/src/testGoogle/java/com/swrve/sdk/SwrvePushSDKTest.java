package com.swrve.sdk;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.swrve.sdk.qa.SwrveQAUser;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowPendingIntent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

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

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
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

        //Assert there are 18 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 2);

        //Now make use of the ability to configure the duplication cache size
        extras.putInt(SwrvePushConstants.PUSH_ID_CACHE_SIZE_KEY, 1);
        intent.putExtras(extras);
        service.onHandleIntent(intent);

        //Assert there are 18 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 2);

        //Send again to make sure dedupe is happening with newest item.
        service.onHandleIntent(intent);

        //Assert there are 18 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 2);

        //Change the key to new timestamp
        extras.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(newTimestamp));
        intent.putExtras(extras);
        service.onHandleIntent(intent);

        //Assert there are 19 notifications
        assertNumberOfNotifications(DEFAULT_PUSH_ID_CACHE_SIZE + 3);

        //Use the previous timestamp - but now expect an extra notification since the cache is only 1.
        extras.putString(SwrvePushConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));
        intent.putExtras(extras);
        service.onHandleIntent(intent);

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
        SwrveBase swrve = (SwrveBase)SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        JSONObject jsonObject = new JSONObject("{\"campaigns\":[],\"logging\":true,\"reset_device_state\":true,\"logging_url\":\"http:\\/\\/1031.qa-log.swrve.com\"}");
        SwrveQAUser swrveQAUser = new SwrveQAUser(swrve, jsonObject);
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
        Swrve swrveSpy = Mockito.spy(swrveReal);

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
        Bundle intentExtras = intent.getBundleExtra(SwrvePushConstants.PUSH_BUNDLE);
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

}
