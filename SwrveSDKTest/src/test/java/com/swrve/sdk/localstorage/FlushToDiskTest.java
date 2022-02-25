package com.swrve.sdk.localstorage;

import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveLogger;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowLog;

import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;

public class FlushToDiskTest  extends SwrveBaseTest {
    private LocalStorage primaryLocalStorage;
    private LocalStorage secondaryLocalStorage;
    private SwrveMultiLayerLocalStorage multiLayerLocalStorage;

    @Before
    public void setUp() throws Exception {
        SwrveLogger.setLogLevel(Log.VERBOSE);
        ShadowLog.stream = System.out;
        primaryLocalStorage = new InMemoryLocalStorage();
        secondaryLocalStorage = new SQLiteLocalStorage(ApplicationProvider.getApplicationContext(), "flushToDiskTest", 2024 * 2024 * 2024);
        multiLayerLocalStorage = new SwrveMultiLayerLocalStorage(primaryLocalStorage);
        multiLayerLocalStorage.setSecondaryStorage(secondaryLocalStorage);
    }

    @Test
    public void testFlushToDisk() throws Exception {
        String userId = "userId";
        int amount = 2000;
        for (int i = 0; i < amount; i++) {
            multiLayerLocalStorage.addEvent(userId, "event" + i);
        }
        LinkedHashMap<LocalStorage, LinkedHashMap<Long, String>> events = multiLayerLocalStorage.getCombinedFirstNEvents(amount, userId);
        int eventCount = events.get(primaryLocalStorage).size();
        int primaryEventCount = primaryLocalStorage.getFirstNEvents(amount, userId).size();
        int secondaryEventCount = secondaryLocalStorage.getFirstNEvents(amount, userId).size();
        assertEquals(1, events.size());
        assertEquals(amount, eventCount);
        assertEquals(amount, primaryEventCount);
        assertEquals(0, secondaryEventCount);

        multiLayerLocalStorage.flush();

        events = multiLayerLocalStorage.getCombinedFirstNEvents(amount, userId);
        eventCount = events.get(secondaryLocalStorage).size();
        primaryEventCount = primaryLocalStorage.getFirstNEvents(amount, userId).size();
        secondaryEventCount = secondaryLocalStorage.getFirstNEvents(amount, userId).size();
        assertEquals(1, events.size());
        assertEquals(amount, eventCount);
        assertEquals(0, primaryEventCount);
        assertEquals(amount, secondaryEventCount);
    }
}
