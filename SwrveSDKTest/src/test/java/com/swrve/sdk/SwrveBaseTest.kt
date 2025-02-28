package com.swrve.sdk

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.swrve.sdk.SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance
import com.swrve.sdk.test.MainActivity
import org.awaitility.Awaitility.await
import org.junit.After
import org.junit.Before
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowLog
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@TargetApi(
    Build.VERSION_CODES.VANILLA_ICE_CREAM
)
abstract class SwrveBaseTest {
    @JvmField
    protected var shadowApplication: ShadowApplication? = null
    @JvmField
    protected var mActivity: Activity? = null
    @JvmField
    protected var mShadowActivity: ShadowActivity? = null

    @Before
    @Throws(Exception::class)
    open fun setUp() {
        RuntimeEnvironment.setQualifiers("+land")
        SwrveLogger.setLogLevel(Log.VERBOSE)
        ShadowLog.stream = System.out
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowApplication = Shadows.shadowOf(application)
        if (mActivity == null) {
            //mActivity = Robolectric.buildActivity(MainActivity.class).create().visible().get(); // this is the old way Robolectric recommends
            waitForActivityLifecycle()
            mShadowActivity = Shadows.shadowOf(mActivity)
        }
    }

    @After
    @Throws(Exception::class)
    open fun tearDown() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
    }

    private fun waitForActivityLifecycle() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.moveToState(Lifecycle.State.CREATED)
        val activityReady = AtomicBoolean(false)
        scenario.onActivity { activity: MainActivity? ->
            mActivity = activity
            activityReady.set(true)
        }
        await().untilTrue(activityReady)
    }

    inner class RetryRule(private val retryCount: Int) : TestRule {
        override fun apply(base: Statement, description: Description): Statement {
            return object : Statement() {
                @Throws(Throwable::class)
                override fun evaluate() {
                    var caughtThrowable: Throwable? = null
                    for (i in 0 until retryCount) {
                        try {
                            base.evaluate()
                            return
                        } catch (t: Throwable) {
                            caughtThrowable = t
                        }
                    }
                    if (caughtThrowable != null) {
                        throw caughtThrowable
                    }
                }
            }
        }
    }
}
