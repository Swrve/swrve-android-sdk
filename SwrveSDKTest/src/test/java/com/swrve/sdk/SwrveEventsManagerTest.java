package com.swrve.sdk;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.localstorage.InMemoryLocalStorage;
import com.swrve.sdk.localstorage.LocalStorage;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.RESTResponse;
import com.swrve.sdk.rest.RESTResponseLog;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;
import static com.swrve.sdk.SwrveEventsManagerImp.PREF_EVENT_SEND_RESPONSE_LOG;
import static com.swrve.sdk.SwrveTestUtils.createFakeRestClient;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

public class SwrveEventsManagerTest extends SwrveBaseTest {

    private SwrveMultiLayerLocalStorage multiLayerLocalStorage = null;
    private InMemoryLocalStorage secondaryStorage = null;
    private String userId = "userId";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        secondaryStorage = new InMemoryLocalStorage();
        multiLayerLocalStorage = new SwrveMultiLayerLocalStorage(new InMemoryLocalStorage());
        // using an InMemoryLocalStorage as secondary storage to avoid sqlite thread issues.
        multiLayerLocalStorage.setSecondaryStorage(secondaryStorage);
        SwrveEventsManagerImp.shouldSendResponseLogs = true; // reset this to true at the beginning of each test.
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

    @Test
    public void testResponseLogs() throws Exception {

        assertTrue(SwrveEventsManagerImp.shouldSendResponseLogs);
        ArrayList<String> events = new ArrayList<>();
        assertEquals(0, secondaryStorage.getFirstNEvents(10, userId).size());

        // store event and try sending it with responsecode 500. It will bw saved to storage.
        events.clear();
        events.add("{\"validevent\": \"500\"}");
        storeAndSendEvents(500, events);
        assertEquals(1, secondaryStorage.getFirstNEvents(10, userId).size());
        assertTrue(SwrveEventsManagerImp.shouldSendResponseLogs);

        // store event and try sending it with responsecode 429
        events.clear();
        events.add("{\"event1\": \"429\"}");
        events.add("{\"event2\": \"429\"}");
        events.add("{\"event3\": \"429\"}");
        storeAndSendEvents(429, events);
        assertEquals(4, secondaryStorage.getFirstNEvents(10, userId).size()); // four existing events in the queue, including 3 retained for retry due to 429 response code

        String headers = "{null=[HTTP/1.1 429], X-Android-Selected-Protocol=[http/1.1], Connection=[Close], X-Android-Response-Source=[NETWORK], X-Android-Sent-Millis=[1565687743648], X-Android-Received-Millis=[1565687743788], Content-Type=[text/plain]}";
        assertResponseLog(429, 3, 1, "429", headers);
        assertTrue(SwrveEventsManagerImp.shouldSendResponseLogs);

        // store more event and try sending it with responsecode 413
        events.clear();
        events.add("{\"event4\": \"413\"}");
        events.add("{\"event5\": \"413\"}");
        storeAndSendEvents(413, events);
        assertEquals(4, secondaryStorage.getFirstNEvents(10, userId).size()); // four existing events in the queue, 2 dropped due to 413 response code

        headers = "{null=[HTTP/1.1 413], X-Android-Selected-Protocol=[http/1.1], Connection=[Close], X-Android-Response-Source=[NETWORK], X-Android-Sent-Millis=[1565687743648], X-Android-Received-Millis=[1565687743788], Content-Type=[text/plain]}";
        assertResponseLog(413, 2, 1, "413", headers);
        assertTrue(SwrveEventsManagerImp.shouldSendResponseLogs);

        // store event and try sending it with responsecode 429 (which there should already be a log for)
        events.clear();
        events.add("{\"event6\": \"429\"}");
        events.add("{\"event7\": \"429\"}");
        storeAndSendEvents(429, events);
        assertEquals(6, secondaryStorage.getFirstNEvents(10, userId).size()); // four existing events in the queue, 2 additional due to 429 response code

        headers = "{null=[HTTP/1.1 429], X-Android-Selected-Protocol=[http/1.1], Connection=[Close], X-Android-Response-Source=[NETWORK], X-Android-Sent-Millis=[1565687743648], X-Android-Received-Millis=[1565687743788], Content-Type=[text/plain]}";
        assertResponseLog(429, 5, 2, "429", headers);
        assertTrue(SwrveEventsManagerImp.shouldSendResponseLogs);

        // store event and try sending it with responsecode 301
        events.clear();
        events.add("{\"event8\": \"301\"}");
        storeAndSendEvents(301, events);
        assertEquals(6, secondaryStorage.getFirstNEvents(10, userId).size()); // six existing events in the queue, 1 dropped due to 301 response code

        headers = "{null=[HTTP/1.1 301], X-Android-Selected-Protocol=[http/1.1], Connection=[Close], X-Android-Response-Source=[NETWORK], X-Android-Sent-Millis=[1565687743648], X-Android-Received-Millis=[1565687743788], Content-Type=[text/plain]}";
        assertResponseLog(301, 1, 1, "301", headers);
        assertTrue(SwrveEventsManagerImp.shouldSendResponseLogs);

        // store event and try sending it with responsecode 301
        events.clear();
        events.add("{\"event9\": \"100\"}");
        storeAndSendEvents(100, events);
        assertEquals(6, secondaryStorage.getFirstNEvents(10, userId).size()); // six existing events in the queue, 1 dropped due to 100 response code

        headers = "{null=[HTTP/1.1 100], X-Android-Selected-Protocol=[http/1.1], Connection=[Close], X-Android-Response-Source=[NETWORK], X-Android-Sent-Millis=[1565687743648], X-Android-Received-Millis=[1565687743788], Content-Type=[text/plain]}";
        assertResponseLog(100, 1, 1, "100", headers);
        assertTrue(SwrveEventsManagerImp.shouldSendResponseLogs);

        // send everything with responsecode 200
        sendStoredEvents(200);
        assertEquals(0, secondaryStorage.getFirstNEvents(10, userId).size()); // all events in the queue sent
        assertFalse(SwrveEventsManagerImp.shouldSendResponseLogs);

        SharedPreferences sharedPreferences = mActivity.getSharedPreferences(PREF_EVENT_SEND_RESPONSE_LOG, MODE_PRIVATE);
        assertEquals(0, sharedPreferences.getAll().size()); // logs cleared down
    }

