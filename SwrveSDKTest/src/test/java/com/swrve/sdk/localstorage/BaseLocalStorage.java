package com.swrve.sdk.localstorage;

import com.swrve.sdk.SwrveBaseTest;

import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public abstract class BaseLocalStorage extends SwrveBaseTest {

    protected LocalStorage localStorage;

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        LocalStorageTestUtils.removeSQLiteOpenHelperSingletonInstance();
    }

    protected int insertEvents(int eventCount, String userId) throws Exception {
        int eventsSaved = 0;
        for (int i = 0; i < eventCount; i++) {
            eventsSaved++;
            localStorage.addEvent(userId, "myevent");
        }
        return eventsSaved;
    }

    @Test
    public void testInsertEvents() throws Exception {
        int events = insertEvents(500, "userId");
        assertEquals(500, events);
        assertEquals(500, localStorage.getFirstNEvents(1000, "userId").size());
    }

    @Test
    public void testAddAndRemoveEvents() throws Exception {
        insertEvents(10, "userId");
        Map<Long, String> events = localStorage.getFirstNEvents(20, "userId");
        assertEquals(10, events.keySet().size());

        localStorage.removeEvents("userId", events.keySet());

        Map<Long, String> notRemovedEvents = localStorage.getFirstNEvents(20, "userId");
        assertEquals(0, notRemovedEvents.keySet().size());
    }

    @Test
    public void testGetFirstNEvents() throws Exception {
        insertEvents(10, "userId");

        Map<Long, String> events = localStorage.getFirstNEvents(20, "userId");
        assertEquals(10, events.keySet().size());

        insertEvents(10, "userId");
        Map<Long, String> eventsStep2 = localStorage.getFirstNEvents(20, "userId");
        assertEquals(20, eventsStep2.keySet().size());

        Map<Long, String> eventsStep3 = localStorage.getFirstNEvents(30, "userId");
        assertEquals(20, eventsStep3.keySet().size());
    }

    @Test
    public void testNoEvents() {
        Map<Long, String> events = localStorage.getFirstNEvents(20, "userId");
        assertEquals(0, events.keySet().size());
    }

    @Test
    public void testNotSavedCacheEntry() {
        assertNull(localStorage.getCacheItem("userId", "not_defined_category"));
    }

    @Test
    public void testSetCacheEntry() {
        for (int i = 0; i < 50; i++) {
            localStorage.setCacheEntry("userId", "category" + i, "rawData" + i);
        }
        for (int i = 0; i < 50; i++) {
            assertEquals("rawData" + i, localStorage.getCacheItem("userId", "category" + i).rawData);
        }
    }
}
