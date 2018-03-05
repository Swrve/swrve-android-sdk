package com.swrve.sdk.localstorage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SQLiteLocalStorageTest extends BaseLocalStorage {

    int maxSize = 2024 * 1024 * 1024;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        localStorage = new SQLiteLocalStorage(RuntimeEnvironment.application, "test", maxSize);
    }

    @Test
    public void testOverflowSpace() {
        SQLiteLocalStorage sqLiteLocalStorage = (SQLiteLocalStorage)localStorage;
        assertEquals("Maximum size not being set correctly.", maxSize, sqLiteLocalStorage.database.getMaximumSize());
    }

    @Test
    public void testSaveMultipleEventItems() {

        String userId = "userId_AddMultipleEvent";
        SQLiteLocalStorage sqLiteLocalStorage = (SQLiteLocalStorage) localStorage;
        assertEquals(0, sqLiteLocalStorage.getFirstNEvents(100, userId).size());

        List<SwrveEventItem> events = new ArrayList<>();

        SwrveEventItem item1 = createEventItem(userId, "event1");
        SwrveEventItem item2 = createEventItem(userId, "event2");
        events.add(item1);
        events.add(item2);
        sqLiteLocalStorage.saveMultipleEventItems(events);

        LinkedHashMap<Long, String> firstNEvents = sqLiteLocalStorage.getFirstNEvents(100, userId);
        assertEquals(2, firstNEvents.size());
        assertTrue(firstNEvents.containsKey(1L));
        assertEquals(item1.event, firstNEvents.get(1L));
        assertTrue(firstNEvents.containsKey(2L));
        assertEquals(item2.event, firstNEvents.get(2L));
    }

    private SwrveEventItem createEventItem(String userId, String event) {
        SwrveEventItem item = new SwrveEventItem();
        item.userId = userId;
        item.event = event;
        return item;
    }
}
