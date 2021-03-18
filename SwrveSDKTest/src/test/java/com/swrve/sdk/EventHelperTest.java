package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_DELIVERED;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class EventHelperTest extends SwrveBaseTest {

    private ISwrveCommon swrveCommonMock;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveCommonMock = mock(ISwrveCommon.class);
        Mockito.doReturn("some_app_version").when(swrveCommonMock).getAppVersion();
        Mockito.doReturn("some_device_id").when(swrveCommonMock).getDeviceId();
        Mockito.doReturn("some_session_key").when(swrveCommonMock).getSessionKey();
        Mockito.doReturn(1).when(swrveCommonMock).getNextSequenceNumber();
        SwrveCommon.setSwrveCommon(swrveCommonMock);
    }

    @Test
    public void testPushDeliveredEventRegular() throws Exception {
        Bundle mockedPushMsg = new Bundle();
        mockedPushMsg.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        String deliveryNormalPushEventString = EventHelper.getPushDeliveredEvent(mockedPushMsg, 123).get(0);
        assertPushDeliveredEvent(deliveryNormalPushEventString, false);
    }

    @Test
    public void testPushDeliveredEventSilent() throws Exception {
        Bundle mockedSilentPushMsg = new Bundle();
        mockedSilentPushMsg.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "1");
        String silentPushEventString = EventHelper.getPushDeliveredEvent(mockedSilentPushMsg, 123).get(0);
        assertPushDeliveredEvent(silentPushEventString, true);
    }

    private void assertPushDeliveredEvent(String eventString, boolean silentPush) throws JSONException {
        JSONObject jObj = new JSONObject(eventString);
        assertTrue(jObj.get("type").equals(EVENT_TYPE_GENERIC_CAMPAIGN));
        assertTrue(jObj.get("time").equals(123));
        assertTrue(jObj.get("seqnum").equals(1));
        assertTrue(jObj.get("type").equals(EVENT_TYPE_GENERIC_CAMPAIGN));
        assertTrue(jObj.get(GENERIC_EVENT_ACTION_TYPE_KEY).equals(GENERIC_EVENT_ACTION_TYPE_DELIVERED));
        assertTrue(jObj.get("id").equals("1"));
        JSONObject payload = jObj.getJSONObject("payload");
        assertEquals(Boolean.toString(silentPush), payload.get("silent"));
    }

    @Test
    public void testPushDeliveredBatchEvent() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        ArrayList<String> eventsList = EventHelper.getPushDeliveredEvent(bundle, 123);
        String event = EventHelper.getPushDeliveredBatchEvent(eventsList);
        // @formatter:off
        String expectedEvent =
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
                                "\"silent\":\"false\"" +
                            "}" +
                        "}" +
                    "]" +
                "}";
        // @formatter:on
        assertEquals(expectedEvent, event);
    }

    @Test
    public void testExtractEventFromBatch() throws Exception {
        // @formatter:off
        String event =
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
        String batchEvent =
                "{" +
                    "\"session_token\":\"some_session_key\"," +
                    "\"version\":\"3\"," +
                    "\"app_version\":\"some_app_version\"," +
                    "\"unique_device_id\":\"some_device_id\"," +
                    "\"data\":" +
                    "[" +
                        event +
                    "]" +
                "}";
        // @formatter:on

        String extractedEvent = EventHelper.extractEventFromBatch(batchEvent);
        assertEquals(event, extractedEvent);
    }

    @Test
    public void testSendUninitiatedDeviceUpdateEvent() throws Exception {

        JSONObject deviceUpdateAttributes = new JSONObject();
        deviceUpdateAttributes.put("testkey1", "testvalue1");
        EventHelper.sendUninitiatedDeviceUpdateEvent(ApplicationProvider.getApplicationContext(), "userId", deviceUpdateAttributes);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> events = ArgumentCaptor.forClass(ArrayList.class);
        Mockito.verify(swrveCommonMock, Mockito.atLeastOnce()).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), events.capture());

        List<ArrayList> capturedProperties = events.getAllValues();
        String jsonString = capturedProperties.get(0).get(0).toString();
        JSONObject jsonObject = new JSONObject(jsonString);

        assertTrue(jsonObject.has("time"));
        assertTrue(jsonObject.has("seqnum"));
        assertEquals("device_update", jsonObject.get("type"));
        assertEquals("false", jsonObject.get("user_initiated"));
        assertTrue(jsonObject.has("attributes"));
        JSONObject attributes = (JSONObject) jsonObject.get("attributes");
        assertTrue(attributes.length() == 1);
        assertEquals("testvalue1", attributes.get("testkey1"));
    }
}
