package com.swrve.sdk;

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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.localstorage.LocalStorage;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @Test
    public void testFullInitialization() throws Exception {

        SwrveEventsManager eventsManagerMock = mock(SwrveEventsManager.class);
        doReturn(eventsManagerMock).when(swrveSpy).getSwrveEventsManager(anyString(), anyString(), anyString());
        doReturn(123l).when(swrveSpy).getSessionTime();

        swrveSpy.init(mActivity);

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
    public void testAllEventMethods() {

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

//        String seqnum = swrveSpy.multiLayerLocalStorage.getCacheEntry(swrveSpy.getUserId(), "seqnum");
//        assertEquals(10, Integer.parseInt(seqnum));

        // 9 events queued via queueEvent method
        verify(swrveSpy, times(8)).queueEvent(anyString(), anyString(), nullable(Map.class), nullable(Map.class), anyBoolean());

        // 2 events queued via sendSessionStart (via init and SwrveSDK.sessionStart())
        verify(swrveSpy, times(2)).sendSessionStart(123);
    }

    @Test
    public void testBatchSendTest() throws Exception {

        swrveSpy.onCreate(mActivity);

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
        assertEquals("generic_event_0", data2.getJSONObject(3).getString("name"));
        assertEquals("generic_event_1", data2.getJSONObject(4).getString("name"));
        assertEquals("generic_event_2", data2.getJSONObject(5).getString("name"));
    }

    @Test
    public void testDeviceIDUniqueBetweenInstances() throws Exception {

        String deviceID = swrveSpy.getDeviceId();
        assertNotNull(deviceID);

        // shutdown current instance and call setup again to recreate it
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        String swrve1Instance = ((Object) swrveSpy).toString();
        setUp();
        String swrve2Instance = ((Object) swrveSpy).toString();
        assertNotEquals(swrve1Instance, swrve2Instance); // verify its a new instance

        assertEquals(deviceID, swrveSpy.getDeviceId());
    }

    @Test
    public void testFlush() {
        int eventsAddedOnInit = 3;
        String userId = swrveSpy.getUserId();
        swrveSpy.onCreate(mActivity);
        for (int i = 0; i < 50; i++) {
            swrveSpy.multiLayerLocalStorage.getPrimaryStorage().setCacheEntry(userId, "category" + i, "rawData" + i);
        }

        // 50 events plus firstsession event, and user device prop event
        assertEquals(eventsAddedOnInit, getAllEventsInPrimaryStorage(userId).size());
        swrveSpy.flushToDisk();
        // Data has been moved to the other storage
        assertEquals(0, getAllEventsInPrimaryStorage(userId).size());

        int storageCount = swrveSpy.multiLayerLocalStorage.getSecondaryStorage().getFirstNEvents(150, userId).size();
        assertEquals(eventsAddedOnInit, storageCount);
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

        swrveSpy.sessionStart();

        ArgumentCaptor<SwrveMessage> swrveMessageCaptor = ArgumentCaptor.forClass(SwrveMessage.class);
        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(swrveSpy, Mockito.atLeast(1)).displaySwrveMessage(swrveMessageCaptor.capture(), mapCaptor.capture());
        messageShownId = swrveMessageCaptor.getAllValues().get(0).getId();

        assertEquals(165, messageShownId);
    }

    @Test
    public void testButtonDismissWasPressed() throws Exception {
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");

        assertEquals(3, getAllEvents().size());

        SwrveButton buttonDismiss = createButton("DISMISS", "campaign.json", null, 150);
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SwrveInAppMessageActivity.class);
        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, 165);

        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_1", "some personalized value1");
        personalization.put("test_2", "some personalized value2");
        intent.putExtra(SwrveInAppMessageActivity.SWRVE_PERSONALISATION_KEY, personalization);
        InAppMessageHandler inAppMessageHandler = new InAppMessageHandler(ApplicationProvider.getApplicationContext(), intent, null);
        inAppMessageHandler.customEventDelayQueueSeconds = 0;

        inAppMessageHandler.buttonClicked(buttonDismiss, "someAction", "", 0, "");

        Thread.sleep(100l); // Custom events are sent a short period of time later so sleep

        // 4 new buttons events should be queued, Test Json has Swrve. event which should not be sent
        assertEquals(7, getAllEvents().size());

        verifyDataCapturedFromButtonClick();
    }

    @Test
    public void testButtonInstallWasPressed() throws Exception {
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");

        SwrveButton buttonInstall = createButton("INSTALL", "campaign.json", null, 150);
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SwrveInAppMessageActivity.class);
        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, 165);
        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_1", "some personalized value1");
        personalization.put("test_2", "some personalized value2");
        intent.putExtra(SwrveInAppMessageActivity.SWRVE_PERSONALISATION_KEY, personalization);
        InAppMessageHandler inAppMessageHandler = new InAppMessageHandler(ApplicationProvider.getApplicationContext(), intent, null);
        inAppMessageHandler.customEventDelayQueueSeconds = 0;

        inAppMessageHandler.buttonClicked(buttonInstall, "someAction", "",0, "");

        boolean clickFound = false;
        Object[] events = getAllEvents().values().toArray();
        for (int i = 0, j = events.length; i < j && !clickFound; i++) {
            String eventData = (String) events[i];
            clickFound = eventData.contains("Swrve.Messages.Message-" + buttonInstall.getMessage().getId());
        }
        assertTrue(clickFound);

        Thread.sleep(100l); // Custom events are sent a short period of time later so sleep

        // 4 new buttons events should be queued, Test Json has Swrve. event which should not be sent
        assertEquals(8, getAllEvents().size());

        verifyDataCapturedFromButtonClick();
    }

    @Test
    public void testButtonCustomWasPressed() throws Exception {
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");

        assertEquals(3, getAllEvents().size());

        SwrveButton buttonCustom = createButton("CUSTOM", "campaign.json", null, 150);
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SwrveInAppMessageActivity.class);
        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, 165);
        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_1", "some personalized value1");
        personalization.put("test_2", "some personalized value2");
        intent.putExtra(SwrveInAppMessageActivity.SWRVE_PERSONALISATION_KEY, personalization);
        InAppMessageHandler inAppMessageHandler = new InAppMessageHandler(ApplicationProvider.getApplicationContext(), intent, null);
        inAppMessageHandler.customEventDelayQueueSeconds = 0;

        inAppMessageHandler.buttonClicked(buttonCustom, "someAction", "", 0, "");

        boolean clickFound = false;
        Object[] events = getAllEvents().values().toArray();
        for (int i = 0, j = events.length; i < j && !clickFound; i++) {
            String eventData = (String) events[i];
            clickFound = eventData.contains("Swrve.Messages.Message-" + buttonCustom.getMessage().getId());
        }
        assertTrue(clickFound);

        Thread.sleep(100l); // Custom events are sent a short period of time later so sleep

        // 4 new buttons events should be queued, Test Json has Swrve. event which should not be sent
        assertEquals(8, getAllEvents().size());

        verifyDataCapturedFromButtonClick();
    }

    @Test
    public void testButtonOpenAppSettings() throws Exception {
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");

        SwrveButton buttonCustom = createButton("OPEN_APP_SETTINGS", "campaign.json", "", 150);
        InAppMessageHandler inAppMessageHandler = new InAppMessageHandler(mActivity, null, null);
        inAppMessageHandler.buttonClicked(buttonCustom, "", "", 0, "");

        Shadows.shadowOf(mActivity.getMainLooper()).idle();

        ShadowActivity.IntentForResult nextIntent = Shadows.shadowOf(mActivity).getNextStartedActivityForResult();
        assertNotNull(nextIntent);
        assertEquals("android.settings.APPLICATION_DETAILS_SETTINGS", nextIntent.intent.getAction());
        assertEquals("package:com.swrve.sdk.test", nextIntent.intent.getDataString());
    }

    @Test
    public void testButtonOpenNotificationSettings() throws Exception {
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");

        SwrveButton buttonCustom = createButton("OPEN_NOTIFICATION_SETTINGS", "campaign.json", "", 150);
        InAppMessageHandler inAppMessageHandler = new InAppMessageHandler(mActivity, null, null);
        inAppMessageHandler.buttonClicked(buttonCustom, "", "", 0, "");

        Shadows.shadowOf(mActivity.getMainLooper()).idle();

        ShadowActivity.IntentForResult nextIntent = Shadows.shadowOf(mActivity).getNextStartedActivityForResult();
        assertNotNull(nextIntent);
        assertEquals("android.settings.APP_NOTIFICATION_SETTINGS", nextIntent.intent.getAction());
    }

    @Test
    public void testButtonStartGeo() throws Exception {
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");

        SwrveButton buttonCustom = createButton("START_GEO", "campaign.json", "", 150);
        InAppMessageHandler inAppMessageHandlerSpy = spy(new InAppMessageHandler(mActivity, null, null));
        inAppMessageHandlerSpy.buttonClicked(buttonCustom, "", "", 0, "");

        Shadows.shadowOf(mActivity.getMainLooper()).idle();

        ShadowActivity.IntentForResult nextIntent = Shadows.shadowOf(mActivity).getNextStartedActivityForResult();
        assertNull(nextIntent);
        verify(inAppMessageHandlerSpy, atLeastOnce()).startGeoButtonClicked();
    }

    @Test
    public void testButtonRequestCapabilityPermissions() throws Exception {
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");

        SwrveButton buttonCustom = createButton("REQUEST_CAPABILITY", "campaign.json", "android.permission.POST_NOTIFICATIONS", 150);
        InAppMessageHandler inAppMessageHandler = new InAppMessageHandler(ApplicationProvider.getApplicationContext(), null, null);
        inAppMessageHandler.buttonClicked(buttonCustom, "android.permission.POST_NOTIFICATIONS", "", 0, "");

        Shadows.shadowOf(mActivity.getMainLooper()).idle();

        ShadowActivity.IntentForResult nextIntent = Shadows.shadowOf(mActivity).getNextStartedActivityForResult();
        assertNotNull(nextIntent);
        assertEquals("com.swrve.sdk.SwrvePermissionRequesterActivity", nextIntent.intent.getComponent().getClassName());
    }

    private SwrveButton createButton(String type, String dummyJson, String action, Integer appId) throws Exception {
        SwrveInAppCampaign campaign = null;
        String json = SwrveTestUtils.getAssetAsText(mActivity, dummyJson);
        JSONObject jsonObj = new JSONObject(json);
        JSONObject campaignData = jsonObj.getJSONArray("campaigns").getJSONObject(0);
        campaign = new SwrveInAppCampaign(swrveSpy, new SwrveCampaignDisplayer(), campaignData, new HashSet<>(), null);
        SwrveMessage message = campaign.getMessage();

        String buttonJson = "                  {\n" +
                "                    \"name\": \"accept\",\n" +
                "                    \"x\": {\n" +
                "                      \"type\": \"number\",\n" +
                "                      \"value\": -200\n" +
                "                    },\n" +
                "                    \"y\": {\n" +
                "                      \"type\": \"number\",\n" +
                "                      \"value\": 80\n" +
                "                    },\n" +
                "                    \"w\": {\n" +
                "                      \"type\": \"number\",\n" +
                "                      \"value\": 229\n" +
                "                    },\n" +
                "                    \"h\": {\n" +
                "                      \"type\": \"number\",\n" +
                "                      \"value\": 114\n" +
                "                    },\n" +
                "                    \"image_up\": {\n" +
                "                      \"type\": \"asset\",\n" +
                "                      \"value\": \"8721fd4e657980a5e12d498e73aed6e6a565dfca\"\n" +
                "                    },\n" +
                "                    \"action\": {\n" +
                "                      \"type\": \"text\",\n" +
                "                      \"value\": \"" + action + "\"\n" +
                "                    },\n" +
                "                    \"game_id\": {\n" +
                "                      \"type\": \"number\",\n" +
                "                      \"value\": \"" + appId + "\"\n" +
                "                    },\n" +
                "                    \"type\": {\n" +
                "                      \"type\": \"text\",\n" +
                "                      \"value\": \"" + type + "\"\n" +
                "                    },\n" +
                "                    \"events\": [" +
                "                           {\n" +
                "                             \"name\": \"Swrve.Not Allowed\" \n" +
                "                           },\n" +
                "                           {\n" +
                "                             \"name\": \"Test Event No Payload\" \n" +
                "                           },\n" +
                "                           {\n" +
                "                           \"name\": \"Test Event 1\",\n" +
                "                           \"payload\": [{\n" +
                "                               \"key\": \"key1\",\n" +
                "                                \"value\":\"some value personalized:${test_1}\"\n" +
                "                                },\n" +
                "                               { \n" +
                "                               \"key\": \"key2\", \n" +
                "                                \"value\": \"some value personalized:${test_2}\"\n" +
                "                               }\n" +
                "                            ]\n" +
                "                          },\n" +
                "                          {\n" +
                "                          \"name\": \"Test Event 2\",\n" +
                "                          \"payload\": [{\n" +
                "                             \"key\": \"key1\",\n" +
                "                             \"value\": \"some value personalized:${test_1}\"\n" +
                "                           }]\n" +
                "                         }\n" +
                "                       ],\n" +
                "                       \"user_updates\": [{\n" +
                "                            \"key\": \"key1\",\n" +
                "                            \"value\": \"some value personalized:${test_1}\"\n" +
                "                         }]\n" +
                "                       }\n" +
                "                  }";

        SwrveButton btn = new SwrveButton(message, new JSONObject(buttonJson));
        return btn;
    }

    private LinkedHashMap<Long, String> getAllEvents() {
        return swrveSpy.multiLayerLocalStorage.getPrimaryStorage().getFirstNEvents(Integer.MAX_VALUE, swrveSpy.getUserId());
    }

    private void verifyDataCapturedFromButtonClick() {
        ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, String>> payload = ArgumentCaptor.forClass(Map.class);
        verify(swrveSpy, times(4)).event(name.capture(), payload.capture());
        assertEquals("Swrve.Not Allowed", name.getAllValues().get(0));
        assertEquals(0, payload.getAllValues().get(0).size());
        assertEquals("Test Event No Payload", name.getAllValues().get(1));
        assertEquals(0, payload.getAllValues().get(1).size());
        assertEquals("Test Event 1", name.getAllValues().get(2));
        assertEquals("some value personalized:some personalized value1", payload.getAllValues().get(2).get("key1"));
        assertEquals("some value personalized:some personalized value2", payload.getAllValues().get(2).get("key2"));
        assertEquals("Test Event 2", name.getAllValues().get(3));
        assertEquals("some value personalized:some personalized value1", payload.getAllValues().get(3).get("key1"));

        ArgumentCaptor<Map<String, String>> attributes = ArgumentCaptor.forClass(Map.class);
        verify(swrveSpy, times(1)).userUpdate(attributes.capture());
        assertEquals("some value personalized:some personalized value1", attributes.getValue().get("key1"));
    }
}

