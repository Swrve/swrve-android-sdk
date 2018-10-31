package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.localstorage.InMemoryLocalStorage;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;
import com.swrve.sdk.rest.IRESTClient;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.UUID;

import static com.swrve.sdk.SwrveTestUtils.createFakeRestClient;
import static org.junit.Assert.assertEquals;

public class SwrveEventsManagerTest extends SwrveBaseTest {

    private SwrveMultiLayerLocalStorage multiLayerLocalStorage = null;
    private InMemoryLocalStorage secondaryStorage = null;
    private String userId = "userId";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        String dbName = "SwrveEventsManagerTest_" + System.currentTimeMillis();
        secondaryStorage = new InMemoryLocalStorage();
        multiLayerLocalStorage = new SwrveMultiLayerLocalStorage(new InMemoryLocalStorage());
        // using an InMemoryLocalStorage as secondary storage to avoid sqlite thread issues.
        multiLayerLocalStorage.setSecondaryStorage(secondaryStorage);
    }

    @Test
    public void testRestResponseCode() throws Exception {
        // 200 OK - Event should be deleted from queue because it was processed ok
        // 300 - Event should be deleted from queue and should not be resent
        // 400 Bad request - Event should be deleted from queue because its a bad event and should not be resent
        // 500 Server - Event should remain in queue so it can be resent later

        ArrayList<String> events = new ArrayList<>();
        assertEquals(0, secondaryStorage.getFirstNEvents(10, userId).size());

        // store 1 event and try sending it with responsecode 500
        events.clear();
        events.add("{\"someevent1\": \"500\"}");
        storeAndSendEvents(500, events);
        assertEquals(1, secondaryStorage.getFirstNEvents(10, userId).size());

        // send everything with responsecode 200
        sendStoredEvents(200);
        assertEquals(0, secondaryStorage.getFirstNEvents(10, userId).size());

        // store 2 events and try sending them with responsecode 500
        events.clear();
        events.add("{\"someevent2\": \"500\"}");
        events.add("{\"someevent3\": \"500\"}");
        storeAndSendEvents(500, events);
        assertEquals(2, secondaryStorage.getFirstNEvents(10, userId).size());

        // store another event and try sending it with responsecode 300
        events.clear();
        events.add("{\"someevent4\": \"300\"}");
        storeAndSendEvents(300, events);
        assertEquals(2, secondaryStorage.getFirstNEvents(10, userId).size()); // someevent4 is not in queue

        // store another event and try sending it with responsecode 400
        events.clear();
        events.add("{\"someevent5\": \"400\"}");
        storeAndSendEvents(400, events);
        assertEquals(2, secondaryStorage.getFirstNEvents(10, userId).size()); // someevent5 is not in queue

        // store another event and try sending it with responsecode 500
        events.clear();
        events.add("{\"someevent6\": \"500\"}");
        storeAndSendEvents(500, events);
        assertEquals(3, secondaryStorage.getFirstNEvents(10, userId).size()); // someevent6 added to queue

        // store another event and try sending it with responsecode 501
        events.clear();
        events.add("{\"someevent7\": \"501\"}");
        storeAndSendEvents(501, events);
        assertEquals(4, secondaryStorage.getFirstNEvents(10, userId).size()); // someevent7 added to queue

        // send everything with responsecode 200
        sendStoredEvents(200);
        assertEquals(0, secondaryStorage.getFirstNEvents(10, userId).size());
    }

    private void storeAndSendEvents(int responseCode, ArrayList<String> events) throws Exception {
        IRESTClient restClient = createFakeRestClient(responseCode);
        String deviceId = UUID.randomUUID().toString();
        SwrveEventsManager swrveEventsManager = new SwrveEventsManagerImp(new SwrveConfig(), restClient, userId, "1", "sessionToken", deviceId);
        swrveEventsManager.storeAndSendEvents(events, secondaryStorage);
    }

    private void sendStoredEvents(int responseCode) throws Exception {
        IRESTClient restClient = createFakeRestClient(responseCode);
        String deviceId = UUID.randomUUID().toString();
        SwrveEventsManager swrveEventsManager = new SwrveEventsManagerImp(new SwrveConfig(), restClient, userId, "1", "sessionToken", deviceId);
        swrveEventsManager.sendStoredEvents(multiLayerLocalStorage);
    }
}
