package com.swrve.sdk.localstorage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.swrve.sdk.ISwrve;
import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.SwrveTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.CACHE_COLUMN_CATEGORY;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.CACHE_COLUMN_RAW_DATA;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.CACHE_COLUMN_USER_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.CACHE_TABLE_NAME;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.EVENTS_COLUMN_EVENT;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.EVENTS_COLUMN_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.EVENTS_COLUMN_USER_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.EVENTS_TABLE_NAME;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.SWRVE_DB_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SwrveSQLiteOpenHelperTest extends SwrveBaseTest {

    @Test
    public void testNewDatabase() {
        // test brand new instance that table created ok
        String dbName = "testNewDatabase";
        SwrveSQLiteOpenHelper sqLiteOpenHelper = SwrveSQLiteOpenHelper.getInstance(RuntimeEnvironment.application, dbName, SWRVE_DB_VERSION);
        SQLiteDatabase database = sqLiteOpenHelper.getWritableDatabase();

        Cursor cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table'", null);
        assertEquals("Should be 4 tables in database.", 4, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='android_metadata'", null);
        assertEquals("Should be 1 table called android_metadata in database.", 1, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='" + EVENTS_TABLE_NAME + "'", null);
        assertEquals("Should be 1 table called events in database.", 1, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='" + CACHE_TABLE_NAME + "'", null);
        assertEquals("Should be 1 table called cache in database.", 1, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='sqlite_sequence'", null);
        assertEquals("Should be 1 table called sqlite_sequence in database.", 1, cursor.getCount());

        cursor = database.rawQuery("SELECT * FROM " + EVENTS_TABLE_NAME, null);
        String[] columnNames = cursor.getColumnNames();
        assertEquals("Should only be 3 columns '_id', 'event', and 'user_id'", 3, columnNames.length);
        assertTrue("onCreate of tables failed as there should be a _id column", Arrays.asList(columnNames).contains(EVENTS_COLUMN_ID));
        assertTrue("onCreate of tables failed as there should be a user_id column", Arrays.asList(columnNames).contains(EVENTS_COLUMN_USER_ID));
        assertTrue("onCreate of tables failed as there should be a event column", Arrays.asList(columnNames).contains(EVENTS_COLUMN_EVENT));
        assertEquals("Should be 0 rows.", 0, cursor.getCount());

        cursor = database.rawQuery("SELECT * FROM " + CACHE_TABLE_NAME, null);
        columnNames = cursor.getColumnNames();
        assertEquals("Should only be 3 columns 'user_id', 'category', and 'raw_data'", 3, columnNames.length);
        assertTrue("onCreate of tables failed as there should be a _id column", Arrays.asList(columnNames).contains(CACHE_COLUMN_USER_ID));
        assertTrue("onCreate of tables failed as there should be a category column", Arrays.asList(columnNames).contains(CACHE_COLUMN_CATEGORY));
        assertTrue("onCreate of tables failed as there should be a raw_data column", Arrays.asList(columnNames).contains(CACHE_COLUMN_RAW_DATA));
        assertEquals("Should be 0 rows.", 0, cursor.getCount());

        database.close();
    }

    @Test
    public void testOnUpgrade1_cache() throws Exception{

        // for the purpose of this unit test, use a different database name than swrve, but let the
        // SwrveSQLiteOpenHelper use the userId from SwrveSDK
        String dbName = "testOnUpgrade1_cache";
        ISwrve swrve = SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        SwrveTestUtils.setSDKInstance(swrve);
        SwrveTestUtils.onCreate(swrve, mActivity);

        SwrveSQLiteOpenHelper_v1 swrveSQLiteOpenHelper_v1 = new SwrveSQLiteOpenHelper_v1(RuntimeEnvironment.application, dbName);
        SQLiteDatabase database = swrveSQLiteOpenHelper_v1.getWritableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='server_cache'", null);
        assertEquals("Should be 1 table called server_cache in database v1.", 1, cursor.getCount());

        long rowId = insertSharedCacheEntryDb1(database, "SwrveSDK.installTime");
        assertEquals("RowId should have incremented to 1.", 1, rowId);
        rowId = insertSharedCacheEntryDb1(database, "seqnum");
        assertEquals("RowId should have incremented to 2.", 2, rowId);
        rowId = insertSharedCacheEntryDb1(database, "RegistrationId");
        assertEquals("RowId should have incremented to 3.", 3, rowId);
        rowId = insertSharedCacheEntryDb1(database, "AppVersion");
        assertEquals("RowId should have incremented to 4.", 4, rowId);
        rowId = insertSharedCacheEntryDb1(database, "GoogleAdvertisingId");
        assertEquals("RowId should have incremented to 5.", 5, rowId);
        rowId = insertSharedCacheEntryDb1(database, "GoogleAdvertisingLimitAdTrackingEnabled");
        assertEquals("RowId should have incremented to 6.", 6, rowId);
        rowId = insertSharedCacheEntryDb1(database, "device_id");
        assertEquals("RowId should have incremented to 7.", 7, rowId);

        database.close();

        // increment db version to 2 to trigger onUpgrade
        SwrveSQLiteOpenHelper sqLiteOpenHelper = new SwrveSQLiteOpenHelper(RuntimeEnvironment.application, dbName, 2);
        database = sqLiteOpenHelper.getWritableDatabase();
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='server_cache'", null);
        assertEquals("Should be no table called server_cache in database v2.", 0, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='" + CACHE_TABLE_NAME + "'", null);
        assertEquals("Should be one table called server_cache in database v2.", 1, cursor.getCount());

        assertCacheUserIdUpdated(database, SwrveSDK.getUserId(), "SwrveSDK.installTime");
        assertCacheUserIdUpdated(database, SwrveSDK.getUserId(), "seqnum");
        assertCacheUserIdUpdated(database, SwrveSDK.getUserId(), "RegistrationId");
        assertCacheUserIdUpdated(database, SwrveSDK.getUserId(), "AppVersion");
        assertCacheUserIdUpdated(database, SwrveSDK.getUserId(), "GoogleAdvertisingId");
        assertCacheUserIdUpdated(database, SwrveSDK.getUserId(), "GoogleAdvertisingLimitAdTrackingEnabled");
        assertCacheUserIdUpdated(database, "", "device_id"); // userId for deviceId is blank

        database.close();
    }

    // database v1 repeated/stored the category value in the userId column in the old tablename 'server_cache'
    private long insertSharedCacheEntryDb1(SQLiteDatabase database, String categoryValue){
        ContentValues values = new ContentValues();
        values.put(CACHE_COLUMN_USER_ID, categoryValue);
        values.put(CACHE_COLUMN_CATEGORY, categoryValue);
        values.put(CACHE_COLUMN_RAW_DATA, "12345");
        return database.insertOrThrow("server_cache", null, values);
    }

    private void assertCacheUserIdUpdated(SQLiteDatabase database, String userId, String categoryValue){
        Cursor cursor = database.rawQuery("SELECT * FROM " + CACHE_TABLE_NAME + " WHERE " + CACHE_COLUMN_CATEGORY + "='" + categoryValue + "'", null);
        assertEquals("Should be one row with category=" + categoryValue, 1, cursor.getCount());
        cursor.moveToFirst();
        String userIdEntry = cursor.getString(cursor.getColumnIndex(CACHE_COLUMN_USER_ID));
        assertEquals(categoryValue + " should now have the current user: " + userId, userIdEntry, userId);
    }

    @Test
    public void testOnUpgrade1_events() throws Exception{

        // for the purpose of this unit test, use a different database name than swrve, but let the
        // SwrveSQLiteOpenHelper use the userId from SwrveSDK
        String dbName = "testOnUpgrade1_events";
        ISwrve swrve = SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        SwrveTestUtils.setSDKInstance(swrve);
        SwrveTestUtils.onCreate(swrve, mActivity);

        SwrveSQLiteOpenHelper_v1 swrveSQLiteOpenHelper_v1 = new SwrveSQLiteOpenHelper_v1(RuntimeEnvironment.application, dbName);
        SQLiteDatabase database = swrveSQLiteOpenHelper_v1.getWritableDatabase();
        long rowId = 0;
        for (int i = 0; i < 50; i++) {
            ContentValues values = new ContentValues();
            values.put(EVENTS_COLUMN_ID, i);
            values.put(EVENTS_COLUMN_EVENT, "eventJSON" + i);
            rowId = database.insertOrThrow(EVENTS_TABLE_NAME, null, values);
        }
        assertEquals("RowId should have incremented to 49.", 49, rowId);
        Cursor cursor = database.rawQuery("SELECT * FROM " + EVENTS_TABLE_NAME, null);
        String[] columnNames = cursor.getColumnNames();
        assertEquals("Should only be 2 columns '_id' and 'event'", 2, columnNames.length);
        database.close();

        // increment db version to 2 to trigger onUpgrade
        SwrveSQLiteOpenHelper sqLiteOpenHelper = new SwrveSQLiteOpenHelper(RuntimeEnvironment.application, dbName, 2);
        database = sqLiteOpenHelper.getWritableDatabase();

        cursor = database.rawQuery("SELECT * FROM " + EVENTS_TABLE_NAME, null);
        columnNames = cursor.getColumnNames();
        assertEquals("Should only be 3 columns '_id', 'event', and 'user_id'", 3, columnNames.length);
        assertTrue("upgrade failed as there should be a user_id column", Arrays.asList(columnNames).contains(EVENTS_COLUMN_USER_ID));
        assertEquals("Should be 50 rows.", 50, cursor.getCount());
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast() == false) {
                String userId = cursor.getString(cursor.getColumnIndex(EVENTS_COLUMN_USER_ID));
                assertEquals("All values should be upgraded to the current userId.", userId, SwrveSDK.getUserId());
                cursor.moveToNext();
            }
        }
        database.close();
    }

    private static class SwrveSQLiteOpenHelper_v1 extends SQLiteOpenHelper {
        public static final int SWRVE_DB_VERSION = 1;
        public static final String TABLE_EVENTS_JSON = "events";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_EVENT = "event";
        public static final String TABLE_CACHE = "server_cache";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_RAW_DATA = "raw_data";

        public SwrveSQLiteOpenHelper_v1(Context context, String dbName) {
            super(context, dbName, null, SWRVE_DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_EVENTS_JSON + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_EVENT + " TEXT NOT NULL);");
            db.execSQL("CREATE TABLE " + TABLE_CACHE + " (" + COLUMN_USER_ID + " TEXT NOT NULL, " + COLUMN_CATEGORY + " TEXT NOT NULL, " + COLUMN_RAW_DATA + " TEXT NOT NULL, " + "PRIMARY KEY (" + COLUMN_USER_ID + "," + COLUMN_CATEGORY + "));");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // no upgrade in first version
        }
    }
}
