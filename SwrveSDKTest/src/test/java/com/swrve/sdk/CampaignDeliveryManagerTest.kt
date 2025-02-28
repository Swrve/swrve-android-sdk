package com.swrve.sdk

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import com.swrve.sdk.localstorage.SQLiteLocalStorage
import com.swrve.sdk.rest.IRESTClient
import com.swrve.sdk.rest.RESTResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.kotlin.verify

class CampaignDeliveryManagerTest : SwrveBaseTest() {
    private val testEndpoint = "https://someendpoint.com"

    // @formatter:off
    private val testEvent = "{" +
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
    private val testBatchEvent = "{" +
                "\"session_token\":\"some_session_key\"," +
                "\"version\":\"3\"," +
                "\"app_version\":\"some_app_version\"," +
                "\"unique_device_id\":\"some_device_id\"," +
                "\"data\":" +
                "[" +
                    testEvent +
                "]" +
            "}"
    // @formatter:on

    @Test
    fun testPostBadData() {
        val deliveryManagerSpy = spy(CampaignDeliveryManager(mActivity))
        val mockRestClient = mock(IRESTClient::class.java)
        doReturn(mockRestClient).`when`(deliveryManagerSpy).getRestClient(CampaignDeliveryManager.REST_CLIENT_TIMEOUT_MILLIS)

        val invalidInputData = Data.Builder().build()
        val result = deliveryManagerSpy.post(invalidInputData, 0)

        verify(mockRestClient, never()).post(anyString(), anyString(), any(CampaignDeliveryManager.RESTResponseListener::class.java))
        assertEquals(ListenableWorker.Result.failure(), result) // default result is failure
    }

    @Test
    fun testPostMaxedAttempts() {
        val deliveryManagerSpy = spy(CampaignDeliveryManager(mActivity))
        val mockRestClient = mock(IRESTClient::class.java)
        doReturn(mockRestClient).`when`(deliveryManagerSpy).getRestClient(CampaignDeliveryManager.REST_CLIENT_TIMEOUT_MILLIS)

        val invalidInputData = Data.Builder().build()
        val result = deliveryManagerSpy.post(invalidInputData, 3)

        verify(mockRestClient, never()).post(anyString(), anyString(), any(CampaignDeliveryManager.RESTResponseListener::class.java))
        assertEquals(ListenableWorker.Result.failure(), result) // default result is failure
    }

    @Test
    fun testPost() {
        val runAttempt = 0
        val runNumber = 1
        val deliveryManagerSpy = spy(CampaignDeliveryManager(mActivity))
        val mockRestClient = mock(IRESTClient::class.java)
        doReturn(mockRestClient).`when`(deliveryManagerSpy).getRestClient(CampaignDeliveryManager.REST_CLIENT_TIMEOUT_MILLIS)
        val restResponseListener = deliveryManagerSpy.RESTResponseListener(runNumber, testBatchEvent)
        doReturn(restResponseListener).`when`(deliveryManagerSpy).getRestResponseListener(runNumber, testBatchEvent)

        val inputData = Data.Builder()
            .putString(CampaignDeliveryManager.KEY_END_POINT, testEndpoint)
            .putString(CampaignDeliveryManager.KEY_BODY, testBatchEvent)
            .build()
        val result = deliveryManagerSpy.post(inputData, runAttempt)

        verify(mockRestClient, atLeastOnce()).post(testEndpoint, testBatchEvent, restResponseListener)
        assertEquals(ListenableWorker.Result.failure(), result) // default result is failure
    }

    @Test
    fun testPostWithRunNumber() {
        val runAttempt = 1
        val runNumber = 2

        val deliveryManagerSpy = spy(CampaignDeliveryManager(mActivity))
        val mockRestClient = mock(IRESTClient::class.java)
        doReturn(mockRestClient).`when`(deliveryManagerSpy).getRestClient(CampaignDeliveryManager.REST_CLIENT_TIMEOUT_MILLIS)
        val restResponseListener = deliveryManagerSpy.RESTResponseListener(runNumber, testBatchEvent)
        doReturn(restResponseListener).`when`(deliveryManagerSpy).getRestResponseListener(anyInt(), anyString())

        // post with testBatchEvent which does not contain a runNumber. The verify method checks that mockRestClient is called with json that contains runNumber
        val inputData = Data.Builder()
            .putString(CampaignDeliveryManager.KEY_END_POINT, testEndpoint)
            .putString(CampaignDeliveryManager.KEY_BODY, testBatchEvent)
            .build()
        val result = deliveryManagerSpy.post(inputData, runAttempt)

        // Below event is same as testBatchEvent but with runNumber:2 in the payload of the event
        val expectedBatchEventWithRunNumber =
            "{\"session_token\":\"some_session_key\",\"version\":\"3\",\"app_version\":\"some_app_version\",\"unique_device_id\":\"some_device_id\",\"data\":[{\"type\":\"generic_campaign_event\",\"time\":123,\"seqnum\":1,\"actionType\":\"delivered\",\"campaignType\":\"push\",\"id\":\"1\",\"payload\":{\"silent\":\"false\",\"runNumber\":2}}]}"
        verify(mockRestClient, atLeastOnce()).post(testEndpoint, expectedBatchEventWithRunNumber, restResponseListener)
        assertEquals(ListenableWorker.Result.failure(), result) // default result is failure
    }

