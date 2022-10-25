package com.swrve.sdk;

import static com.swrve.sdk.CampaignDeliveryManager.KEY_BODY;
import static com.swrve.sdk.CampaignDeliveryManager.KEY_END_POINT;
import static com.swrve.sdk.CampaignDeliveryManager.MAX_ATTEMPTS;
import static com.swrve.sdk.CampaignDeliveryManager.REST_CLIENT_TIMEOUT_MILLIS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.impl.model.WorkSpec;

import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.RESTResponse;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class CampaignDeliveryManagerTest extends SwrveBaseTest {

    private String testEndpoint = "https://someendpoint.com";
    // @formatter:off
    private String testEvent =
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
            "}";
    private String testBatchEvent =
            "{" +
                "\"session_token\":\"some_session_key\"," +
                "\"version\":\"3\"," +
                "\"app_version\":\"some_app_version\"," +
                "\"unique_device_id\":\"some_device_id\"," +
                "\"data\":" +
                "[" +
                    testEvent +
                "]" +
            "}";
    // @formatter:on

    @Test
    public void testPostBadData() {

        CampaignDeliveryManager deliveryManagerSpy = spy(new CampaignDeliveryManager(mActivity));
        IRESTClient mockRestClient = mock(IRESTClient.class);
        doReturn(mockRestClient).when(deliveryManagerSpy).getRestClient(REST_CLIENT_TIMEOUT_MILLIS);

        Data invalidInputData = new Data.Builder().build();
        ListenableWorker.Result result = deliveryManagerSpy.post(invalidInputData, 0);

        verify(mockRestClient, never()).post(anyString(), anyString(), any(CampaignDeliveryManager.RESTResponseListener.class));
        assertEquals(ListenableWorker.Result.failure(), result); // default result is failure
    }

    @Test
    public void testPostMaxedAttempts() {

        CampaignDeliveryManager deliveryManagerSpy = spy(new CampaignDeliveryManager(mActivity));
        IRESTClient mockRestClient = mock(IRESTClient.class);
        doReturn(mockRestClient).when(deliveryManagerSpy).getRestClient(REST_CLIENT_TIMEOUT_MILLIS);

        Data invalidInputData = new Data.Builder().build();
        ListenableWorker.Result result = deliveryManagerSpy.post(invalidInputData, 3);

        verify(mockRestClient, never()).post(anyString(), anyString(), any(CampaignDeliveryManager.RESTResponseListener.class));
        assertEquals(ListenableWorker.Result.failure(), result); // default result is failure
    }

    @Test
    public void testPost() {

        int runAttempt = 0;
        int runNumber = 1;
        CampaignDeliveryManager deliveryManagerSpy = spy(new CampaignDeliveryManager(mActivity));
        IRESTClient mockRestClient = mock(IRESTClient.class);
        doReturn(mockRestClient).when(deliveryManagerSpy).getRestClient(REST_CLIENT_TIMEOUT_MILLIS);
        CampaignDeliveryManager.RESTResponseListener restResponseListener = deliveryManagerSpy.new RESTResponseListener(runNumber, testBatchEvent);
        doReturn(restResponseListener).when(deliveryManagerSpy).getRestResponseListener(runNumber, testBatchEvent);

        Data inputData = new Data.Builder()
                .putString(KEY_END_POINT, testEndpoint)
                .putString(KEY_BODY, testBatchEvent)
                .build();
        ListenableWorker.Result result = deliveryManagerSpy.post(inputData, runAttempt);

        verify(mockRestClient, atLeastOnce()).post(testEndpoint, testBatchEvent, restResponseListener);
        assertEquals(ListenableWorker.Result.failure(), result); // default result is failure
    }

    @Test
    public void testPostWithRunNumber() {

        int runAttempt = 1;
        int runNumber = 2;

        CampaignDeliveryManager deliveryManagerSpy = spy(new CampaignDeliveryManager(mActivity));
        IRESTClient mockRestClient = mock(IRESTClient.class);
        doReturn(mockRestClient).when(deliveryManagerSpy).getRestClient(REST_CLIENT_TIMEOUT_MILLIS);
        CampaignDeliveryManager.RESTResponseListener restResponseListener = deliveryManagerSpy.new RESTResponseListener(runNumber, testBatchEvent);
        doReturn(restResponseListener).when(deliveryManagerSpy).getRestResponseListener(anyInt(), anyString());

        // post with testBatchEvent which does not contain a runNumber. The verify method checks that mockRestClient is called with json that contains runNumber
        Data inputData = new Data.Builder()
                .putString(KEY_END_POINT, testEndpoint)
                .putString(KEY_BODY, testBatchEvent)
                .build();
        ListenableWorker.Result result = deliveryManagerSpy.post(inputData, runAttempt);

        // Below event is same as testBatchEvent but with runNumber:2 in the payload of the event
        String expectedBatchEventWithRunNumber = "{\"session_token\":\"some_session_key\",\"version\":\"3\",\"app_version\":\"some_app_version\",\"unique_device_id\":\"some_device_id\",\"data\":[{\"type\":\"generic_campaign_event\",\"time\":123,\"seqnum\":1,\"actionType\":\"delivered\",\"campaignType\":\"push\",\"id\":\"1\",\"payload\":{\"silent\":\"false\",\"runNumber\":2}}]}";
        verify(mockRestClient, atLeastOnce()).post(testEndpoint, expectedBatchEventWithRunNumber, restResponseListener);
        assertEquals(ListenableWorker.Result.failure(), result); // default result is failure
    }

    @Test
    public void testRESTResponseListenerSuccess() {
        CampaignDeliveryManager deliveryManagerSpy = spy(new CampaignDeliveryManager(mActivity));
        CampaignDeliveryManager.RESTResponseListener restResponseListenerSpy = spy(deliveryManagerSpy.new RESTResponseListener(1, testBatchEvent));
        RESTResponse successRestResponse = new RESTResponse(200, "", null);
        restResponseListenerSpy.onResponse(successRestResponse);
        assertEquals(ListenableWorker.Result.success(), restResponseListenerSpy.result);
        verify(deliveryManagerSpy, never()).saveEvent(anyString(), anyInt());
        verify(deliveryManagerSpy, atLeastOnce()).sendQaEvent(testBatchEvent);
    }

    @Test
    public void testRESTResponseListenerUserError() {
        CampaignDeliveryManager deliveryManagerSpy = spy(new CampaignDeliveryManager(mActivity));
        CampaignDeliveryManager.RESTResponseListener restResponseListenerSpy = spy(deliveryManagerSpy.new RESTResponseListener(1, testBatchEvent));
        RESTResponse successRestResponse = new RESTResponse(400, "", null);
        restResponseListenerSpy.onResponse(successRestResponse);
        assertEquals(ListenableWorker.Result.failure(), restResponseListenerSpy.result);
        verify(deliveryManagerSpy, never()).saveEvent(anyString(), anyInt());
    }

    @Test
    public void testRESTResponseListenerServerError() {
        CampaignDeliveryManager deliveryManagerSpy = spy(new CampaignDeliveryManager(mActivity));
        CampaignDeliveryManager.RESTResponseListener restResponseListenerSpy = spy(deliveryManagerSpy.new RESTResponseListener(1, testBatchEvent));
        RESTResponse successRestResponse = new RESTResponse(501, "", null);
        restResponseListenerSpy.onResponse(successRestResponse);
        assertEquals(ListenableWorker.Result.retry(), restResponseListenerSpy.result);
        verify(deliveryManagerSpy, never()).saveEvent(anyString(), anyInt());
    }

    @Test
    public void testRESTResponseListenerServerErrorMax() {

        SwrveSDK.createInstance(mActivity.getApplication(), 1, "apiKey");

        CampaignDeliveryManager deliveryManagerSpy = spy(new CampaignDeliveryManager(mActivity));
        for(int runNumber = 1; runNumber < 4; runNumber ++) {
            CampaignDeliveryManager.RESTResponseListener restResponseListenerSpy = spy(deliveryManagerSpy.new RESTResponseListener(runNumber, testBatchEvent));
            RESTResponse successRestResponse = new RESTResponse(501, "", null);
            restResponseListenerSpy.onResponse(successRestResponse);
            if(runNumber == MAX_ATTEMPTS) {
                assertEquals(ListenableWorker.Result.failure(), restResponseListenerSpy.result);
            } else {
                assertEquals(ListenableWorker.Result.retry(), restResponseListenerSpy.result);
            }
        }

        verify(deliveryManagerSpy, atLeastOnce()).saveEvent(testBatchEvent, MAX_ATTEMPTS + 1);
        SQLiteLocalStorage localStorage = new SQLiteLocalStorage(mActivity, SwrveSDK.getConfig().getDbName(), SwrveSDK.getConfig().getMaxSqliteDbSize());
        LinkedHashMap<Long, String> eventsStored = localStorage.getFirstNEvents(50, SwrveSDK.getUserId());
        // The runNumber is now 4 when saved to db
        String testEventWithRunNumber4 = "{\"type\":\"generic_campaign_event\",\"time\":123,\"seqnum\":1,\"actionType\":\"delivered\",\"campaignType\":\"push\",\"id\":\"1\",\"payload\":{\"silent\":\"false\",\"runNumber\":4}}";
        assertTrue(eventsStored.containsValue(testEventWithRunNumber4));
    }

    @Test
    public void testGetRestWorkRequest() {

        CampaignDeliveryManager deliveryManagerSpy = spy(new CampaignDeliveryManager(mActivity));
        OneTimeWorkRequest workRequest = deliveryManagerSpy.getRestWorkRequest(testEndpoint, testBatchEvent);

        WorkSpec workSpec = workRequest.getWorkSpec();
        assertEquals(NetworkType.CONNECTED, workSpec.constraints.getRequiredNetworkType());
        assertEquals(testEndpoint, workSpec.input.getString(KEY_END_POINT));
        assertEquals(testBatchEvent, workSpec.input.getString(KEY_BODY));
        assertEquals(BackoffPolicy.LINEAR, workSpec.backoffPolicy);
        assertEquals(1000*60*60, workSpec.backoffDelayDuration);
    }

    @Test
    public void testSendCampaignDelivery() {

        CampaignDeliveryManager deliveryManagerSpy = spy(new CampaignDeliveryManager(mActivity));
        doNothing().when(deliveryManagerSpy).enqueueUniqueWork(any(Context.class), anyString(), any(OneTimeWorkRequest.class));

        deliveryManagerSpy.sendCampaignDelivery("uniqueWorkName", testEndpoint, testBatchEvent);

        verify(deliveryManagerSpy, atLeastOnce()).getRestWorkRequest(testEndpoint, testBatchEvent);
        verify(deliveryManagerSpy, atLeastOnce()).enqueueUniqueWork(any(Context.class), anyString(), any(OneTimeWorkRequest.class));
    }

    @Test
    public void testSendQaEvent() throws Exception {
        QaUser qaUserMock = mock(QaUser.class);
        QaUser.instance = qaUserMock;
        CampaignDeliveryManager deliveryManager = new CampaignDeliveryManager(mActivity);
        deliveryManager.sendQaEvent(testBatchEvent);
        List expectedEvents = new ArrayList();
        expectedEvents.add(testEvent);
        verify(qaUserMock, atLeastOnce())._wrappedEvents(expectedEvents);
    }
}

