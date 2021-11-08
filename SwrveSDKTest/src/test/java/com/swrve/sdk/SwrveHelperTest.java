package com.swrve.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SwrveHelperTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testCombineTwoStringMaps() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("key1", "replace_me");
        map1.put("key2", "value2");

        Map<String, String> map2 = new HashMap<>();
        map2.put("key1", "value1");
        map2.put("key3", "value3");

        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("key1", "value1");
        expectedMap.put("key2", "value2");
        expectedMap.put("key3", "value3");

        Map<String, String> combinedMap = SwrveHelper.combineTwoStringMaps(map1, map2);
        assertEquals(expectedMap, combinedMap);

        combinedMap = SwrveHelper.combineTwoStringMaps(map1, null);
        assertEquals(map1, combinedMap);

        combinedMap = SwrveHelper.combineTwoStringMaps(null, map2);
        assertEquals(map2, combinedMap);

        // make sure it won't crash
        combinedMap = SwrveHelper.combineTwoStringMaps(null, null);
        assertEquals(null, combinedMap);
    }

    @Test
    public void testConvertPushPayloadToJSONObject_OneLevelDeep() throws Exception {
        // Unfortunately the "_s.JsonPayload" is only included when payload is more than one level deep.
        // This test is for when its one level deep
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, "somefakevalue");
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "456");
        bundle.putString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY, "789");
        bundle.putString("customkey1", "customvalue1");
        bundle.putString("customkey2", "customvalue2");
        bundle.putString("customkey3", "customvalue3");

        JSONObject payload = SwrveHelper.convertPayloadToJSONObject(bundle);

        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertEquals("123", payload.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY));
        assertEquals("somefakevalue", payload.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY));
        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY));
        assertEquals("456", payload.getString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY));
        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY));
        assertEquals("789", payload.getString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY));

        // payloads
        assertTrue(payload.has("customkey1"));
        assertEquals("customvalue1", payload.getString("customkey1"));
        assertTrue(payload.has("customkey2"));
        assertEquals("customvalue2", payload.getString("customkey2"));
        assertTrue(payload.has("customkey3"));
        assertEquals("customvalue3", payload.getString("customkey3"));
    }

    @Test
    public void testConvertPushPayloadToJSONObject_TwoLevelDeep() throws Exception {
        // Unfortunately the "_s.JsonPayload" is only included when payload is more than one level deep.
        // This test is for when its one level deep
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, "somefakevalue");
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "456");
        bundle.putString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY, "789");
        bundle.putString("root1", "value1");
        bundle.putString("root2", "value2");

        String twoDeepJson =
                "{" +
                        "\"g1\":{" +
                        "\"key1\":\"value1\"," +
                        "\"key2\":\"value2\"" +
                        "}," +
                        "\"g2\":{" +
                        "\"key3\":\"value3\"" +
                        "}," +
                        "\"root1\":\"value1\"," +
                        "\"root2\":\"value2\"" +
                        "}";
        bundle.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, twoDeepJson);

        JSONObject payload = SwrveHelper.convertPayloadToJSONObject(bundle);

        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertEquals("123", payload.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY));
        assertEquals("somefakevalue", payload.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY));
        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY));
        assertEquals("456", payload.getString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY));
        assertTrue(payload.has(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY));
        assertEquals("789", payload.getString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY));

        // payloads
        assertTrue(payload.has("root1"));
        assertEquals("value1", payload.getString("root1"));
        assertTrue(payload.has("g1"));
        assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}", payload.getString("g1"));
        assertTrue(payload.has("g2"));
        assertEquals("{\"key3\":\"value3\"}", payload.getString("g2"));
    }
}
