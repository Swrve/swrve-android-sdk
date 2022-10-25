package com.swrve.sdk;

import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.SWRVE_PAYLOAD_KEY;
import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.SWRVE_TRACKING_KEY;
import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY;
import static com.swrve.sdk.SwrveNotificationInternalPayloadConstants.SWRVE_UNIQUE_MESSAGE_ID_MAX_CACHE_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SwrvePushSidDeDuperTest extends SwrveBaseTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.context = ApplicationProvider.getApplicationContext().getApplicationContext();
    }

    @Test
    public void testDeduper() {

        LinkedList<String> idCache = getSidsFromCache();
        assertEquals(0, idCache.size());

        boolean isDupe = SwrvePushSidDeDuper.isDupe(context, getDummyMapData("1", "0"));
        assertFalse(isDupe);
        idCache = getSidsFromCache();
        assertEquals(0, idCache.size()); // Should still be size 0

        isDupe = SwrvePushSidDeDuper.isDupe(context, getDummyMapData("1", "1"));
        assertFalse(isDupe);
        idCache = getSidsFromCache();
        assertEquals(1, idCache.size());
        assertTrue(idCache.contains("1"));

        isDupe = SwrvePushSidDeDuper.isDupe(context, getDummyMapData("1", "1")); // this is the first dupe
        assertTrue(isDupe);
        idCache = getSidsFromCache();
        assertEquals(1, idCache.size());
        assertTrue(idCache.contains("1"));

        isDupe = SwrvePushSidDeDuper.isDupe(context, getDummyMapData("2", "1"));
        assertFalse(isDupe);
        idCache = getSidsFromCache();
        assertEquals(1, idCache.size());
        assertTrue(idCache.contains("2")); // the only side in the cache will be "2"

        isDupe = SwrvePushSidDeDuper.isDupe(context, getDummyMapData("2", "1")); // another dupe
        assertTrue(isDupe);
        idCache = getSidsFromCache();
        assertEquals(1, idCache.size());
        assertTrue(idCache.contains("2"));

        isDupe = SwrvePushSidDeDuper.isDupe(context, getDummyMapData("3", "2")); // increase cache size to 2
        assertFalse(isDupe);
        idCache = getSidsFromCache();
        assertEquals(2, idCache.size());
        assertTrue(idCache.contains("2"));
        assertTrue(idCache.contains("3"));

        isDupe = SwrvePushSidDeDuper.isDupe(context, getDummyMapData("4", "2"));
        assertFalse(isDupe);
        idCache = getSidsFromCache();
        assertEquals(2, idCache.size());
        assertTrue(idCache.contains("3"));
        assertTrue(idCache.contains("4"));
    }

    private Map<String, String> getDummyMapData(String sid, String maxCacheSize) {
        Map<String, String> map = new HashMap<>();
        map.put(SWRVE_TRACKING_KEY, "1");
        map.put(SWRVE_PAYLOAD_KEY, "some json");
        map.put(SWRVE_UNIQUE_MESSAGE_ID_KEY, sid);
        map.put(SWRVE_UNIQUE_MESSAGE_ID_MAX_CACHE_KEY, maxCacheSize);
        return map;
    }

    // This is a copy of SwrvePushSidDeDuper method - its trivial.
    private LinkedList<String> getSidsFromCache() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SwrvePushSidDeDuper.PREFERENCES, Context.MODE_PRIVATE);
        String jsonString = sharedPreferences.getString(SwrvePushSidDeDuper.SIDS, "");
        Gson gson = new Gson();
        LinkedList<String> recentIds = gson.fromJson(jsonString, new TypeToken<LinkedList<String>>() {
        }.getType());
        recentIds = recentIds == null ? new LinkedList<>() : recentIds;
        return recentIds;
    }
}
