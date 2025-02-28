package com.swrve.sdk

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.swrve.sdk.SwrveTestUtils.createSpyInstance
import com.swrve.sdk.SwrveTestUtils.disableBeforeSendDeviceInfo
import com.swrve.sdk.SwrveTestUtils.flushLifecycleExecutorQueue
import com.swrve.sdk.SwrveTestUtils.loadCampaignsFromFile
import com.swrve.sdk.SwrveTestUtils.setSDKInstance
import com.swrve.sdk.SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance
import com.swrve.sdk.rest.IRESTClient
import com.swrve.sdk.rest.IRESTResponseListener
import com.swrve.sdk.rest.RESTResponse
import org.awaitility.Awaitility
import org.awaitility.Durations
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import java.util.concurrent.atomic.AtomicBoolean

class StopTrackingTest : SwrveBaseTest() {
    private var swrveSpy: Swrve? = null
    private var backgroundEventSenderMock: SwrveBackgroundEventSender? = null

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        swrveSpy = createSpyInstance()

        backgroundEventSenderMock = Mockito.mock(SwrveBackgroundEventSender::class.java)
        doNothing().`when`(backgroundEventSenderMock)?.send(anyString(), ArgumentMatchers.anyList())
        doReturn(backgroundEventSenderMock).`when`(swrveSpy)?.getSwrveBackgroundEventSender(any(Context::class.java))

        swrveSpy!!.init(mActivity)
    }

    @Test
    fun testStopTracking() {
        assertEquals(SwrveTrackingState.STARTED, swrveSpy!!.profileManager.trackingState)
        assertTrue(SwrveSDK.isStarted())
        verify(swrveSpy, Mockito.times(1))?.queueDeviceUpdateNow(anyString(), anyString(), anyBoolean()) // device info queued once upon init
        assertTrue(swrveSpy!!.isSdkReady)

        SwrveSDK.stopTracking()

        assertEquals(SwrveTrackingState.STOPPED, swrveSpy!!.profileManager.trackingState)
        assertFalse(SwrveSDK.isStarted())
        verify(swrveSpy, Mockito.times(2))?.queueDeviceUpdateNow(anyString(), anyString(), anyBoolean()) // new device info queued after stop
        assertFalse(swrveSpy!!.isSdkReady)

        verify(swrveSpy, Mockito.atLeastOnce())?.clearAllAuthenticatedNotifications() // verify clearAllAuthenticatedNotifications is called

        assertNull(swrveSpy!!.campaignsAndResourcesExecutor)
    }

    @Test
    @Throws(Exception::class)
    fun testStopTrackingAndIAM() {
        // build IAM and show it

        loadCampaignsFromFile(mActivity!!, swrveSpy!!, "campaign_right_away.json", "1111111111111111111111111")
        val intent = Intent(ApplicationProvider.getApplicationContext(), SwrveInAppMessageActivity::class.java)
        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, 165)
        val activity = Robolectric.buildActivity(SwrveInAppMessageActivity::class.java, intent).create().get()

        // verify it is the current activity showing and not finishing
        var activityCurrent = swrveSpy!!.activityContext.get()
        assertEquals(activity, activityCurrent)
        assertFalse(activityCurrent!!.isFinishing)

        swrveSpy!!.stopTracking()

        // verify it is finishing after stop
        activityCurrent = swrveSpy!!.activityContext.get()
        assertTrue(activityCurrent!!.isFinishing)
    }

    @Test
    @Throws(Exception::class)
    fun testStopTrackingAndDeviceUpdate() {
        shutdownAndRemoveSwrveSDKSingletonInstance()

        // some setup but crucially set a dummy restclient to intercept device update events via setRestClientToAssertDeviceUpdate method.
        val swrveReal = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey") as Swrve
        flushLifecycleExecutorQueue(swrveReal) // wait until swrve instance is fully created before getting a mockito spy.
        swrveSpy = spy(swrveReal)
        disableBeforeSendDeviceInfo(swrveReal, swrveSpy!!) // disable token registration
        setSDKInstance(swrveSpy)
        backgroundEventSenderMock = Mockito.mock(SwrveBackgroundEventSender::class.java)
        doNothing().`when`(backgroundEventSenderMock)?.send(anyString(), anyList())
        doReturn(backgroundEventSenderMock).`when`(swrveSpy)?.getSwrveBackgroundEventSender(any(Context::class.java))

        // test device update sent upon init
        val initCallback = AtomicBoolean(false)
        setRestClientToAssertDeviceUpdate(initCallback, "swrve.tracking_state", "STARTED")
        swrveSpy!!.init(mActivity)
        Awaitility.await().atMost(Durations.ONE_MINUTE).untilTrue(initCallback) // wait until device info queued and swrve.tracking_state sent

        // test device update after stop called
        val stopTrackingCallback = AtomicBoolean(false)
        setRestClientToAssertDeviceUpdate(stopTrackingCallback, "swrve.tracking_state", "STOPPED")
        SwrveSDK.stopTracking()
        Awaitility.await().atMost(Durations.ONE_MINUTE).untilTrue(stopTrackingCallback) // wait until device info queued and swrve.tracking_state sent
    }

    private fun setRestClientToAssertDeviceUpdate(callbackCompleted: AtomicBoolean, deviceUpdateAttributeName: String, deviceUpdateAttributeExpectedValue: String) {
        // this rest client will swallow rest calls and won't actually make any network calls

        swrveSpy!!.restClient = object : IRESTClient {
            override fun get(endpoint: String, callback: IRESTResponseListener) {
                // empty
            }

            override fun get(endpoint: String, params: Map<String, String>, callback: IRESTResponseListener) {
                // empty
            }

            override fun post(endpoint: String, encodedBody: String, callback: IRESTResponseListener) {
                if (encodedBody.contains("device_update")) {
                    try {
                        val body = JSONObject(encodedBody)
                        Assert.assertNotNull(body)
                        val data = body.getJSONArray("data")
                        var deviceUpdate: JSONObject? = null
                        Assert.assertNotNull(data)

                        for (i in 0 until data.length()) {
                            val item = data.getJSONObject(i)
                            if (item.getString("type") == "device_update") {
                                deviceUpdate = item
                                break // only looks at first device_update, there could be multiple.
                            }
                        }
                        Assert.assertNotNull(deviceUpdate)

                        val attributeDevices = deviceUpdate!!.getJSONObject("attributes")
                        if (attributeDevices.has(deviceUpdateAttributeName)) {
                            val trackingState = attributeDevices.getString(deviceUpdateAttributeName)
                            assertTrue(SwrveHelper.isNotNullOrEmpty(trackingState))
                            assertEquals(deviceUpdateAttributeExpectedValue, attributeDevices.getString(deviceUpdateAttributeName))
                            callbackCompleted.set(true)
                        }
                    } catch (ex: Exception) {
                        SwrveLogger.e("Error checking for device_update.", ex)
                    }
                }
                callback.onResponse(RESTResponse(200, "success", null))
            }

            override fun post(endpoint: String, encodedBody: String, callback: IRESTResponseListener, contentType: String) {
                // empty
            }
        }
    }
}
