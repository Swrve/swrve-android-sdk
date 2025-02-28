package com.swrve.sdk

import android.content.Context
import com.google.gson.internal.LinkedTreeMap
import com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_PIM_DELETE
import com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_PIM_ENGAGED
import com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_PIM_READ
import com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PIM
import com.swrve.sdk.SwrvePushInboxListenerResult.ResultCode
import com.swrve.sdk.rest.IRESTClient
import com.swrve.sdk.rest.IRESTResponseListener
import com.swrve.sdk.rest.RESTResponse
import org.awaitility.Awaitility.await
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.any
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.annotation.LooperMode
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class PushInboxTest : SwrveBaseTest() {

    lateinit var swrveSpy: Swrve
    val responseExceptionCode = -666
    val RESPONSE_MODIFED = "{\"state\": \"modified\"}"
    val RESPONSE_UNMODIFED = "{\"state\": \"unmodified\"}"

    @Before
    override fun setUp() {
        super.setUp()
        swrveSpy = SwrveTestUtils.createSpyInstance()
        SwrveCommon.setSwrveCommon(swrveSpy)
        doNothing().`when`(swrveSpy).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testPushInboxUserProperty() {
        val deviceInfo = swrveSpy._getDeviceInfo()
        assertEquals(deviceInfo.getBoolean("swrve.support.push_inbox"), true)
    }

    @Test
    fun testPushInboxMessagesJsonParsed() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)
        assertEquals(8, swrveSpy.pushInboxManager.getMessages().size)

        val message0 = swrveSpy.pushInboxManager.getMessages()[0]
        assertEquals(1, message0.messageId)
        assertEquals(11, message0.variantId)
        assertEquals(1714655770, message0.endDate)
        assertEquals(SwrvePushInboxMessageState.UNREAD, message0.state)
        assertEquals(1714555770, message0.sentDate)
        val messageJson = JSONObject(
            "{\n" +
                    "        \"title\": \"New Arrivals!\",\n" +
                    "        \"body\": \"Check out our latest collection of running shoes and activewear. Stay ahead of the game with FitGear's newest arrivals.\"\n" +
                    "}"
        )
        assertEquals(messageJson.toString(), message0.customerJson.toString())

        // Check that the second message is READ
        val message1 = swrveSpy.pushInboxManager.getMessages()[1]
        assertEquals(SwrvePushInboxMessageState.READ, message1.state)
    }

    @Test
    fun testGetPushInboxMessages() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages_past_and_future.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        val messages = SwrveSDK.getPushInboxMessages()
        // Only 3 messages should be returned which have messageId 1, 3 and 4. The other messages are either expired.
        assertEquals(3, messages.size)
        assertEquals(1, messages[0].messageId)
        assertEquals(3, messages[1].messageId)
        assertEquals(4, messages[2].messageId)
    }

    @Test
    fun testEngage_sdkNotReady() {

        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        SwrveSDK.stopTracking() // stop the sdk in this test so the sdk is not ready

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.ERROR, result.resultCode)
                assertEquals("SDK is not ready", result.errorMessage)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.engagePushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        verify(swrveSpy, never()).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testRead_sdkNotReady() {

        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        SwrveSDK.stopTracking() // stop the sdk in this test so the sdk is not ready

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.ERROR, result.resultCode)
                assertEquals("SDK is not ready", result.errorMessage)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.readPushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        verify(swrveSpy, never()).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testDelete_sdkNotReady() {

        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        SwrveSDK.stopTracking() // stop the sdk in this test so the sdk is not ready

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.ERROR, result.resultCode)
                assertEquals("SDK is not ready", result.errorMessage)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.deletePushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        verify(swrveSpy, never()).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testRead_restServerError() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(500, RESPONSE_MODIFED)

        val message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
        assertNotNull(message)
        assertEquals(SwrvePushInboxMessageState.UNREAD, message!!.state)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.ERROR, result.resultCode)
                assertEquals("Push Inbox Message 1 failed to mark as read. Server response code:500", result.errorMessage)
                assertEquals(500, result.httpResponseCode)
                assertEquals(SwrvePushInboxMessageState.UNREAD, message.state)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.readPushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        verify(swrveSpy, never()).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testRead_restAlreadyRead() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(200, RESPONSE_UNMODIFED)

        val message = swrveSpy.pushInboxManager.getPushInboxMessage(2) // this message is already read
        assertNotNull(message)
        assertEquals(SwrvePushInboxMessageState.READ, message!!.state)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId: Long, result: SwrvePushInboxListenerResult) {
                assertEquals(2, messageId)
                assertEquals(ResultCode.SUCCESS, result.resultCode)
                assertEquals("", result.errorMessage)
                assertEquals(200, result.httpResponseCode)
                assertEquals(SwrvePushInboxMessageState.READ, message.state)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.readPushInboxMessage(2, listener)
        await().untilTrue(listenerCalled)

        verify(swrveSpy, never()).sendEventsInBackground(any(), anyString(), any()) // No read event will be queued
    }

    @Test
    fun testDelete_restServerError() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(500, RESPONSE_MODIFED)

        var message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
        assertNotNull(message)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.ERROR, result.resultCode)
                assertEquals("Push Inbox Message 1 failed to delete. Server response code:500", result.errorMessage)
                assertEquals(500, result.httpResponseCode)
                message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
                assertNotNull(message) // message should not be deleted
                listenerCalled.set(true)
            }
        }

        SwrveSDK.deletePushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        verify(swrveSpy, never()).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testRead_restUserError() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(400, RESPONSE_MODIFED)

        val message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
        assertNotNull(message)
        assertEquals(SwrvePushInboxMessageState.UNREAD, message!!.state)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.ERROR, result.resultCode)
                assertEquals("Push Inbox Message 1 failed to mark as read. Server response code:400", result.errorMessage)
                assertEquals(400, result.httpResponseCode)
                assertEquals("message should remain unread", SwrvePushInboxMessageState.UNREAD, message.state)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.readPushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        verify(swrveSpy, never()).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testDelete_restUserError() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(400, RESPONSE_MODIFED)

        var message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
        assertNotNull(message)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.ERROR, result.resultCode)
                assertEquals("Push Inbox Message 1 failed to delete. Server response code:400", result.errorMessage)
                assertEquals(400, result.httpResponseCode)
                message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
                assertNotNull(message) // message should not be deleted
                listenerCalled.set(true)
            }
        }

        SwrveSDK.deletePushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        verify(swrveSpy, never()).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testRead_restUserRetryError() { // explicit test the case where the server returns a 429
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(429, RESPONSE_MODIFED)

        val message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
        assertNotNull(message)
        assertEquals(SwrvePushInboxMessageState.UNREAD, message!!.state)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.ERROR, result.resultCode)
                assertEquals("Push Inbox Message 1 failed to mark as read. Server response code:429", result.errorMessage)
                assertEquals(429, result.httpResponseCode)
                assertEquals(SwrvePushInboxMessageState.UNREAD, message.state) // message should remain unread
                listenerCalled.set(true)
            }
        }

        SwrveSDK.readPushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        verify(swrveSpy, never()).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testRead_restSuccess() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(200, RESPONSE_MODIFED)

        val message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
        assertNotNull(message)
        assertEquals(SwrvePushInboxMessageState.UNREAD, message!!.state)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.SUCCESS, result.resultCode)
                assertEquals("", result.errorMessage)
                assertEquals(200, result.httpResponseCode)
                assertEquals(SwrvePushInboxMessageState.READ, message.state) // message should now be marked as read
                listenerCalled.set(true)
            }
        }

        SwrveSDK.readPushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        val contextCaptor = ArgumentCaptor.forClass(Context::class.java)
        val userIdCaptor = ArgumentCaptor.forClass(String::class.java)
        val eventsCaptor = ArgumentCaptor.forClass(ArrayList::class.java) as ArgumentCaptor<ArrayList<String>>
        verify(swrveSpy, times(1)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture())

        val capturedEvents = eventsCaptor.allValues
        val jsonString = capturedEvents[0][0]
        val event = JSONObject(jsonString)
        val expectedPayload: MutableMap<String, Any?> = LinkedTreeMap()
        expectedPayload["messageId"] = "1"

        SwrveTestUtils.assertGenericEvent(event.toString(), null, GENERIC_EVENT_CAMPAIGN_TYPE_PIM, GENERIC_EVENT_ACTION_TYPE_PIM_READ, expectedPayload)
    }

    @Test
    fun testEngage_restSuccess() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(200, RESPONSE_MODIFED)

        val message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
        assertNotNull(message)
        assertEquals(SwrvePushInboxMessageState.UNREAD, message!!.state)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.SUCCESS, result.resultCode)
                assertEquals("", result.errorMessage)
                assertEquals(200, result.httpResponseCode)
                assertEquals(SwrvePushInboxMessageState.READ, message.state) // message should now be marked as read
                listenerCalled.set(true)
            }
        }

        SwrveSDK.engagePushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        val contextCaptor = ArgumentCaptor.forClass(Context::class.java)
        val userIdCaptor = ArgumentCaptor.forClass(String::class.java)
        val eventsCaptor = ArgumentCaptor.forClass(ArrayList::class.java) as ArgumentCaptor<ArrayList<String>>
        verify(swrveSpy, times(2)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture())

        val capturedEvents = eventsCaptor.allValues

        // engaged event should be sent first
        val jsonString = capturedEvents[0][0]
        val event = JSONObject(jsonString)
        val expectedPayload: MutableMap<String, Any?> = LinkedTreeMap()
        expectedPayload["state"] = "unread"
        expectedPayload["messageId"] = "1"
        SwrveTestUtils.assertGenericEvent(event.toString(), null, GENERIC_EVENT_CAMPAIGN_TYPE_PIM, GENERIC_EVENT_ACTION_TYPE_PIM_ENGAGED, expectedPayload)

        // read event should be sent second
        val jsonString2 = capturedEvents[1][0]
        val event2 = JSONObject(jsonString2)
        expectedPayload.clear();
        expectedPayload["messageId"] = "1"
        SwrveTestUtils.assertGenericEvent(event2.toString(), null, GENERIC_EVENT_CAMPAIGN_TYPE_PIM, GENERIC_EVENT_ACTION_TYPE_PIM_READ, expectedPayload)
    }

    @Test
    fun testEngage_restSuccessAlreadyRead() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(200, RESPONSE_UNMODIFED)

        val message = swrveSpy.pushInboxManager.getPushInboxMessage(2) // this message is already read
        assertNotNull(message)
        assertEquals(SwrvePushInboxMessageState.READ, message!!.state)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(2, messageId)
                assertEquals(ResultCode.SUCCESS, result.resultCode)
                assertEquals("", result.errorMessage)
                assertEquals(200, result.httpResponseCode)
                assertEquals(SwrvePushInboxMessageState.READ, message.state) // message should now be marked as read
                listenerCalled.set(true)
            }
        }

        SwrveSDK.engagePushInboxMessage(2, listener)
        await().untilTrue(listenerCalled)

        val contextCaptor = ArgumentCaptor.forClass(Context::class.java)
        val userIdCaptor = ArgumentCaptor.forClass(String::class.java)
        val eventsCaptor = ArgumentCaptor.forClass(ArrayList::class.java) as ArgumentCaptor<ArrayList<String>>
        verify(swrveSpy, times(1)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture())

        val capturedEvents = eventsCaptor.allValues

        // only the engaged event should be sent and capturedEvents should be size 1
        assertEquals(1, capturedEvents.size)

        // engaged event should be sent first
        val jsonString = capturedEvents[0][0]
        val event = JSONObject(jsonString)
        val expectedPayload: MutableMap<String, Any?> = LinkedTreeMap()
        expectedPayload["state"] = "read"
        expectedPayload["messageId"] = "2"
        SwrveTestUtils.assertGenericEvent(event.toString(), null, GENERIC_EVENT_CAMPAIGN_TYPE_PIM, GENERIC_EVENT_ACTION_TYPE_PIM_ENGAGED, expectedPayload)
    }

    @Test
    fun testEngage_restFailure() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(500, RESPONSE_MODIFED)

        val message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
        assertNotNull(message)
        assertEquals(SwrvePushInboxMessageState.UNREAD, message!!.state)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.ERROR, result.resultCode)
                assertEquals("Push Inbox Message 1 failed to mark as read. Server response code:500", result.errorMessage)
                assertEquals(500, result.httpResponseCode)
                assertEquals(SwrvePushInboxMessageState.UNREAD, message.state)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.engagePushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        val contextCaptor = ArgumentCaptor.forClass(Context::class.java)
        val userIdCaptor = ArgumentCaptor.forClass(String::class.java)
        val eventsCaptor = ArgumentCaptor.forClass(ArrayList::class.java) as ArgumentCaptor<ArrayList<String>>
        verify(swrveSpy, times(1)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture())

        val capturedEvents = eventsCaptor.allValues

        // engaged event should be sent first
        val jsonString = capturedEvents[0][0]
        val event = JSONObject(jsonString)
        val expectedPayload: MutableMap<String, Any?> = LinkedTreeMap()
        expectedPayload["state"] = "unread"
        expectedPayload["messageId"] = "1"
        SwrveTestUtils.assertGenericEvent(event.toString(), null, GENERIC_EVENT_CAMPAIGN_TYPE_PIM, GENERIC_EVENT_ACTION_TYPE_PIM_ENGAGED, expectedPayload)

        // NO read event should be sent, which is verified by the number of times(1) the sendEventsInBackground is called previously
    }

    @Test
    fun testDelete_restSuccess() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(200, RESPONSE_MODIFED)

        var message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
        assertNotNull(message)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.SUCCESS, result.resultCode)
                assertEquals("", result.errorMessage)
                assertEquals(200, result.httpResponseCode)
                message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
                assertNull(message) // message should be deleted
                listenerCalled.set(true)
            }
        }

        SwrveSDK.deletePushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        val contextCaptor = ArgumentCaptor.forClass(Context::class.java)
        val userIdCaptor = ArgumentCaptor.forClass(String::class.java)
        val eventsCaptor = ArgumentCaptor.forClass(ArrayList::class.java) as ArgumentCaptor<ArrayList<String>>
        verify(swrveSpy, times(1)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture())

        val capturedEvents = eventsCaptor.allValues
        val jsonString = capturedEvents[0][0]
        val event = JSONObject(jsonString)
        val expectedPayload: MutableMap<String, Any?> = LinkedTreeMap()
        expectedPayload["state"] = "unread"
        expectedPayload["messageId"] = "1"

        SwrveTestUtils.assertGenericEvent(event.toString(), null, GENERIC_EVENT_CAMPAIGN_TYPE_PIM, GENERIC_EVENT_ACTION_TYPE_PIM_DELETE, expectedPayload)
    }

    @Test
    fun testRead_restException() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(responseExceptionCode, RESPONSE_MODIFED) // use the special code to trigger an exception

        val message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
        assertNotNull(message)
        assertEquals(SwrvePushInboxMessageState.UNREAD, message!!.state)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.ERROR_UNKNOWN, result.resultCode)
                assertEquals("Push Inbox Message error marking as Read:Test Exception", result.errorMessage)
                assertEquals(SwrvePushInboxMessageState.UNREAD, message.state) // message should remain unread
                listenerCalled.set(true)
            }
        }

        SwrveSDK.readPushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        verify(swrveSpy, never()).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testDelete_restException() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        swrveSpy.pushInboxManager.restClient = dummyRestClient(responseExceptionCode, RESPONSE_MODIFED) // use the special code to trigger an exception

        var message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
        assertNotNull(message)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.ERROR_UNKNOWN, result.resultCode)
                assertEquals("Push Inbox Message error deleting:Test Exception", result.errorMessage)
                message = swrveSpy.pushInboxManager.getPushInboxMessage(1)
                assertNotNull(message) // message should not be deleted
                listenerCalled.set(true)
            }
        }

        SwrveSDK.deletePushInboxMessage(1, listener)
        await().untilTrue(listenerCalled)

        verify(swrveSpy, never()).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testRead_restRetry() { //test the restClient retry-on-error
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        val campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity!!, "push_inbox_messages.json")
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.init(mActivity)

        // Run using a rest client that simulates 2 failures, of 404s, and network delays in-between,
        // before returning the specified, expected error code to the test caller
        val restClient = retryRestClient(500)
        swrveSpy.pushInboxManager.restClient = spy(restClient)

        val listenerCalled = AtomicBoolean(false)
        val listener = object : SwrvePushInboxListener {
            override fun onComplete(messageId:Long, result: SwrvePushInboxListenerResult) {
                assertEquals(1, messageId)
                assertEquals(ResultCode.ERROR, result.resultCode)
                assertEquals(500, result.httpResponseCode)
                listenerCalled.set(true)
            }
        }

        SwrveSDK.readPushInboxMessage(1, listener)
        await().atLeast(Duration.ofSeconds(6)).untilTrue(listenerCalled)

        // verify that it took the restClient 3 attempts to get the expected error code
        verify(swrveSpy.pushInboxManager.restClient, times(3)).post(anyString(), anyString(), any(), anyString())
    }

    private fun dummyRestClient(responseCode: Int, responseBody: String): IRESTClient {
        return object : IRESTClient {
            override fun get(url: String, listener: IRESTResponseListener) {
                listener.onResponse(RESTResponse(responseCode, responseBody, null))
            }

            override fun get(url: String, params: Map<String, String>, listener: IRESTResponseListener) {
                listener.onResponse(RESTResponse(responseCode, responseBody, null))
            }

            override fun post(url: String, body: String, listener: IRESTResponseListener) {
                if (responseExceptionCode == responseCode) {
                    listener.onException(Exception("Test Exception"))
                    return
                } else {
                    listener.onResponse(RESTResponse(responseCode, responseBody, null))
                }
            }

            override fun post(url: String, body: String, listener: IRESTResponseListener, contentType: String) {
                if (responseExceptionCode == responseCode) {
                    listener.onException(Exception("Test Exception"))
                    return
                } else {
                    listener.onResponse(RESTResponse(responseCode, responseBody, null))
                }
            }
        }
    }

    private fun retryRestClient(responseCode: Int): IRESTClient {
        return object : IRESTClient {

            val DELAY = 2000L;
            val MAX_FAILURES = 3;

            var attempt = 1;

            private fun simulateNetworkDelay() {
                Thread.currentThread().join(DELAY)
            }
            private fun currentResponseCode() : Int {
                return if (attempt++ < MAX_FAILURES) 404 else responseCode
            }

            override fun get(url: String, listener: IRESTResponseListener) {
                simulateNetworkDelay()
                listener.onResponse(RESTResponse(currentResponseCode(), "", null))
            }

            override fun get(
                endpoint: String,
                params: MutableMap<String, String>,
                listener: IRESTResponseListener
            ) {
                simulateNetworkDelay()
                listener.onResponse(RESTResponse(currentResponseCode(), "", null))
            }

            override fun post(
                endpoint: String,
                encodedBody: String,
                listener: IRESTResponseListener
            ) {
                simulateNetworkDelay()
                listener.onResponse(RESTResponse(currentResponseCode(), "", null))
            }

            override fun post(
                endpoint: String,
                encodedBody: String,
                listener: IRESTResponseListener,
                contentType: String
            ) {
                simulateNetworkDelay()
                listener.onResponse(RESTResponse(currentResponseCode(), "", null))
            }
        }
    }
}
