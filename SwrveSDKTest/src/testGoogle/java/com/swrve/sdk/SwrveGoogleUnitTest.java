package com.swrve.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.gcm.SwrveGcmConstants;
import com.swrve.sdk.test.BuildConfig;
import com.swrve.sdk.test.MainActivity;
import com.swrve.sdk.utils.TestHttpServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLog;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class SwrveGoogleUnitTest extends SwrveBaseTest {

    private Activity activity;

    @Before
    public void setUp() throws Exception {
        removeSingletonInstance();
        activity = Robolectric.buildActivity(MainActivity.class).create().visible().get();
        ShadowLog.stream = System.out;
    }

    @After
    public void tearDown() throws Exception {
        removeSingletonInstance();
    }

    private void removeSingletonInstance() throws Exception {
        Field singleton = SwrveSDKBase.class.getDeclaredField("instance");
        singleton.setAccessible(true);
        singleton.set(null, null);
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

        ShadowActivity shadowMainActivity = Shadows.shadowOf(activity);
        Intent nextIntent = shadowMainActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(Intent.ACTION_VIEW, nextIntent.getAction());
        assertEquals("swrve://deeplink/campaigns", nextIntent.getData().toString());
        assertTrue(nextIntent.hasExtra("customdata"));
        assertEquals("customdata_value", nextIntent.getStringExtra("customdata"));
    }

    @Test
    public void testManualRegistrationId() throws InterruptedException, JSONException {
        SwrveConfig config = new SwrveConfig();
        initLocalHttpServer(activity, config);
        config.setSenderId("12345");
        config.setObtainRegistrationIdEnabled(false);
        Swrve swrve = (Swrve) SwrveSDK.createInstance(activity, 1, "apiKey", config);
        swrve.onCreate(activity);
        swrve.setRegistrationId("manual_reg_id");

        // Find a POST with device info where we sent the right new sender ID
        boolean foundPost = false;
        int retries = 10;
        while(!foundPost && (retries--) > 0) {
            Iterator<TestHttpServer.TestHttpPostRequest> it = httpEventServer.postRequests.iterator();
            while(it.hasNext()) {
                TestHttpServer.TestHttpPostRequest request = it.next();
                if (request.body.contains("swrve.gcm_token")) {
                    JSONArray jsonEvents = request.getEventsJSON();
                    if (jsonEvents != null && jsonEvents.length() > 0) {
                        for (int i = 0; i < jsonEvents.length() && !foundPost; i++) {
                            JSONObject jsonEvent = jsonEvents.getJSONObject(i);
                            if (jsonEvent.get("type").equals("user")) {
                                JSONObject jsonAttributes = jsonEvent.getJSONObject("attributes");
                                if (jsonAttributes.has("swrve.gcm_token")) {
                                    foundPost = jsonAttributes.get("swrve.gcm_token").equals("manual_reg_id");
                                }
                            }
                        }
                    }
                }
            }

            if (!foundPost) {
                Thread.sleep(1000);
            }
        }
        assertTrue(foundPost);
        swrve.onDestroy(activity);
    }
}
