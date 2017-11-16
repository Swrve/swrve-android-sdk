package com.swrve.sdk.localstorage;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SwrveMultiLayerLocalStorageTest extends BaseLocalStorage {
    private LocalStorage primaryLocalStorage;
    private LocalStorage secondaryLocalStorage;
    private SwrveMultiLayerLocalStorage multiLayerLocalStorage;

    @Before
    public void setUp() throws Exception {
        primaryLocalStorage = new InMemoryLocalStorage();
        secondaryLocalStorage = new SQLiteLocalStorage(RuntimeEnvironment.application, "flushToDiskTest", 2024 * 2024 * 2024);
        multiLayerLocalStorage = new SwrveMultiLayerLocalStorage(primaryLocalStorage);
        multiLayerLocalStorage.setSecondaryStorage(secondaryLocalStorage);
        localStorage = secondaryLocalStorage;
    }

    @Test
    public void testGetCombinedFirstNEvents() throws Exception {
        String userId = "userId";
        for (int i = 0; i < 20; i++) {
            primaryLocalStorage.addEvent(userId, "primary event" + i);
        }
        for (int i = 0; i < 20; i++) {
            secondaryLocalStorage.addEvent(userId, "secondary event" + i);
        }

        LinkedHashMap<LocalStorage, LinkedHashMap<Long, String>> events = multiLayerLocalStorage.getCombinedFirstNEvents(30, userId);
        int primaryCount = events.get(primaryLocalStorage).size();
        int secondaryCount = events.get(secondaryLocalStorage).size();

        assertEquals(10, primaryCount);
        assertEquals(20, secondaryCount);
    }

    @Test
    public void testHitFirstStorage() {
        // Both have same content
        primaryLocalStorage.setCacheEntry("a", "a", "data");
        secondaryLocalStorage.setCacheEntry("a", "a", "data_2");
        String cacheEntry = multiLayerLocalStorage.getCacheEntry("a", "a");
        assertEquals("data", cacheEntry);

        // Content only on secondary
        secondaryLocalStorage.setCacheEntry("b", "b", "data_2");
        cacheEntry = multiLayerLocalStorage.getCacheEntry("b", "b");
        assertEquals("data_2", cacheEntry);
    }

    @Test
    public void testSignatureValid() {
        String uniqueKey = "my_unique_key";
        String content = "myData@@$^SDSAD";
        multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser("userId", "categoryA", content, uniqueKey);
        // Should not raise an exception
        String contentFromCache = multiLayerLocalStorage.getSecureCacheEntryForUser("userId", "categoryA", uniqueKey);
        assertEquals(content, contentFromCache);

        // Should not raise an exception
        String contentFromSQLite = multiLayerLocalStorage.getSecureCacheEntryForUser("userId", "categoryA", uniqueKey);
        assertEquals(content, contentFromSQLite);
    }

    @Test
    public void testSignatureInvalidAfterModification() {
        String uniqueKey = "my_unique_key";
        String content = "myData@@$^SDSAD";
        multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser("userId", "categoryB", content, uniqueKey);

        // Modify memory cache content
        primaryLocalStorage.setCacheEntry("userId", "categoryB", "new_content");
        // Should raise an exception
        try {
            multiLayerLocalStorage.getSecureCacheEntryForUser("userId", "categoryB", uniqueKey);
            fail();
        } catch (SecurityException ex) {
            // Correctly detected bad content
        }

        // Modify database content
        secondaryLocalStorage.setCacheEntry("userId", "categoryB", "new_content");
        // Should raise an exception
        try {
            multiLayerLocalStorage.getSecureCacheEntryForUser("userId", "categoryB", uniqueKey);
            fail();
        } catch (SecurityException ex) {
            // Correctly detected bad content
        }
    }
}
