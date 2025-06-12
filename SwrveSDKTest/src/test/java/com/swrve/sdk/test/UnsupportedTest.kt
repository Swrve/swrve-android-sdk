package com.swrve.sdk.test

import android.os.Build
import com.swrve.sdk.SwrveBaseEmpty
import com.swrve.sdk.SwrveBaseTest
import com.swrve.sdk.SwrveHelper
import com.swrve.sdk.SwrveIAPRewards
import com.swrve.sdk.SwrveSDK
import com.swrve.sdk.SwrveUserResourcesDiffListener
import com.swrve.sdk.SwrveUserResourcesListener
import com.swrve.sdk.config.SwrveConfigBase
import org.json.JSONException
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import java.util.Locale

/**
 * Test that the SDK fails gracefully in unsupported versions (lower than 4.X)
 */
class UnsupportedTest : SwrveBaseTest() {
    private var originalSDKVersion = 0

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        originalSDKVersion = Build.VERSION.SDK_INT
        // Explicitly hack the static field that describes the sdk version
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 15)
    }

    @After
    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        // revert to the original sdk version
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", originalSDKVersion)
    }

    @Test
    @Throws(JSONException::class)
    fun testSDKDoesNothingAndDoesntCrash() {
        if (SwrveHelper.sdkAvailable()) {
            // This test should only be executed in platforms levels that the sdk does not support. Otherwise fail it.
            Assert.fail("UnsupportedTestHelper explicitly tests sdk on devices where it is not meant to run")
        }

        // Add here all the external API calls and objects that a customer can invoke
        val sdk = SwrveSDK.createInstance(mActivity!!.application, 572, "fake_api_key")
        // The SDK is an empty SDK
        Assert.assertTrue(sdk is SwrveBaseEmpty<*, *>)

        val payload: MutableMap<String, String> = HashMap()
        payload["key"] = "value"
        val rewards = SwrveIAPRewards("USD", 99)
        rewards.addCurrency("USD", 100)
        rewards.addItem("stars", 1)
        val rewardsJson = rewards.rewardsJSON
        Assert.assertNotNull(rewardsJson)

        sdk.sessionStart()
        sdk.event("new_event")
        sdk.event("event", payload)
        sdk.purchase("item", "USB", 10, 1)
        sdk.currencyGiven("diamonds", 99.0)
        sdk.userUpdate(payload)
        sdk.iap(1, "productId", 0.99, "USB")
        sdk.iap(1, "productId", 0.99, "USB", rewards)
        val resourceManager = sdk.resourceManager
        Assert.assertNotNull(rewardsJson)

        sdk.setResourcesListener {}
        sdk.getUserResources(object : SwrveUserResourcesListener {
            override fun onUserResourcesSuccess(
                resources: Map<String, Map<String, String>>,
                resourcesAsJSON: String?
            ) {
            }

            override fun onUserResourcesError(exception: Exception) {
            }
        })
        sdk.getUserResourcesDiff(object : SwrveUserResourcesDiffListener {
            override fun onUserResourcesDiffSuccess(
                oldResourcesValues: Map<String, Map<String, String>>,
                newResourcesValues: Map<String, Map<String, String>>,
                resourcesAsJSON: String?
            ) {
            }

            override fun onUserResourcesDiffError(exception: Exception) {
            }
        })
        sdk.sendQueuedEvents()
        sdk.flushToDisk()
        sdk.shutdown()
        sdk.setLanguage(Locale.JAPAN)
        val language = sdk.language
        Assert.assertEquals("ja-JP", language)

        val apiKey = sdk.apiKey
        Assert.assertNotNull(apiKey)

        val userId = sdk.userId
        Assert.assertNotNull(userId)

        val deviceInfo = sdk.deviceInfo
        Assert.assertNotNull(deviceInfo)

        sdk.refreshContent(null)

        val cacheDir = sdk.cacheDir
        Assert.assertNotNull(cacheDir)

        val initialisedTime = sdk.initialisedTime
        Assert.assertNotNull(initialisedTime)

        val config: SwrveConfigBase = sdk.config
        Assert.assertNotNull(config)

        sdk.stopTracking()
    }
}