    private void assertResponseLog(int code, int eventsCount, int requestCount, String body, String headers) {
        Gson gson = new Gson();
        SharedPreferences sharedPreferences = mActivity.getSharedPreferences(PREF_EVENT_SEND_RESPONSE_LOG, MODE_PRIVATE);
        String responseLogJson = sharedPreferences.getString(String.valueOf(code), null);
        assertNotNull(responseLogJson);
        RESTResponseLog responseLog = gson.fromJson(responseLogJson, new TypeToken<RESTResponseLog>() {
        }.getType());
        assertEquals(code, responseLog.code);
        assertEquals(eventsCount, responseLog.eventsCount);
        assertEquals(requestCount, responseLog.requestCount);
        assertEquals(body, responseLog.body);
        assertEquals(headers, responseLog.headers);
        assertTrue(responseLog.time > 0);
    }

    @Test
    public void testSendResponseLogs() throws Exception {

        // create swrveEventsManager spy
        IRESTClient restClient = createFakeRestClient(200);
        SwrveEventsManagerImp swrveEventsManagerSpy = spy(new SwrveEventsManagerImp(mActivity, new SwrveConfig(), restClient, userId, "1", "sessionToken", UUID.randomUUID().toString()));
        Mockito.doReturn(9876).when(swrveEventsManagerSpy).getNextSequenceNumber();
        Mockito.doReturn(1565691809624l).when(swrveEventsManagerSpy).getTime();

        // log some responses
        Map<String, List<String>> headersMap = new HashMap<>();
        List<String> headersList = new ArrayList<>();
        headersList.add( "HTTP/1.1");
        headersMap.put(null, headersList);
        swrveEventsManagerSpy.logResponse(new RESTResponse(413, "413 body", headersMap), 4);
        swrveEventsManagerSpy.logResponse(new RESTResponse(429, "429 body", headersMap), 2);

        swrveEventsManagerSpy.sendResponseLogs(secondaryStorage);

        ArgumentCaptor<List> eventsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<LocalStorage> localStorageCaptor = ArgumentCaptor.forClass(LocalStorage.class);
        Mockito.verify(swrveEventsManagerSpy, Mockito.times(1)).storeAndSendEvents(eventsCaptor.capture(), localStorageCaptor.capture());

        String actualEvents = eventsCaptor.getValue().toString();
// @formatter:off
        String expectedEvents =
                "[{" +
                    "\"type\":\"event\"," +
                    "\"time\":1565691809624," +
                    "\"seqnum\":9876," +
                    "\"name\":\"Swrve.RestResponseLog\"," +
                    "\"payload\":{" +
                        "\"headers\":\"{null=[HTTP\\/1.1]}\"," +
                        "\"code\":\"413\"," +
                        "\"events_count\":\"4\"," +
                        "\"time\":\"1565691809624\"," +
                        "\"body\":\"413 body\"," +
                        "\"request_count\":\"1\"}}, " +
                "{" +
                    "\"type\":\"event\"," +
                    "\"time\":1565691809624," +
                    "\"seqnum\":9876," +
                    "\"name\":\"Swrve.RestResponseLog\"," +
                    "\"payload\":{" +
                        "\"headers\":\"{null=[HTTP\\/1.1]}\"," +
                        "\"code\":\"429\"," +
                        "\"events_count\":\"2\"," +
                        "\"time\":\"1565691809624\"," +
                        "\"body\":\"429 body\"," +
                        "\"request_count\":\"1\"}}" +
                 "]";
// @formatter:on
        assertEquals(expectedEvents, actualEvents);

        assertFalse(SwrveEventsManagerImp.shouldSendResponseLogs);
    }

    private void storeAndSendEvents(int responseCode, ArrayList<String> events) throws Exception {
        IRESTClient restClient = createFakeRestClient(responseCode);
        String deviceId = UUID.randomUUID().toString();
        SwrveEventsManager swrveEventsManager = new SwrveEventsManagerImp(mActivity, new SwrveConfig(), restClient, userId, "1", "sessionToken", deviceId);
        swrveEventsManager.storeAndSendEvents(events, secondaryStorage);
    }

    private void sendStoredEvents(int responseCode) {
        IRESTClient restClient = createFakeRestClient(responseCode);
        String deviceId = UUID.randomUUID().toString();
        SwrveEventsManager swrveEventsManager = new SwrveEventsManagerImp(mActivity, new SwrveConfig(), restClient, userId, "1", "sessionToken", deviceId);
        swrveEventsManager.sendStoredEvents(multiLayerLocalStorage);
    }
}
