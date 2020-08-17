package com.swrve.sdk;

import com.swrve.sdk.localstorage.LocalStorage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SwrveSingleThreadedTests extends SwrveBaseTest {

    private Swrve swrveSpy;
    IRESTClient restClientMock = Mockito.mock(IRESTClient.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();
        SwrveTestUtils.disableRestClientExecutor(swrveSpy);
        swrveSpy.restClient = restClientMock;

        SwrveTestUtils.runSingleThreaded(swrveSpy);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testFullInitialization() throws Exception {

        SwrveEventsManager eventsManagerMock = mock(SwrveEventsManager.class);
        doReturn(eventsManagerMock).when(swrveSpy).getSwrveEventsManager(anyString(), anyString(), anyString());
        doReturn(123l).when(swrveSpy).getSessionTime();

        swrveSpy.init(mActivity);

        // 3 events are sent are part of init so seqnum is 3
        String seqnum = swrveSpy.multiLayerLocalStorage.getCacheEntry(swrveSpy.getUserId(), "seqnum");
        assertEquals(3, Integer.parseInt(seqnum));

        // 2 events queued via queueEvent method
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> parametersMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> payloadMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Boolean> triggerEventListenerCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(swrveSpy, times(2)).queueEvent(userIdStringCaptor.capture(),
                eventStringCaptor.capture(), parametersMapCaptor.capture(), payloadMapCaptor.capture(), triggerEventListenerCaptor.capture());

        // Swrve.first_session event
        assertEquals(swrveSpy.getUserId(), userIdStringCaptor.getAllValues().get(0));
        assertEquals("event", eventStringCaptor.getAllValues().get(0));
        assertEquals("Swrve.first_session", parametersMapCaptor.getAllValues().get(0).get("name"));
        assertNull(payloadMapCaptor.getAllValues().get(0));
        assertTrue(triggerEventListenerCaptor.getAllValues().get(0));

        // device_update event
        assertEquals(swrveSpy.getUserId(), userIdStringCaptor.getAllValues().get(1));
        assertEquals("device_update", eventStringCaptor.getAllValues().get(1));
        JSONObject deviceUpdateJsonObject = (JSONObject) parametersMapCaptor.getAllValues().get(1).get("attributes");
        assertNotNull(deviceUpdateJsonObject);
        assertTrue(deviceUpdateJsonObject.has("swrve.timezone_name"));
        assertTrue(deviceUpdateJsonObject.has("swrve.utc_offset_seconds"));
        assertNull(payloadMapCaptor.getAllValues().get(1));
        assertTrue(triggerEventListenerCaptor.getAllValues().get(1));

        // 1 event sent via sendSessionStart
        verify(swrveSpy, times(1)).sendSessionStart(123);

        // queueDeviceUpdateNow is called in init --> which calls sendQueuedEvents
        verify(eventsManagerMock, times(1)).sendStoredEvents(swrveSpy.multiLayerLocalStorage);

        // sendSessionStart is called in init --> which calls storeAndSendEvents
        verify(eventsManagerMock, times(1)).storeAndSendEvents(anyList(), any(LocalStorage.class));

        assertNotNull(swrveSpy.getInitialisedTime());
        assertTrue(SwrveHelper.isNotNullOrEmpty(swrveSpy.multiLayerLocalStorage.getCacheEntry("", "device_id")));
    }

    @Test
    public void testAllEventMethods() throws Exception {

        SwrveEventsManager eventsManagerMock = mock(SwrveEventsManager.class);
        doReturn(eventsManagerMock).when(swrveSpy).getSwrveEventsManager(anyString(), anyString(), anyString());
        doReturn(123l).when(swrveSpy).getSessionTime();

        swrveSpy.init(mActivity);

        SwrveIAPRewards rewards = new SwrveIAPRewards();
        rewards.addCurrency("gold", 200);
        rewards.addItem("sword", 1);

        SwrveSDK.sessionStart(); // adds userUpdate too
        SwrveSDK.currencyGiven("givenCurrency", 999);
        SwrveSDK.purchase("item", "currency", 999, 10);
        SwrveSDK.iap(1, "com.swrve.productid1", 0.99, "USD");
        SwrveSDK.iap(1, "com.swrve.productid2", 1.99, "EUR", rewards); // IAP with rewards
        SwrveSDK.event("generic_event", null);
        SwrveSDK.userUpdate(new HashMap<>());
        SwrveSDK.sessionEnd();

        String seqnum = swrveSpy.multiLayerLocalStorage.getCacheEntry(swrveSpy.getUserId(), "seqnum");
        assertEquals(11, Integer.parseInt(seqnum));

        // 9 events queued via queueEvent method
        verify(swrveSpy, times(9)).queueEvent(anyString(), anyString(), anyMap(), anyMap(), anyBoolean());

        // 2 events queued via sendSessionStart (via init and SwrveSDK.sessionStart())
        verify(swrveSpy, times(2)).sendSessionStart(123);
    }

    @Test
    public void testBatchSendTest() throws Exception {

        swrveSpy.onCreate(mActivity);

        SwrveSDK.sessionEnd();
        SwrveSDK.event("generic_event_0", null);
        SwrveSDK.event("generic_event_1", null);
        SwrveSDK.event("generic_event_2", null);
        SwrveSDK.sendQueuedEvents();

        // three batches sent:
        // - batch0 containing session_start
        // - batch1 containing session_start + device_update
        // - batch2 containing session_start + device_update + 4 queued events
        ArgumentCaptor<String> endpointStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<IRESTResponseListener> callbackIRESTResponseListenerCaptor = ArgumentCaptor.forClass(IRESTResponseListener.class);
        verify(restClientMock, times(3)).post(endpointStringCaptor.capture(), bodyStringCaptor.capture(), callbackIRESTResponseListenerCaptor.capture());

        // batch0
        JSONArray data0 = new JSONObject(bodyStringCaptor.getAllValues().get(0)).getJSONArray("data");
        assertEquals("session_start", data0.getJSONObject(0).getString("type"));

        // batch1
        JSONArray data1 = new JSONObject(bodyStringCaptor.getAllValues().get(1)).getJSONArray("data");
        assertEquals("session_start", data1.getJSONObject(0).getString("type"));
        assertEquals("Swrve.first_session", data1.getJSONObject(1).getString("name"));
        assertEquals("device_update", data1.getJSONObject(2).getString("type"));

        // batch2
        JSONArray data2 = new JSONObject(bodyStringCaptor.getAllValues().get(2)).getJSONArray("data");
        assertEquals("session_start", data2.getJSONObject(0).getString("type"));
        assertEquals("Swrve.first_session", data2.getJSONObject(1).getString("name"));
        assertEquals("device_update", data2.getJSONObject(2).getString("type"));
        assertEquals("session_end", data2.getJSONObject(3).getString("type"));
        assertEquals("generic_event_0", data2.getJSONObject(4).getString("name"));
        assertEquals("generic_event_1", data2.getJSONObject(5).getString("name"));
        assertEquals("generic_event_2", data2.getJSONObject(6).getString("name"));
    }
}
