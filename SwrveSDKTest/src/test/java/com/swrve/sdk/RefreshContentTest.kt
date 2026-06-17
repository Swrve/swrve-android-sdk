package com.swrve.sdk

import com.swrve.sdk.rest.IRESTClient
import com.swrve.sdk.rest.IRESTResponseListener
import com.swrve.sdk.rest.RESTResponse
import org.awaitility.Awaitility.await
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.any
import org.mockito.Mockito.doNothing
import org.robolectric.annotation.LooperMode
import java.util.concurrent.atomic.AtomicBoolean

@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class RefreshContentTest : SwrveBaseTest() {

    lateinit var swrveSpy: Swrve
    val responseExceptionCode = -666
    val responseExceptionCodeNullMessage = -667

    @Before
    override fun setUp() {
        super.setUp()
        swrveSpy = SwrveTestUtils.createSpyInstance()
        SwrveCommon.setSwrveCommon(swrveSpy)
        doNothing().`when`(swrveSpy).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testRefreshContent_sdkNotReady() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        swrveSpy.init(mActivity)

        SwrveSDK.stopTracking() // stop the sdk in this test so the sdk is not ready

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrveRefreshContentListener {
            override fun onComplete(result: SwrveRefreshContentListenerResult) {
                assertEquals(SwrveRefreshContentListenerResult.ResultCode.ERROR, result.resultCode)
                assertEquals("SDK is not ready", result.errorMessage)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.refreshContent(listener)
        await().untilTrue(listenerCalled)
    }

    @Test
    fun testRefreshContent_restServerError() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        swrveSpy.init(mActivity)
        swrveSpy.restClient = dummyRestClient(500, "Server error 500")

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrveRefreshContentListener {
            override fun onComplete(result: SwrveRefreshContentListenerResult) {
                assertEquals(SwrveRefreshContentListenerResult.ResultCode.ERROR, result.resultCode)
                assertEquals("Server error 500", result.errorMessage)
                assertEquals(500, result.httpResponseCode)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.refreshContent(listener)
        await().untilTrue(listenerCalled)
    }

    @Test
    fun testRefreshContent_restSuccess() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        swrveSpy.init(mActivity)
        swrveSpy.restClient = dummyRestClient(200, "{}")

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrveRefreshContentListener {
            override fun onComplete(result: SwrveRefreshContentListenerResult) {
                assertEquals(SwrveRefreshContentListenerResult.ResultCode.SUCCESS, result.resultCode)
                assertEquals("", result.errorMessage)
                assertEquals(200, result.httpResponseCode)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.refreshContent(listener)
        await().untilTrue(listenerCalled)
    }

    @Test
    fun testRefreshContent_restFailure() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        swrveSpy.init(mActivity)
        swrveSpy.restClient = dummyRestClient(responseExceptionCode, "") // use the special code to trigger an exception

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrveRefreshContentListener {
            override fun onComplete(result: SwrveRefreshContentListenerResult) {
                assertEquals(SwrveRefreshContentListenerResult.ResultCode.ERROR_UNKNOWN, result.resultCode)
                assertEquals("Test Exception", result.errorMessage)
                assertEquals(0, result.httpResponseCode)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.refreshContent(listener)
        await().untilTrue(listenerCalled)
    }

    @Test
    fun testRefreshContent_restFailureNullExceptionMessage() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        swrveSpy.init(mActivity)
        swrveSpy.restClient = dummyRestClient(responseExceptionCodeNullMessage, "")

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrveRefreshContentListener {
            override fun onComplete(result: SwrveRefreshContentListenerResult) {
                assertEquals(SwrveRefreshContentListenerResult.ResultCode.ERROR_UNKNOWN, result.resultCode)
                assertEquals("Exception", result.errorMessage) // falls back to class simple name when message is null
                assertEquals(0, result.httpResponseCode)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.refreshContent(listener)
        await().untilTrue(listenerCalled)
    }

    private fun dummyRestClient(responseCode: Int, responseBody: String): IRESTClient {
        return object : IRESTClient {
            override fun get(url: String, listener: IRESTResponseListener) {
                when (responseCode) {
                    responseExceptionCode -> listener.onException(Exception("Test Exception"))
                    responseExceptionCodeNullMessage -> listener.onException(Exception()) // null message
                    else -> listener.onResponse(RESTResponse(responseCode, responseBody, null))
                }
            }

            override fun get(url: String, params: Map<String, String>, listener: IRESTResponseListener) {
                when (responseCode) {
                    responseExceptionCode -> listener.onException(Exception("Test Exception"))
                    responseExceptionCodeNullMessage -> listener.onException(Exception()) // null message
                    else -> listener.onResponse(RESTResponse(responseCode, responseBody, null))
                }
            }

            override fun post(url: String, body: String, listener: IRESTResponseListener) {
                listener.onResponse(RESTResponse(responseCode, responseBody, null))
            }

            override fun post(url: String, body: String, listener: IRESTResponseListener, contentType: String) {
                listener.onResponse(RESTResponse(responseCode, responseBody, null))
            }
        }
    }
}
