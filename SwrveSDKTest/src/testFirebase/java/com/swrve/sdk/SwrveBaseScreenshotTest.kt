package com.swrve.sdk

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.swrve.sdk.config.SwrveConfig
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController

// Shared setup for the in-app message screenshot tests, drawn with native graphics and compared with src/test/screenshots.
// Firebase variant only, for one set of screenshots.
abstract class SwrveBaseScreenshotTest : SwrveBaseTest() {

    protected lateinit var swrveSpy: Swrve

    @Before
    override fun setUp() {
        super.setUp()
        val swrveReal = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", SwrveConfig()) as Swrve
        swrveSpy = spy(swrveReal)
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy)
        SwrveTestUtils.setSDKInstance(swrveSpy)
        SwrveTestUtils.disableAssetsManager(swrveSpy)
        doReturn(true).whenever(swrveSpy).restClientExecutorExecute(any())
        doNothing().whenever(swrveSpy).sendEventsInBackground(any<Context>(), anyString(), any())
        swrveSpy.init(mActivity)
    }

    // Starts the campaign's message on a screen given as qualifiers (w540dp-h1182dp-port-xhdpi is 1080x2364px), with its
    // assets copied from the test resources for native graphics to decode. Call visible() on the result to draw it.
    protected fun startMessage(qualifiers: String, campaignFile: String, event: String, vararg assets: String): ActivityController<SwrveInAppMessageActivity> {
        RuntimeEnvironment.setQualifiers(qualifiers)
        SwrveTestUtils.loadCampaignsFromFile(mActivity!!, swrveSpy, campaignFile, *assets)
        assets.forEach { SwrveTestUtils.writeResourceFileToCache(it, it) }
        swrveSpy.event(event)
        val intent = mShadowActivity!!.peekNextStartedActivity()
        assertNotNull(intent)
        return Robolectric.buildActivity(SwrveInAppMessageActivity::class.java, intent).create().start().resume()
    }

    // Compares with the reference, allowing each pixel a little more colour difference than Roborazzi's default: Skia
    // anti-aliases edges a level or two differently on macOS and on CI's Linux
    protected fun View.captureScreenshot() = captureRoboImage(roborazziOptions = ROBORAZZI_OPTIONS)

    companion object {
        // The standard screen for the tests of message elements: a tall phone, 1080x2364px
        const val PHONE = "w540dp-h1182dp-port-xhdpi"

        private val ROBORAZZI_OPTIONS = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(imageComparator = SimpleImageComparator(maxDistance = 0.02f))
        )
    }
}
