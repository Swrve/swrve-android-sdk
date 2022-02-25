package com.swrve.sdk.localstorage;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.SwrveUser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.NOTIFICATIONS_AUTHENTICATED_COLUMN_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.NOTIFICATIONS_AUTHENTICATED_TABLE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SQLiteLocalStorageTest extends BaseLocalStorage {

    int maxSize = 2024 * 1024 * 1024;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        localStorage = new SQLiteLocalStorage(ApplicationProvider.getApplicationContext(), "test", maxSize);
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

    @Test
    public void testSwrveUserCRUD() {

        SQLiteLocalStorage sqLiteLocalStorage = (SQLiteLocalStorage) localStorage;

        //CREATE
        SwrveUser swrveUser1 = new SwrveUser("swrveId1", "externalUserId1", true);
        sqLiteLocalStorage.saveUser(swrveUser1);

        SwrveUser swrveUser2 = new SwrveUser("swrveId2", "externalUserId2", false);
        sqLiteLocalStorage.saveUser(swrveUser2);

        assertNull(sqLiteLocalStorage.getUserBySwrveUserId("doesntExist"));
        assertNull(sqLiteLocalStorage.getUserByExternalUserId("doesntExist"));

        //READ
        swrveUser1 = sqLiteLocalStorage.getUserBySwrveUserId("swrveId1");
        assertNotNull(swrveUser1);
        assertEquals(swrveUser1.getSwrveUserId(), "swrveId1");
        assertEquals(swrveUser1.getExternalUserId(), "externalUserId1");
        assertEquals(swrveUser1.isVerified(), true);

        swrveUser2 = sqLiteLocalStorage.getUserByExternalUserId("externalUserId2");
        assertNotNull(swrveUser2);
        assertEquals(swrveUser2.getSwrveUserId(), "swrveId2");
        assertEquals(swrveUser2.getExternalUserId(), "externalUserId2");
        assertEquals(swrveUser2.isVerified(), false);

        //UPDATE saveUser does an insertOrUpdate
        swrveUser2.setVerified(true);
        sqLiteLocalStorage.saveUser(swrveUser2);
        SwrveUser updatedSwrveUser2 = sqLiteLocalStorage.getUserBySwrveUserId("swrveId2");
        assertEquals(updatedSwrveUser2.isVerified(), true);

        //DELETE
        sqLiteLocalStorage.deleteUser("swrveId2");
        assertNull(sqLiteLocalStorage.getUserBySwrveUserId("swrveId2"));
        assertNotNull(sqLiteLocalStorage.getUserBySwrveUserId("swrveId1"));
    }

    @Test
    public void testSaveNotificationAuthenicated() {
        SQLiteLocalStorage sqLiteLocalStorage = (SQLiteLocalStorage) localStorage;
        sqLiteLocalStorage.saveNotificationAuthenticated(1, 123);
        sqLiteLocalStorage.saveNotificationAuthenticated(2, 124);
        // add following entry twice on purpose because a notification can be updated
        sqLiteLocalStorage.saveNotificationAuthenticated(3, 125);
        sqLiteLocalStorage.saveNotificationAuthenticated(3, 126);
        SQLiteDatabase database = sqLiteLocalStorage.database;
        Cursor cursor = database.rawQuery("SELECT * FROM " + NOTIFICATIONS_AUTHENTICATED_TABLE_NAME, null);
        if (cursor.moveToFirst()) {
            int row = 1;
            while (cursor.isAfterLast() == false) {
                long notificationId = cursor.getLong(cursor.getColumnIndex(NOTIFICATIONS_AUTHENTICATED_COLUMN_ID));
                assertEquals(row, notificationId);
                row++;
                cursor.moveToNext();
            }
        } else {
            fail("testSaveCurrentNotifications failed because cursor is empty and should contain entries.");
        }
    }

    @Test
    public void testGetNotificationsAuthenicated() {
        SQLiteLocalStorage sqLiteLocalStorage = (SQLiteLocalStorage) localStorage;
        sqLiteLocalStorage.saveNotificationAuthenticated(1, 123);
        sqLiteLocalStorage.saveNotificationAuthenticated(2, 124);
        // add following entry twice on purpose because a notification can be updated
        sqLiteLocalStorage.saveNotificationAuthenticated(3, 125);
        sqLiteLocalStorage.saveNotificationAuthenticated(3, 126);

        List<Integer> notifications = sqLiteLocalStorage.getNotificationsAuthenticated();
        assertEquals(3, notifications.size());
        boolean foundId1 = false, foundId2 = false, foundId3 = false;
        for (Integer notificationId : notifications) {
            if (notificationId == 1) {
                foundId1 = true;
            } else if (notificationId == 2) {
                foundId2 = true;
            } else if (notificationId == 3) {
                foundId3 = true;
            }
        }
        if (!foundId1 || !foundId2 || !foundId3) {
            fail("testGetCurrentNotifications failed because id's returned didn't match what was saved.");
        }
    }

    @Test
    public void testDeleteNotificationsAuthenicated() {
        SQLiteLocalStorage sqLiteLocalStorage = (SQLiteLocalStorage) localStorage;
        sqLiteLocalStorage.saveNotificationAuthenticated(1, 123);
        sqLiteLocalStorage.saveNotificationAuthenticated(2, 124);
        sqLiteLocalStorage.saveNotificationAuthenticated(3, 125);

        List<Integer> notifications = sqLiteLocalStorage.getNotificationsAuthenticated();
        assertEquals(3, notifications.size());

        sqLiteLocalStorage.deleteNotificationsAuthenticated();
        notifications = sqLiteLocalStorage.getNotificationsAuthenticated();
        assertEquals(0, notifications.size());
    }

    @Test
    public void testTruncateNotificationsAuthenicated() {
        SQLiteLocalStorage sqLiteLocalStorage = (SQLiteLocalStorage) localStorage;
        sqLiteLocalStorage.saveNotificationAuthenticated(1, 100); // oldest
        sqLiteLocalStorage.saveNotificationAuthenticated(2, 200);
        sqLiteLocalStorage.saveNotificationAuthenticated(3, 300);
        sqLiteLocalStorage.saveNotificationAuthenticated(4, 400);
        sqLiteLocalStorage.saveNotificationAuthenticated(5, 500);
        sqLiteLocalStorage.saveNotificationAuthenticated(6, 600); // most recent

        List<Integer> notifications = sqLiteLocalStorage.getNotificationsAuthenticated();
        assertEquals(6, notifications.size());

        sqLiteLocalStorage.truncateNotificationsAuthenticated(4);
        notifications = sqLiteLocalStorage.getNotificationsAuthenticated();
        assertEquals(4, notifications.size());

        boolean foundId3 = false, foundId4 = false, foundId5 = false, foundId6 = false;
        for (Integer notificationId : notifications) {
            if (notificationId == 1) {
                Assert.fail("testTruncateNotificationsAuthenicated failed oldest notification did not get truncated.");
            } else if (notificationId == 2) {
                Assert.fail("testTruncateNotificationsAuthenicated failed oldest notification did not get truncated.");
            } else if (notificationId == 3) {
                foundId3 = true;
            } else if (notificationId == 4) {
                foundId4 = true;
            } else if (notificationId == 5) {
                foundId5 = true;
            } else if (notificationId == 6) {
                foundId6 = true;
            }
        }
        if (!foundId3 || !foundId4 || !foundId5 || !foundId6) {
            Assert.fail("testSaveNotificationAuthenicated failed because id's returned didn't match what was saved.");
        }
    }

    @Test
    public void testTruncateNotificationsAuthenicatedEmpty() {
        SQLiteLocalStorage sqLiteLocalStorage = (SQLiteLocalStorage) localStorage;
        List<Integer> notifications = sqLiteLocalStorage.getNotificationsAuthenticated();
        assertEquals(0, notifications.size());
        sqLiteLocalStorage.truncateNotificationsAuthenticated(4);
        notifications = sqLiteLocalStorage.getNotificationsAuthenticated();
        assertEquals(0, notifications.size());
    }

}
