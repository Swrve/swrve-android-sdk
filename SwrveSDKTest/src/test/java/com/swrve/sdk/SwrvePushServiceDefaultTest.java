package com.swrve.sdk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.swrve.sdk.test.MainActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class SwrvePushServiceDefaultTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ISwrveCommon swrveCommonSpy = mock(ISwrveCommon.class);
        SwrveCommon.setSwrveCommon(swrveCommonSpy);
        doReturn("testUserId").when(swrveCommonSpy).getUserId();
        SwrveNotificationConfig notificationConfig = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, com.swrve.sdk.test.R.drawable.ic_launcher, null)
                .activityClass(MainActivity.class)
                .build();
        doReturn(notificationConfig).when(swrveCommonSpy).getNotificationConfig();
    }

    @Test
    public void testHandleInvalidExtras() {
        Bundle extras = null;
        boolean success = SwrvePushServiceDefault.handle(RuntimeEnvironment.application, extras);
        assertFalse(success);

        extras = new Bundle();
        success = SwrvePushServiceDefault.handle(RuntimeEnvironment.application, extras);
        assertFalse(success);
    }

    @Test
    public void testHandleIntent() {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        intent.putExtras(extras);
        boolean success = SwrvePushServiceDefault.handle(RuntimeEnvironment.application, intent);
        assertTrue(success);
    }

    @Test
    public void testHandleMap() {
        Map<String, String> data = new HashMap<>();
        data.put(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        boolean success = SwrvePushServiceDefault.handle(RuntimeEnvironment.application, data);
        assertTrue(success);
    }

    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void testHandleLollipop() {
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        boolean success = SwrvePushServiceDefault.handle(RuntimeEnvironment.application, extras);
        assertTrue(success);

        List<Intent> intents = shadowApplication.getBroadcastIntents();
        assertNotNull(intents);
        assertEquals(1, intents.size());
        assertEquals("com.swrve.sdk.SwrvePushServiceDefaultReceiver", intents.get(0).getComponent().getClassName());
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @Test
    public void testHandleOreo() {
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        boolean success = SwrvePushServiceDefault.handle(RuntimeEnvironment.application, extras);
        assertTrue(success);

        JobScheduler jobScheduler = (JobScheduler) RuntimeEnvironment.application.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        List<JobInfo> jobs = jobScheduler.getAllPendingJobs();
        assertNotNull(jobs);
        assertEquals(1, jobs.size());
        assertEquals("com.swrve.sdk.SwrvePushServiceDefaultJobIntentService", jobs.get(0).getService().getClassName());
    }

    @Test
    public void testSilentAuthPush_TargetUserNotCurrentOne() {

        SwrvePushServiceManager pushServiceManager = new SwrvePushServiceManager(mActivity);
        SwrvePushServiceManager pushServiceManagerSpy = spy(pushServiceManager);

        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value1\"}");
        bundle.putString(SwrveNotificationConstants.SWRVE_AUTH_USER_KEY, "SomeOtherUserId");
        doReturn(SwrveTestUtils.parseDate("2017/01/01 0:00")).when(pushServiceManagerSpy).getNow();
        pushServiceManagerSpy.processMessage(bundle);
        assertNumberOfNotification(0);

        SharedPreferences sharedPreferences = mActivity.getSharedPreferences(SwrveCampaignInfluence.INFLUENCED_PREFS, Context.MODE_PRIVATE);
        List<SwrveCampaignInfluence.InfluenceData> influenceData = new SwrveCampaignInfluence().getSavedInfluencedData(sharedPreferences);
        assertTrue(influenceData.size() == 0);
    }

    @Test
    public void testSilentAuthPush_TargetedUser() {

        SwrvePushServiceManager pushServiceManager = new SwrvePushServiceManager(mActivity);
        SwrvePushServiceManager pushServiceManagerSpy = spy(pushServiceManager);

        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.SILENT_PAYLOAD_KEY, "{\"custom\":\"value1\"}");
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
        bundle.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, "some custom values");
        bundle.putString("sound", "default");
        doReturn(SwrveTestUtils.parseDate("2017/01/01 0:00")).when(pushServiceManagerSpy).getNow();
        pushServiceManagerSpy.processMessage(bundle);
        assertNumberOfNotification(1);

        SharedPreferences sharedPreferences = mActivity.getSharedPreferences(SwrveCampaignInfluence.INFLUENCED_PREFS, Context.MODE_PRIVATE);
        List<SwrveCampaignInfluence.InfluenceData> influenceData = new SwrveCampaignInfluence().getSavedInfluencedData(sharedPreferences);
        assertTrue(influenceData.size() == 1);
    }

    private void assertNumberOfNotification(int numberOfNotifications) {
        NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(numberOfNotifications, notifications.size());
    }
}
