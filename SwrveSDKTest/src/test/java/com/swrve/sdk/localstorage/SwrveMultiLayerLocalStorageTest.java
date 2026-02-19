package com.swrve.sdk.localstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.SwrveBaseTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;

public class SwrveMultiLayerLocalStorageTest extends SwrveBaseTest {
    private LocalStorage primaryLocalStorage;
    private LocalStorage secondaryLocalStorage;
    private SwrveMultiLayerLocalStorage multiLayerLocalStorage;

    @Before
    public void setUp() throws Exception {
        primaryLocalStorage = new InMemoryLocalStorage();
        secondaryLocalStorage = new SQLiteLocalStorage(ApplicationProvider.getApplicationContext(), "flushToDiskTest", 2024 * 2024 * 2024);
        multiLayerLocalStorage = new SwrveMultiLayerLocalStorage(primaryLocalStorage);
        multiLayerLocalStorage.setSecondaryStorage(secondaryLocalStorage);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        ApplicationProvider.getApplicationContext().deleteDatabase("flushToDiskTest");
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

    @Test
    public void testDeleteAllDataForUserId_RemovesAllUserData() throws Exception {
        String userId = "user1";

        // ---- seed primary (memory)
        primaryLocalStorage.addEvent(userId, "event1");
        primaryLocalStorage.addEvent(userId, "event2");
        primaryLocalStorage.setCacheEntry(userId, "cacheA", "valueA");

        // ---- seed secondary (db)
        secondaryLocalStorage.addEvent(userId, "event_db_1");
        secondaryLocalStorage.setCacheEntry(userId, "cacheB", "valueB");
        secondaryLocalStorage.saveOfflineCampaign(userId, "campaign1", "{}");
        secondaryLocalStorage.saveUser(new com.swrve.sdk.SwrveUser(userId, "external1", true));

        // sanity check
        assertEquals(1, secondaryLocalStorage.getFirstNEvents(10, userId).size());
        assertEquals("valueB", secondaryLocalStorage.getCacheItem(userId, "cacheB").rawData);

        // ---- delete
        multiLayerLocalStorage.deleteAllDataForUserId(userId);

        // ---- verify events removed
        assertEquals(0, primaryLocalStorage.getFirstNEvents(10, userId).size());
        assertEquals(0, secondaryLocalStorage.getFirstNEvents(10, userId).size());

        // ---- verify cache removed
        Assert.assertNull(primaryLocalStorage.getCacheItem(userId, "cacheA"));
        Assert.assertNull(secondaryLocalStorage.getCacheItem(userId, "cacheB"));

        // ---- verify campaigns removed
        Assert.assertNull(secondaryLocalStorage.getOfflineCampaign(userId, "campaign1"));

        // ---- verify user removed
        Assert.assertNull(secondaryLocalStorage.getUserBySwrveUserId(userId));
    }

    @Test
    public void testDeleteAllDataForUserId_DoesNotAffectOtherUsers() throws Exception {
        String user1 = "user1";
        String user2 = "user2";

        primaryLocalStorage.addEvent(user1, "event1");
        primaryLocalStorage.addEvent(user2, "event2");

        secondaryLocalStorage.addEvent(user1, "event_db_1");
        secondaryLocalStorage.addEvent(user2, "event_db_2");

        secondaryLocalStorage.setCacheEntry(user1, "cache", "u1");
        secondaryLocalStorage.setCacheEntry(user2, "cache", "u2");

        multiLayerLocalStorage.deleteAllDataForUserId(user1);

        // user1 wiped
        assertEquals(0, primaryLocalStorage.getFirstNEvents(10, user1).size());
        assertEquals(0, secondaryLocalStorage.getFirstNEvents(10, user1).size());
        Assert.assertNull(secondaryLocalStorage.getCacheItem(user1, "cache"));

        // user2 untouched
        assertEquals(1, primaryLocalStorage.getFirstNEvents(10, user2).size());
        assertEquals(1, secondaryLocalStorage.getFirstNEvents(10, user2).size());
        assertEquals("u2", secondaryLocalStorage.getCacheItem(user2, "cache").rawData);
    }

    @Test
    public void testDeleteAllDataForUserId_NoData_NoCrash() {
        String userId = "nonexistent_user";

        try {
            multiLayerLocalStorage.deleteAllDataForUserId(userId);
        } catch (Exception e) {
            fail("deleteAllDataForUserId should not throw when user has no data");
        }
    }
}