    @Test
    fun testRESTResponseListenerSuccess() {
        val deliveryManagerSpy = spy(CampaignDeliveryManager(mActivity))
        val restResponseListenerSpy = spy(deliveryManagerSpy.RESTResponseListener(1, testBatchEvent))
        val successRestResponse = RESTResponse(200, "", null)
        restResponseListenerSpy.onResponse(successRestResponse)
        assertEquals(ListenableWorker.Result.success(), restResponseListenerSpy.result)
        verify(deliveryManagerSpy, never()).saveEvent(anyString(), anyInt())
        verify(deliveryManagerSpy, atLeastOnce()).sendQaEvent(testBatchEvent)
    }

    @Test
    fun testRESTResponseListenerUserError() {
        val deliveryManagerSpy = spy(CampaignDeliveryManager(mActivity))
        val restResponseListenerSpy = spy(deliveryManagerSpy.RESTResponseListener(1, testBatchEvent))
        val successRestResponse = RESTResponse(400, "", null)
        restResponseListenerSpy.onResponse(successRestResponse)
        assertEquals(ListenableWorker.Result.failure(), restResponseListenerSpy.result)
        verify(deliveryManagerSpy, never()).saveEvent(anyString(), anyInt())
    }

    @Test
    fun testRESTResponseListenerServerError() {
        val deliveryManagerSpy = spy(CampaignDeliveryManager(mActivity))
        val restResponseListenerSpy = spy(deliveryManagerSpy.RESTResponseListener(1, testBatchEvent))
        val successRestResponse = RESTResponse(501, "", null)
        restResponseListenerSpy.onResponse(successRestResponse)
        assertEquals(ListenableWorker.Result.retry(), restResponseListenerSpy.result)
        verify(deliveryManagerSpy, never()).saveEvent(anyString(), anyInt())
    }

    @Test
    fun testRESTResponseListenerServerErrorMax() {
        SwrveSDK.createInstance(mActivity!!.application, 1, "apiKey")

        val deliveryManagerSpy = spy(CampaignDeliveryManager(mActivity))
        for (runNumber in 1..3) {
            val restResponseListenerSpy = spy(deliveryManagerSpy.RESTResponseListener(runNumber, testBatchEvent))
            val successRestResponse = RESTResponse(501, "", null)
            restResponseListenerSpy.onResponse(successRestResponse)
            if (runNumber == CampaignDeliveryManager.MAX_ATTEMPTS) {
                assertEquals(ListenableWorker.Result.failure(), restResponseListenerSpy.result)
            } else {
                assertEquals(ListenableWorker.Result.retry(), restResponseListenerSpy.result)
            }
        }

        verify(deliveryManagerSpy, atLeastOnce()).saveEvent(testBatchEvent, CampaignDeliveryManager.MAX_ATTEMPTS + 1)
        val localStorage = SQLiteLocalStorage(mActivity, SwrveSDK.getConfig().dbName, SwrveSDK.getConfig().maxSqliteDbSize)
        val eventsStored = localStorage.getFirstNEvents(50, SwrveSDK.getUserId())
        // The runNumber is now 4 when saved to db
        val testEventWithRunNumber4 =
            "{\"type\":\"generic_campaign_event\",\"time\":123,\"seqnum\":1,\"actionType\":\"delivered\",\"campaignType\":\"push\",\"id\":\"1\",\"payload\":{\"silent\":\"false\",\"runNumber\":4}}"
        assertTrue(eventsStored.containsValue(testEventWithRunNumber4))
    }

    @Test
    fun testGetRestWorkRequest() {
        val deliveryManagerSpy = spy(CampaignDeliveryManager(mActivity))
        val workRequest = deliveryManagerSpy.getRestWorkRequest(testEndpoint, testBatchEvent)

        val workSpec = workRequest.workSpec
        assertEquals(NetworkType.CONNECTED, workSpec.constraints.requiredNetworkType)
        assertEquals(testEndpoint, workSpec.input.getString(CampaignDeliveryManager.KEY_END_POINT))
        assertEquals(testBatchEvent, workSpec.input.getString(CampaignDeliveryManager.KEY_BODY))
        assertEquals(BackoffPolicy.LINEAR, workSpec.backoffPolicy)
        assertEquals((1000 * 60 * 60).toLong(), workSpec.backoffDelayDuration)
    }

    @Test
    fun testSendCampaignDelivery() {
        val deliveryManagerSpy = spy(CampaignDeliveryManager(mActivity))
        doNothing().`when`(deliveryManagerSpy).enqueueUniqueWork(any(Context::class.java), anyString(), any(OneTimeWorkRequest::class.java))

        deliveryManagerSpy.sendCampaignDelivery("uniqueWorkName", testEndpoint, testBatchEvent)

        verify(deliveryManagerSpy, atLeastOnce()).getRestWorkRequest(testEndpoint, testBatchEvent)
        verify(deliveryManagerSpy, atLeastOnce()).enqueueUniqueWork(any(Context::class.java), anyString(), any(OneTimeWorkRequest::class.java))
    }

    @Test
    fun testSendQaEvent() {
        val qaUserMock = mock(QaUser::class.java)
        QaUser.instance = qaUserMock
        val deliveryManager = CampaignDeliveryManager(mActivity)
        deliveryManager.sendQaEvent(testBatchEvent)
        val expectedEvents: MutableList<String> = mutableListOf()
        expectedEvents.add(testEvent)
        verify(qaUserMock, atLeastOnce())._wrappedEvents(expectedEvents)
    }
}
