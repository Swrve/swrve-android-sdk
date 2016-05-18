package com.swrve.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.gcm.ISwrvePushNotificationListener;
import com.swrve.sdk.gcm.ISwrveSilentPushNotificationListener;
import com.swrve.sdk.gcm.SwrveGcmConstants;
import com.swrve.sdk.gcm.SwrveGcmIntentService;
import com.swrve.sdk.gcm.TestSwrveGcmHandler;
import com.swrve.sdk.test.BuildConfig;
import com.swrve.sdk.test.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLog;

import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class SwrveGoogleUnitTest {

    private Activity activity;

    @Before
    public void setUp() throws Exception {
        SwrveTestUtils.removeSingletonInstance();
        activity = Robolectric.buildActivity(MainActivity.class).create().visible().get();
        ShadowLog.stream = System.out;
    }

    @After
    public void tearDown() throws Exception {
        SwrveTestUtils.removeSingletonInstance();
    }

    @Test
    public void testPushListener() throws Exception {
        SwrveTestUtils.startTestServers(activity);
        SwrveConfig config = SwrveTestUtils.configToLocalServer();
        config.setSenderId("12345");

        SwrveObservable swrve = SwrveObservable.createInstance(activity, 1, "apiKey", config);
        swrve.clearData();
        swrve.stopEventsFromBeingSent = true;
        swrve.onCreate(activity);

        final Bundle[] triggeredBundle = new Bundle[1];
        swrve.setPushNotificationListener(new ISwrvePushNotificationListener() {
            @Override
            public void onPushNotification(Bundle bundle) {
                triggeredBundle[0] = bundle;
            }
        });

        // Simulate engagement of the local notification
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("customdata", "customdata_value");
        extras.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1");
        intent.putExtra(SwrveGcmConstants.GCM_BUNDLE, extras);
        swrve.processIntent(intent);
        swrve.waitForJobs(60);

        // Check that the listener was triggered
        assertEquals("customdata_value", triggeredBundle[0].get("customdata"));
        assertEquals("1", triggeredBundle[0].get(SwrveGcmConstants.SWRVE_TRACKING_KEY));
        // Check engaged event was sent
        LinkedHashMap<Long, String> events = swrve.cachedLocalStorage.getFirstNEvents(100);
        assertNotNull(SwrveTestUtils.getEventWithName(events, "Swrve.Messages.Push-1.engaged"));

        swrve.shutdown();
        SwrveTestUtils.stopTestServers();
    }

    @Test
    public void testSilentPushListener() throws Exception {
        SwrveGcmIntentService swrveGcmService = new SwrveGcmIntentService();
        swrveGcmService.onCreate();
        TestSwrveGcmHandler handler = new TestSwrveGcmHandler(RuntimeEnvironment.application, swrveGcmService);

        Bundle bundle = new Bundle();
        bundle.putString("customdata", "customdata_value");
        bundle.putString(SwrveGcmConstants.SWRVE_SILENT_TRACKING_KEY, "1");

        SwrveConfig config = new SwrveConfig();
        config.setSenderId("12345");
        SwrveObservable swrve = SwrveObservable.createInstance(activity, 1, "apiKey", config);
        swrve.clearData();

        final Bundle[] triggeredBundle = new Bundle[1];
        swrve.setPushNotificationListener(new ISwrvePushNotificationListener() {
            @Override
            public void onPushNotification(Bundle bundle) {
                triggeredBundle[0] = bundle;
            }
        });
        final Bundle[] triggeredSilentBundle = new Bundle[1];
        swrve.setSilentPushNotificationListener(new ISwrveSilentPushNotificationListener() {
            @Override
            public void onSilentPushNotification(Bundle bundle) {
                triggeredSilentBundle[0] = bundle;
            }
        });

        handler.onMessageReceived("1234", bundle);

        // Check that the silent listener was triggered
        assertEquals("customdata_value", triggeredSilentBundle[0].get("customdata"));
        assertEquals("1", triggeredSilentBundle[0].get(SwrveGcmConstants.SWRVE_SILENT_TRACKING_KEY));
        // Check that the normal push notification listener was not triggered
        assertNull(triggeredBundle[0]);
        // Check that no event was sent
        LinkedHashMap<Long, String> events = swrve.cachedLocalStorage.getFirstNEvents(100);
        assertNull(SwrveTestUtils.getEventWithName(events, "Swrve.Messages.Push-1.engaged"));
    }

    @Test
    public void testProcessIntentWithDeeplink() throws Exception {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("customdata", "customdata_value");
        extras.putString(SwrveGcmConstants.DEEPLINK_KEY, "swrve://deeplink/campaigns");
        extras.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1");
        intent.putExtra(SwrveGcmConstants.GCM_BUNDLE, extras);

        SwrveConfig config = new SwrveConfig();
        config.setSenderId("12345");
        Swrve swrve = (Swrve) SwrveSDK.createInstance(activity, 1, "apiKey", config);
        swrve.cachedLocalStorage = swrve.createCachedLocalStorage();

        swrve.processIntent(intent);

        // Check that the deeplink was launched
        ShadowActivity shadowMainActivity = Shadows.shadowOf(activity);
        Intent nextIntent = shadowMainActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(Intent.ACTION_VIEW, nextIntent.getAction());
        assertEquals("swrve://deeplink/campaigns", nextIntent.getData().toString());
        assertTrue(nextIntent.hasExtra("customdata"));
        assertEquals("customdata_value", nextIntent.getStringExtra("customdata"));
    }

}
