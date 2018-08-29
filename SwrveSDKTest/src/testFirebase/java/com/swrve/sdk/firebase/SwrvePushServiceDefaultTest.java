package com.swrve.sdk.firebase;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveNotificationConstants;
import com.swrve.sdk.push.SwrvePushServiceDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SwrvePushServiceDefaultTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application);
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
        assertEquals("com.swrve.sdk.push.SwrvePushServiceDefaultReceiver", intents.get(0).getComponent().getClassName());
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
        assertEquals("com.swrve.sdk.push.SwrvePushServiceDefaultJobIntentService", jobs.get(0).getService().getClassName());
    }
}
