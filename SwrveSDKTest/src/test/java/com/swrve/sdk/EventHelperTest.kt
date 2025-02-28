package com.swrve.sdk

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor


class EventHelperTest : SwrveBaseTest() {
    private var swrveCommonMock: ISwrveCommon? = null

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        swrveCommonMock = Mockito.mock(ISwrveCommon::class.java)
        Mockito.doReturn("some_app_version").`when`(swrveCommonMock)?.appVersion
        Mockito.doReturn("some_device_id").`when`(swrveCommonMock)?.deviceId
        Mockito.doReturn("some_session_key").`when`(swrveCommonMock)?.sessionKey
        Mockito.doReturn(1).`when`(swrveCommonMock)?.nextSequenceNumber
        SwrveCommon.setSwrveCommon(swrveCommonMock)
    }

    @Test
    @Throws(Exception::class)
    fun testPushDeliveredEventRegular() {
        val mockedPushMsg = Bundle()
        mockedPushMsg.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1")
        val deliveryNormalPushEventString =
            EventHelper.getPushDeliveredEvent(mockedPushMsg, 123, true, "")[0]
        assertPushDeliveredEvent(deliveryNormalPushEventString, false)
    }

    @Test
    @Throws(Exception::class)
    fun testPushDeliveredEventSilent() {
        val mockedSilentPushMsg = Bundle()
        mockedSilentPushMsg.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "1")
        val silentPushEventString =
            EventHelper.getPushDeliveredEvent(mockedSilentPushMsg, 123, false, "")[0]
        assertPushDeliveredEvent(silentPushEventString, true)
    }

    @Throws(JSONException::class)
    private fun assertPushDeliveredEvent(eventString: String, silentPush: Boolean) {
        val jObj = JSONObject(eventString)
        Assert.assertTrue(jObj["type"] == ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN)
        Assert.assertTrue(jObj["time"] == 123)
        Assert.assertTrue(jObj["seqnum"] == 1)
        Assert.assertTrue(jObj["type"] == ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN)
        Assert.assertTrue(jObj[ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_KEY] == ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_DELIVERED)
        Assert.assertTrue(jObj["id"] == "1")
        val payload = jObj.getJSONObject("payload")
        Assert.assertEquals(silentPush.toString(), payload["silent"])
    }

    @Test
    @Throws(Exception::class)
    fun testPushDeliveredBatchEventDisplayedTrue() {
        val bundle = Bundle()
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1")
        val eventsList = EventHelper.getPushDeliveredEvent(bundle, 123, true, "")
        val event = EventHelper.getPushDeliveredBatchEvent(eventsList)

        val expectedEvent =
        "{" + "\"session_token\":\"some_session_key\"," +
        "\"version\":\"3\"," +
        "\"app_version\":\"some_app_version\"," +
        "\"unique_device_id\":\"some_device_id\"," +
        "\"data\":" +
        "[{" +
        "\"type\":\"generic_campaign_event\"," +
        "\"time\":123,"+
        "\"seqnum\":1," +
        "\"actionType\":\"delivered\"," +
        "\"campaignType\":\"push\"," +
        "\"id\":\"1\"," +
        "\"payload\":{" +
        "\"displayed\":\"true\"," +
        "\"silent\":\"false\"" +
        "}" +
        "}]" +
        "}"
        Assert.assertEquals(expectedEvent, event)
    }

    @Test
    @Throws(Exception::class)
    fun testPushDeliveredBatchEventDisplayedFalse() {
        val bundle = Bundle()
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1")
        val eventsList = EventHelper.getPushDeliveredEvent(bundle, 123, false, "some_reason")
        val event = EventHelper.getPushDeliveredBatchEvent(eventsList)
        // @formatter:off
        val expectedEvent = 
        "{" + 
        "\"session_token\":\"some_session_key\"," + 
        "\"version\":\"3\"," + 
        "\"app_version\":\"some_app_version\"," + 
        "\"unique_device_id\":\"some_device_id\"," + 
        "\"data\":" + 
        "[" + 
        "{" + 
        "\"type\":\"generic_campaign_event\"," + 
        "\"time\":123," + 
        "\"seqnum\":1," + 
        "\"actionType\":\"delivered\"," + 
        "\"campaignType\":\"push\"," + 
        "\"id\":\"1\"," + 
        "\"payload\":{" + 
        "\"displayed\":\"false\"," + 
        "\"reason\":\"some_reason\"," + 
        "\"silent\":\"false\"" + 
        "}" + 
        "}" + 
        "]" + 
        "}"
                // @formatter:on
        Assert.assertEquals(expectedEvent, event)
    }

    @Test
    @Throws(Exception::class)
    fun testExtractEventFromBatch() {
        // @formatter:off
        val event = 
        "{" + 
        "\"type\":\"generic_campaign_event\"," + 
        "\"time\":123," + 
        "\"seqnum\":1," + 
        "\"actionType\":\"delivered\"," + 
        "\"campaignType\":\"push\"," + 
        "\"id\":\"1\"," + 
        "\"payload\":{" + 
        "\"silent\":\"false\"" + 
        "}" + 
        "}"
        val batchEvent = 
        "{" + 
        "\"session_token\":\"some_session_key\"," + 
        "\"version\":\"3\"," + 
        "\"app_version\":\"some_app_version\"," + 
        "\"unique_device_id\":\"some_device_id\"," + 
        "\"data\":" + 
        "[" + 
        event + 
        "]" + 
        "}"
        
                // @formatter:on
        val extractedEvent = EventHelper.extractEventFromBatch(batchEvent)
        Assert.assertEquals(event, extractedEvent)
    }

    @Test
    @Throws(Exception::class)
    fun testSendUninitiatedDeviceUpdateEvent() {
        val deviceUpdateAttributes = JSONObject()
        deviceUpdateAttributes.put("testkey1", "testvalue1")
        EventHelper.sendUninitiatedDeviceUpdateEvent(
            ApplicationProvider.getApplicationContext(),
            "userId",
            deviceUpdateAttributes
        )

        val contextCaptor = argumentCaptor<Context>()

        val userIdStringCaptor = argumentCaptor<String>()

        val events = argumentCaptor<ArrayList<String>>()

        Mockito.verify(swrveCommonMock, Mockito.atLeastOnce())?.sendEventsInBackground(
            contextCaptor.capture(),
            userIdStringCaptor.capture(),
            events.capture()
        )

        val capturedProperties = events.allValues
        val jsonString = capturedProperties[0][0].toString()
        val jsonObject = JSONObject(jsonString)

        Assert.assertTrue(jsonObject.has("time"))
        Assert.assertTrue(jsonObject.has("seqnum"))
        Assert.assertEquals("device_update", jsonObject["type"])
        Assert.assertEquals("false", jsonObject["user_initiated"])
        Assert.assertTrue(jsonObject.has("attributes"))
        val attributes = jsonObject["attributes"] as JSONObject
        Assert.assertTrue(attributes.length() == 1)
        Assert.assertEquals("testvalue1", attributes["testkey1"])
    }
}
