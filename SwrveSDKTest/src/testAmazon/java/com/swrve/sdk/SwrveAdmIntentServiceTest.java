package com.swrve.sdk;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.swrve.sdk.adm.SwrveAdmIntentService;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.IntentServiceController;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

public class SwrveAdmIntentServiceTest extends SwrveBaseTest {

    private SwrveAdmIntentService service;
    private TestableSwrvePushSDK swrvePushSDK;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SwrvePushSDK.createInstance(RuntimeEnvironment.application);
        swrvePushSDK = new TestableSwrvePushSDK(RuntimeEnvironment.application, true);
        setSwrvePushSDKInstance(swrvePushSDK);
        ShadowLog.stream = System.out;
        IntentServiceController<SwrveAdmIntentService> serviceController = IntentServiceController.of(Robolectric.getShadowsAdapter(), new SwrveAdmIntentService(), null);
        serviceController.create();
        service = serviceController.get();
    }

    @After
    public void tearDown() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testOnMessage() throws Exception {
        // Check null scenario
        service.onMessage(null);
        assertEquals(0, swrvePushSDK.processRemoteNotificationExecuted);

        // Check no payload scenario
        Intent intent = new Intent();
        Bundle missingTrackingKey = new Bundle();
        missingTrackingKey.putString("text", "");
        intent.putExtras(missingTrackingKey);
        service.onMessage(intent);
        assertEquals(1, swrvePushSDK.processRemoteNotificationExecuted);
    }

    class TestableSwrvePushSDK extends SwrvePushSDK {
        int processRemoteNotificationExecuted = 0;
        int processInfluenceDataCallCount = 0;
        public Date dateNow;

        public TestableSwrvePushSDK(Context context, boolean isPushEnabled) {
            super(context);
        }

        @Override
        public void processRemoteNotification(Bundle msg, boolean checkDupes) {
            super.processRemoteNotification(msg, checkDupes);
            processRemoteNotificationExecuted = 1;
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
        bundle.putString(SwrvePushConstants.TIMESTAMP_KEY, "12345");
        intent.putExtras(bundle);
        swrvePushSDK.dateNow = SwrveTestUtils.parseDate("2017/01/01 0:00");
        service.onMessage(intent);
        // Silent push 2
        bundle.putString(SwrvePushConstants.SWRVE_SILENT_TRACKING_KEY, "2");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrvePushConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value2\"}");
        intent.putExtras(bundle);
        swrvePushSDK.dateNow = SwrveTestUtils.parseDate("2017/01/01 10:00");
        service.onMessage(intent);
        // Silent push 3
        bundle.putString(SwrvePushConstants.SWRVE_SILENT_TRACKING_KEY, "3");
        bundle.putString(SwrvePushConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrvePushConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value3\"}");
        intent.putExtras(bundle);
        swrvePushSDK.dateNow = SwrveTestUtils.parseDate("2017/01/01 11:00");
        service.onMessage(intent);

        assertEquals(1, swrvePushSDK.processRemoteNotificationExecuted);
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

    private void assertNumberOfNotifications(int expectedNumberOfNotifications)  {
        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(expectedNumberOfNotifications, notifications.size());
    }
}
