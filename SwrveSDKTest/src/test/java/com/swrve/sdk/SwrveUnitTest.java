package com.swrve.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.test.BuildConfig;
import com.swrve.sdk.test.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class SwrveUnitTest {

    private Activity mActivity;

    @Before
    public void setUp() throws Exception {
        removeSingletonInstance();
        mActivity = Robolectric.buildActivity(MainActivity.class).create().visible().get();
        ShadowLog.stream = System.out;
    }

    @After
    public void tearDown() throws Exception {
        removeSingletonInstance();
    }

    private void removeSingletonInstance() throws Exception{
        Field instance = SwrveSDKBase.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    public void testInitWithAppVersion() throws Exception {
        String appVersion = "my_version";
        SwrveConfig config = new SwrveConfig();
        config.setAppVersion(appVersion);
        Swrve swrve = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        assertEquals(appVersion, swrve.appVersion);
    }

    @Test
    public void testLanguage() throws Exception {
        String strangeLanguage = "strange_language";
        String strangeLanguage2 = "strange_language_other";
        SwrveConfig config = new SwrveConfig();
        config.setLanguage(strangeLanguage);
        ISwrve swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        assertEquals(strangeLanguage, swrve.getLanguage());
        swrve.setLanguage(strangeLanguage2);
        assertEquals(strangeLanguage2, swrve.getLanguage());
    }

    @Test
    public void testInitialisationWithUserId() throws Exception {
        ISwrve swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey");
        String userId = swrve.getUserId();
        assertNotNull(userId);

        removeSingletonInstance();

        SwrveConfig config = new SwrveConfig();
        config.setUserId("custom_user_id");
        swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        String userId2 = swrve.getUserId();
        assertNotSame(userId, userId2);
        assertEquals("custom_user_id", userId2);
    }

    @Test
    public void testInitialisationWithNoId() throws Exception {
        ISwrve swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey");
        String userId = swrve.getUserId();
        assertNotNull(userId);

        removeSingletonInstance();

        swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey");
        String userId2 = swrve.getUserId();
        assertEquals(userId, userId2);
    }

    @Test
    public void testGetUserIdForced() {
        SwrveConfig config = new SwrveConfig();
        config.setUserId("forced");
        ISwrve swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        assertEquals("forced", swrve.getUserId());
    }
}
