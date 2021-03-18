package com.swrve.sdk.localstorage;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class SwrveMultiLayerLocalStorageTest extends BaseLocalStorage {
    private LocalStorage primaryLocalStorage;
    private LocalStorage secondaryLocalStorage;
    private SwrveMultiLayerLocalStorage multiLayerLocalStorage;

    @Before
    public void setUp() throws Exception {
        primaryLocalStorage = new InMemoryLocalStorage();
        secondaryLocalStorage = new SQLiteLocalStorage(ApplicationProvider.getApplicationContext(), "flushToDiskTest", 2024 * 2024 * 2024);
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

    @Test
    public void testSaveNotificationAuthenicated() {
        secondaryLocalStorage.saveNotificationAuthenticated(1, 100); // oldest
        secondaryLocalStorage.saveNotificationAuthenticated(2, 200);
        secondaryLocalStorage.saveNotificationAuthenticated(3, 300);
        secondaryLocalStorage.saveNotificationAuthenticated(4, 400);
        secondaryLocalStorage.saveNotificationAuthenticated(5, 500);
        secondaryLocalStorage.saveNotificationAuthenticated(6, 600); // most recent

        List<Integer> notifications = secondaryLocalStorage.getNotificationsAuthenticated();
        assertEquals(6, notifications.size());

        multiLayerLocalStorage.NOTIFICATIONS_AUTHENICATED_MAX_ROWS = 6;

        multiLayerLocalStorage.saveNotificationAuthenticated(7);

        notifications = secondaryLocalStorage.getNotificationsAuthenticated();
        assertEquals(6, notifications.size());
        boolean foundId2 = false, foundId3 = false, foundId4 = false, foundId5 = false, foundId6 = false, foundId7 = false;;
        for (Integer notificationId : notifications) {
            if (notificationId == 1) {
                Assert.fail("testSaveNotificationAuthenicated failed oldest notification did not get truncated.");
            } else if (notificationId == 2) {
                foundId2 = true;
            } else if (notificationId == 3) {
                foundId3 = true;
            } else if (notificationId == 4) {
                foundId4 = true;
            } else if (notificationId == 5) {
                foundId5 = true;
            } else if (notificationId == 6) {
                foundId6 = true;
            } else if (notificationId == 7) {
                foundId7 = true;
            }
        }

        if (!foundId2 || !foundId3 || !foundId4 || !foundId5 || !foundId6 || !foundId7) {
            Assert.fail("testSaveNotificationAuthenicated failed because id's returned didn't match what was saved.");
        }

    }
}
