package com.swrve.sdk.test;

import android.app.Activity;

import com.swrve.sdk.ISwrve;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.SwrveSDKBase;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;

import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
public class SwrveGoogleUnitTest {

    @Ignore ("Ignoring temporarily")
    @Test
    public void testOnCreate() throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.setSenderId("SENDER_ID"); // setSenderId is a google SwrveConfig method only.
        ISwrve swrve = SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        swrve.onCreate(new Activity());
        assertNotNull(swrve);
    }

    @Test
    public void testCreateInstance() throws Exception {
        // Force the instance to be null
        Field hack = SwrveSDKBase.class.getDeclaredField("instance");
        hack.setAccessible(true);
        hack.set(null, null);

        try {
            SwrveSDK.sessionStart();
            Assert.fail("Should have thrown exception here because SDK is called without createInstance first.");
        } catch (RuntimeException e) {
            // success
        }
    }
}
