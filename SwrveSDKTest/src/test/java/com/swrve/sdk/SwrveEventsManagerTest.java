package com.swrve.sdk;

import com.swrve.sdk.localstorage.MemoryCachedLocalStorage;
import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SwrveEventsManagerTest extends SwrveBaseTest {

    private Swrve swrve = null;
    private MemoryCachedLocalStorage memoryCachedLocalStorage = null;
    private SQLiteLocalStorage sqLiteLocalStorage = null;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        swrve = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey");
        swrve.cachedLocalStorage = swrve.createCachedLocalStorage();
        sqLiteLocalStorage = new SQLiteLocalStorage(RuntimeEnvironment.application, swrve.config.getDbName(), swrve.config.getMaxSqliteDbSize());
        memoryCachedLocalStorage = new MemoryCachedLocalStorage(sqLiteLocalStorage, null);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        if (sqLiteLocalStorage != null) sqLiteLocalStorage.close();
        if (memoryCachedLocalStorage != null) memoryCachedLocalStorage.close();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testRestResponseCode() throws Exception {
        // 200 OK - Event should be deleted from queue because it was processed ok
        // 300 - Event should be deleted from queue and should not be resent
        // 400 Bad request - Event should be deleted from queue because its a bad event and should not be resent
        // 500 Server - Event should remain in queue so it can be resent later

        ArrayList<String> events = new ArrayList<>();
        assertEquals(0, sqLiteLocalStorage.getFirstNEvents(10).size());

        // store 1 event and try sending it with responsecode 500
        events.clear();
        events.add("{\"someevent1\": \"500\"}");
        storeAndSendEvents(500, events);
        assertEquals(1, sqLiteLocalStorage.getFirstNEvents(10).size());

        // send everything with responsecode 200
        sendStoredEvents(200, events);
        assertEquals(0, sqLiteLocalStorage.getFirstNEvents(10).size());

        // store 2 events and try sending them with responsecode 500
        events.clear();
        events.add("{\"someevent2\": \"500\"}");
        events.add("{\"someevent3\": \"500\"}");
        storeAndSendEvents(500, events);
        assertEquals(2, sqLiteLocalStorage.getFirstNEvents(10).size());

        // store another event and try sending it with responsecode 300
        events.clear();
        events.add("{\"someevent4\": \"300\"}");
        storeAndSendEvents(300, events);
        assertEquals(2, sqLiteLocalStorage.getFirstNEvents(10).size()); // someevent4 is not in queue

        // store another event and try sending it with responsecode 400
        events.clear();
        events.add("{\"someevent5\": \"400\"}");
        storeAndSendEvents(400, events);
        assertEquals(2, sqLiteLocalStorage.getFirstNEvents(10).size()); // someevent5 is not in queue

        // store another event and try sending it with responsecode 500
        events.clear();
        events.add("{\"someevent6\": \"500\"}");
        storeAndSendEvents(500, events);
        assertEquals(3, sqLiteLocalStorage.getFirstNEvents(10).size()); // someevent6 added to queue

        // store another event and try sending it with responsecode 501
        events.clear();
        events.add("{\"someevent7\": \"501\"}");
        storeAndSendEvents(501, events);
        assertEquals(4, sqLiteLocalStorage.getFirstNEvents(10).size()); // someevent7 added to queue

        // send everything with responsecode 200
        sendStoredEvents(200, events);
        assertEquals(0, sqLiteLocalStorage.getFirstNEvents(10).size());
    }

    private void storeAndSendEvents(int responseCode, ArrayList<String> events) throws Exception {
        IRESTClient restClient = createFakeRestClient(responseCode);
        short deviceId = 1;
        SwrveEventsManager swrveEventsManager = new SwrveEventsManagerImp(swrve.config, restClient, swrve.userId, swrve.appVersion, "sessionToken", deviceId);
        swrveEventsManager.storeAndSendEvents(events, memoryCachedLocalStorage, sqLiteLocalStorage);
    }

    private void sendStoredEvents(int responseCode, ArrayList<String> events) throws Exception {
        IRESTClient restClient = createFakeRestClient(responseCode);
        short deviceId = 1;
        SwrveEventsManager swrveEventsManager = new SwrveEventsManagerImp(swrve.config, restClient, swrve.userId, swrve.appVersion, "sessionToken", deviceId);
        swrveEventsManager.sendStoredEvents(memoryCachedLocalStorage);
    }

    private IRESTClient createFakeRestClient(final int responseCode) {
        return new IRESTClient() {
            @Override
            public void get(String endpoint, IRESTResponseListener callback) {
                // unused
            }
            @Override
            public void get(String endpoint, Map<String, String> params, IRESTResponseListener callback) throws UnsupportedEncodingException {
                // unused
            }
            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback) {
                RESTResponse response = new RESTResponse(responseCode, String.valueOf(responseCode), new HashMap<String, List<String>>());
                callback.onResponse(response);
            }
            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback, String contentType) {

                RESTResponse response = new RESTResponse(responseCode, String.valueOf(responseCode), new HashMap<String, List<String>>());
                callback.onResponse(response);
            }
        };
    }

}
