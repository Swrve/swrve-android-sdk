package com.swrve.sdk;

import com.swrve.sdk.localstorage.LocalStorage;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageListener;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SwrveSingleThreadedTests extends SwrveBaseTest {

    private Swrve swrveSpy;
    private IRESTClient restClientMock = Mockito.mock(IRESTClient.class);
    private int messageShownId;

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
        verify(swrveSpy, times(9)).queueEvent(anyString(), anyString(), nullable(Map.class), nullable(Map.class), anyBoolean());

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

    @Test
    public void testDeviceIDUniqueBetweenInstances() throws Exception {

        String deviceID = swrveSpy.getDeviceId();
        assertNotNull(deviceID);

        // shutdown current instance and call setup again to recreate it
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        String swrve1Instance = ((Object)swrveSpy).toString();
        setUp();
        String swrve2Instance = ((Object)swrveSpy).toString();
        assertNotEquals(swrve1Instance, swrve2Instance); // verify its a new instance

        assertEquals(deviceID, swrveSpy.getDeviceId());
    }

    @Test
    public void testFlush() {
        int eventsAddedOnInit = 3;
        String userId = swrveSpy.getUserId();
        swrveSpy.onCreate(mActivity);
        for (int i = 0; i < 50; i++) {
            swrveSpy.sessionEnd();
            swrveSpy.multiLayerLocalStorage.getPrimaryStorage().setCacheEntry(userId, "category" + i, "rawData" + i);
        }

        // 50 events plus firstsession event, and user device prop event
        assertEquals(50 + eventsAddedOnInit, getAllEventsInPrimaryStorage(userId).size());
        swrveSpy.flushToDisk();
        // Data has been moved to the other storage
        assertEquals(0, getAllEventsInPrimaryStorage(userId).size());

        int storageCount = swrveSpy.multiLayerLocalStorage.getSecondaryStorage().getFirstNEvents(150, userId).size();
        assertEquals(50 + eventsAddedOnInit, storageCount);
        for (int i = 0; i < 50; i++) {
            assertNotNull(swrveSpy.multiLayerLocalStorage.getSecondaryStorage().getCacheItem(userId, "category" + i));
        }
    }

    private LinkedHashMap<Long, String> getAllEventsInPrimaryStorage(String userId) {
        return swrveSpy.multiLayerLocalStorage.getPrimaryStorage().getFirstNEvents(Integer.MAX_VALUE, userId);
    }

    @Test
    public void testSessionStartTrigger() throws Exception {
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_session_start.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        swrveSpy.setMessageListener(new SwrveMessageListener() {
            @Override
            public void onMessage(SwrveMessage message) {
                onMessage(message, null);
            }

            @Override
            public void onMessage(SwrveMessage message, Map<String, String> properties) {
                assertNotNull(message);
                swrveSpy.messageWasShownToUser(message.getFormats().get(0));
                messageShownId = message.getId();
            }
        });

        swrveSpy.sessionStart();
        assertEquals(165, messageShownId);
    }

    @Test
    public void testButtonDismissWasPressed() throws Exception {
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");
        SwrveButton buttonDismiss = createButton(SwrveActionType.Dismiss, "campaign.json", null, null);
        int originalEvents = getAllEvents().size();
        swrveSpy.buttonWasPressedByUser(buttonDismiss);
        int lastEvents = getAllEvents().size();

        // One dismissal less
        assertEquals(originalEvents, lastEvents);
    }

    @Test
    public void testButtonInstallWasPressed() throws Exception {
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");
        SwrveButton buttonInstall = createButton(SwrveActionType.Install, "campaign.json", null, null);
        swrveSpy.buttonWasPressedByUser(buttonInstall);

        boolean clickFound = false;
        Object[] events = getAllEvents().values().toArray();
        for (int i = 0, j = events.length; i < j && !clickFound; i++) {
            String eventData = (String) events[i];
            clickFound = eventData.contains("Swrve.Messages.Message-" + buttonInstall.getMessage().getId());
        }
        assertTrue(clickFound);
    }

    @Test
    public void testButtonCustomWasPressed() throws Exception {
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");
        SwrveButton buttonCustom = createButton(SwrveActionType.Custom, "campaign.json", null, null);
        int originalEvents = getAllEvents().size();
        swrveSpy.buttonWasPressedByUser(buttonCustom);
        int lastEvents = getAllEvents().size();

        // One click event
        assertEquals(1, lastEvents - originalEvents);

        boolean clickFound = false;
        Object[] events = getAllEvents().values().toArray();
        for (int i = 0, j = events.length; i < j && !clickFound; i++) {
            String eventData = (String) events[i];
            clickFound = eventData.contains("Swrve.Messages.Message-" + buttonCustom.getMessage().getId());
        }
        assertTrue(clickFound);
    }

    private SwrveButton createButton(SwrveActionType type, String dummyJson, String action, Integer appId) {
        CustomSwrveCampaign campaign = null;
        try {
            String json = SwrveTestUtils.getAssetAsText(mActivity, dummyJson);
            JSONObject jsonObj = new JSONObject(json);
            JSONObject campaigns = jsonObj.getJSONArray("campaigns").getJSONObject(0);
            campaign = new CustomSwrveCampaign(swrveSpy, new SwrveCampaignDisplayer(), campaigns, new HashSet<SwrveAssetsQueueItem>());
        } catch (Exception exp) {
            SwrveLogger.e("Error createButton.", exp);
        }

        CustomSwrveMessage message = new CustomSwrveMessage(campaign, swrveSpy.getCacheDir());
        message.setId(303);
        message.setName("myMessage");
        campaign.setMessage(message);

        CustomSwrveButton btn = new CustomSwrveButton();
        btn.setAction(action);
        btn.setActionType(type);
        btn.setMessage(message);
        if (appId != null) {
            btn.setAppId(appId);
        }
        return btn;
    }

    class CustomSwrveCampaign extends SwrveInAppCampaign {

        public CustomSwrveCampaign(SwrveBase<?, ?> controller, SwrveCampaignDisplayer campaignManager, JSONObject campaignData, Set<SwrveAssetsQueueItem> assetsQueue) throws JSONException {
            super(controller, campaignManager, campaignData, assetsQueue, null);
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setMessage(SwrveMessage message) {
            this.message = message;
        }
    }

    class CustomSwrveMessage extends SwrveMessage {

        public CustomSwrveMessage(SwrveInAppCampaign campaign, File cacheDir) {
            super(campaign, cacheDir);
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    class CustomSwrveButton extends SwrveButton {
        public void setAction(String action) {
            super.setAction(action);
        }

        public void setActionType(SwrveActionType type) {
            super.setActionType(type);
        }

        public void setMessage(SwrveMessage message) {
            super.setMessage(message);
        }

        public void setAppId(int appId) {
            super.setAppId(appId);
        }
    }

    private LinkedHashMap<Long, String> getAllEvents() {
        return swrveSpy.multiLayerLocalStorage.getPrimaryStorage().getFirstNEvents(Integer.MAX_VALUE, swrveSpy.getUserId());
    }
}
