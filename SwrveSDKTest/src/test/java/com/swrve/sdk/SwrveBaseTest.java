package com.swrve.sdk;

import static org.awaitility.Awaitility.await;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.test.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public abstract class SwrveBaseTest {

    protected ShadowApplication shadowApplication;
    protected Activity mActivity;
    protected ShadowActivity mShadowActivity;

    @Before
    public void setUp() throws Exception {
        RuntimeEnvironment.setQualifiers("+land");
        SwrveLogger.setLogLevel(Log.VERBOSE);
        ShadowLog.stream = System.out;
        Application application = ApplicationProvider.getApplicationContext();
        shadowApplication = Shadows.shadowOf(application);
        if (mActivity == null) {
            //mActivity = Robolectric.buildActivity(MainActivity.class).create().visible().get(); // this is the old way Robolectric recommends
            waitForActivityLifecycle();
            mShadowActivity = Shadows.shadowOf(mActivity);
        }
    }

    @After
    public void tearDown() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
    }

    private void waitForActivityLifecycle() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.moveToState(Lifecycle.State.CREATED);
        final AtomicBoolean activityReady = new AtomicBoolean(false);
        scenario.onActivity(activity -> {
            mActivity = activity;
            activityReady.set(true);
        });
        await().untilTrue(activityReady);
    }
}
