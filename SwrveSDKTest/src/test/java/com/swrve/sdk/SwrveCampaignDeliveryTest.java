package com.swrve.sdk;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_DELIVERED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SwrveCampaignDeliveryTest extends SwrveBaseTest {

    ISwrveCommon swrveCommonSpy = SwrveCommon.getInstance();
    private int appId = 1;
    private String apiKey = "apiKey";
    private String batchUrl = "https://someendpoint.com";
    private String appVersion = "appversion";
    private String deviceId = "12345";
    private String userId = "12";
    private String sessionToken = "098123";


    @Before
    public void setUp() throws Exception {
        super.setUp();
        ISwrveCommon swrveCommonReal = (ISwrveCommon) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveCommonSpy = Mockito.spy(swrveCommonReal);
        SwrveCommon.setSwrveCommon(swrveCommonSpy);
        // Mock SwrveCommon.
        Mockito.doReturn(appId).when(swrveCommonSpy).getAppId();
        Mockito.doReturn(userId).when(swrveCommonSpy).getUserId();
        Mockito.doReturn(apiKey).when(swrveCommonSpy).getApiKey();
        Mockito.doReturn(sessionToken).when(swrveCommonSpy).getSessionKey();
        Mockito.doReturn(batchUrl).when(swrveCommonSpy).getEventsServer();
        Mockito.doReturn(appVersion).when(swrveCommonSpy).getAppVersion();
        Mockito.doReturn(deviceId).when(swrveCommonSpy).getDeviceId();
        Mockito.doReturn(batchUrl).when(swrveCommonSpy).getEventsServer();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        Mockito.reset(swrveCommonSpy);
    }

    @Test
    public void testSwrveCampaignEventString() throws Exception {
        String pushId = "1";
        Bundle mockedPushMsg = new Bundle();
        mockedPushMsg .putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, pushId);

        SwrveCampaignDeliveryAsyncTask deliveryNormalPush = new SwrveCampaignDeliveryAsyncTask(mockedPushMsg);
        String deliveryNormalPushEventString  = deliveryNormalPush.getEventData();
        this.testEventData(deliveryNormalPushEventString, false, pushId);


        Bundle mockedSilentPushMsg = new Bundle();
        mockedSilentPushMsg.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, pushId);

        SwrveCampaignDeliveryAsyncTask deliverySilentPush = new SwrveCampaignDeliveryAsyncTask(mockedSilentPushMsg );
        String silentPushEventString = deliverySilentPush.getEventData();
        this.testEventData(silentPushEventString, true, pushId);
    }

    private void testEventData(String eventString, boolean silentPush, String pushId) throws JSONException {
        JSONObject jObj = new JSONObject(eventString);

        assertTrue(jObj.get("type").equals(EVENT_TYPE_GENERIC_CAMPAIGN));
        assertTrue(jObj.has("time"));
        assertTrue(jObj.has("seqnum"));
        assertTrue(jObj.get("type").equals(EVENT_TYPE_GENERIC_CAMPAIGN));
        assertTrue(jObj.get(GENERIC_EVENT_ACTION_TYPE_KEY).equals(GENERIC_EVENT_ACTION_TYPE_DELIVERED));
        assertTrue(jObj.get("id").equals(pushId));
        JSONObject payload = jObj.getJSONObject("payload");
        assertEquals(Boolean.toString(silentPush), payload.get("silent"));
    }

    @Test
    public void testSendPushDelivery() throws JSONException {

        String expectedEndPoint = batchUrl + "/1/batch";

        String pushId = "1";
        Bundle mockedPushMsg = new Bundle();
        mockedPushMsg.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, pushId);

        SwrveCampaignDeliveryAsyncTask deliveryNormalPush = new SwrveCampaignDeliveryAsyncTask(mockedPushMsg);
        SwrveCampaignDeliveryAsyncTask deliveryNormalPushSpy = Mockito.spy(deliveryNormalPush);
        deliveryNormalPushSpy.doInBackground(null);

        ArgumentCaptor<String> endPointCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> batchEventCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(deliveryNormalPushSpy, Mockito.atLeastOnce()).sendPushDelivery(endPointCaptor.capture(), batchEventCaptor.capture());

        assertEquals(expectedEndPoint, endPointCaptor.getAllValues().get(0));
        JSONObject jObj = new JSONObject(batchEventCaptor.getAllValues().get(0));
        assertTrue(jObj.getString("app_version").equals(appVersion));
        assertTrue(jObj.getString("session_token").equals(sessionToken));
        assertTrue(jObj.get("version").equals("3"));
        assertTrue(jObj.get("user").equals(userId));
        assertTrue(jObj.get("unique_device_id").equals(deviceId));
    }
}
