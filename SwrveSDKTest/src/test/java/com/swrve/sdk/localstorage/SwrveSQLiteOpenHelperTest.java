package com.swrve.sdk.localstorage;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.Swrve;
import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.SwrveTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.CACHE_COLUMN_CATEGORY;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.CACHE_COLUMN_RAW_DATA;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.CACHE_COLUMN_USER_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.CACHE_TABLE_NAME;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.EVENTS_COLUMN_EVENT;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.EVENTS_COLUMN_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.EVENTS_COLUMN_USER_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.EVENTS_TABLE_NAME;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.NOTIFICATIONS_AUTHENTICATED_COLUMN_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.NOTIFICATIONS_AUTHENTICATED_COLUMN_TIME;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.NOTIFICATIONS_AUTHENTICATED_TABLE_NAME;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.OFFLINE_CAMPAIGNS_COLUMN_CAMPAIGN_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.OFFLINE_CAMPAIGNS_COLUMN_JSON;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.OFFLINE_CAMPAIGNS_COLUMN_SWRVE_USER_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.OFFLINE_CAMPAIGNS_TABLE_NAME;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.SWRVE_DB_VERSION;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.USER_COLUMN_EXTERNAL_USER_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.USER_COLUMN_SWRVE_USER_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.USER_COLUMN_VERFIED;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.USER_TABLE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

public class SwrveSQLiteOpenHelperTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveMock = Mockito.mock(Swrve.class);
        doReturn("some_user_id").when(swrveMock).getUserId();
        SwrveTestUtils.setSDKInstance(swrveMock);
    }

    @Test
    public void testNewDatabase() {
        // test brand new instance that table created ok
        String dbName = "testNewDatabase";
        SwrveSQLiteOpenHelper sqLiteOpenHelper = SwrveSQLiteOpenHelper.getInstance(ApplicationProvider.getApplicationContext(), dbName, SWRVE_DB_VERSION);
        SQLiteDatabase database = sqLiteOpenHelper.getWritableDatabase();

        Cursor cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table'", null);
        assertEquals("Should be 6 tables in database.", 7, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='android_metadata'", null);
        assertEquals("Should be 1 table called android_metadata in database.", 1, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='" + EVENTS_TABLE_NAME + "'", null);
        assertEquals("Should be 1 table called events in database.", 1, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='" + CACHE_TABLE_NAME + "'", null);
        assertEquals("Should be 1 table called cache in database.", 1, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='sqlite_sequence'", null);
        assertEquals("Should be 1 table called sqlite_sequence in database.", 1, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='" + USER_TABLE_NAME + "'", null);
        assertEquals("Should be 1 table called sqlite_sequence in database.", 1, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='" + NOTIFICATIONS_AUTHENTICATED_TABLE_NAME + "'", null);
        assertEquals("Should be 1 table called sqlite_sequence in database.", 1, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='" + OFFLINE_CAMPAIGNS_TABLE_NAME + "'", null);
        assertEquals("Should be 1 table called offline_campaigns in database.", 1, cursor.getCount());

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

        cursor = database.rawQuery("SELECT * FROM " + USER_TABLE_NAME, null);
        columnNames = cursor.getColumnNames();
        assertEquals("Should only be 3 columns 'swrve_user_id', 'external_user_id', and 'verified'", 3, columnNames.length);
        assertTrue("onCreate of tables failed as there should be a swrve_user_id column", Arrays.asList(columnNames).contains(USER_COLUMN_SWRVE_USER_ID));
        assertTrue("onCreate of tables failed as there should be a external_user_id column", Arrays.asList(columnNames).contains(USER_COLUMN_EXTERNAL_USER_ID));
        assertTrue("onCreate of tables failed as there should be a verified column", Arrays.asList(columnNames).contains(USER_COLUMN_VERFIED));
        assertEquals("Should be 0 rows.", 0, cursor.getCount());

        cursor = database.rawQuery("SELECT * FROM " + NOTIFICATIONS_AUTHENTICATED_TABLE_NAME, null);
        columnNames = cursor.getColumnNames();
        assertEquals("Should only be 2 column 'notification_id', and 'time'", 2, columnNames.length);
        assertTrue("onCreate of tables failed as there should be a notification_id column", Arrays.asList(columnNames).contains(NOTIFICATIONS_AUTHENTICATED_COLUMN_ID));
        assertTrue("onCreate of tables failed as there should be a time column", Arrays.asList(columnNames).contains(NOTIFICATIONS_AUTHENTICATED_COLUMN_TIME));
        assertEquals("Should be 0 rows.", 0, cursor.getCount());

        cursor = database.rawQuery("SELECT * FROM " + OFFLINE_CAMPAIGNS_TABLE_NAME, null);
        columnNames = cursor.getColumnNames();
        assertEquals("Should only be 3 columns 'user_id', 'campaign_id', and 'campaign_json'", 3, columnNames.length);
        assertTrue("onCreate of tables failed as there should be a user_id column", Arrays.asList(columnNames).contains(OFFLINE_CAMPAIGNS_COLUMN_SWRVE_USER_ID));
        assertTrue("onCreate of tables failed as there should be a campaign_id column", Arrays.asList(columnNames).contains(OFFLINE_CAMPAIGNS_COLUMN_CAMPAIGN_ID));
        assertTrue("onCreate of tables failed as there should be a campaign_json column", Arrays.asList(columnNames).contains(OFFLINE_CAMPAIGNS_COLUMN_JSON));
        assertEquals("Should be 0 rows.", 0, cursor.getCount());


        database.close();
    }

    @Test
    public void testOnUpgradeCache_1_to_2_to_Latest() {

        String dbName = "testOnUpgrade_cache";

        //Set fake etag
        SharedPreferences settings = ApplicationProvider.getApplicationContext().getSharedPreferences("swrve_prefs", 0);
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putString("campaigns_and_resources_etag", "ExistingEtag");
        settingsEditor.apply();

        SwrveSQLiteOpenHelper_v1 swrveSQLiteOpenHelper_v1 = new SwrveSQLiteOpenHelper_v1(ApplicationProvider.getApplicationContext(), dbName);
        SQLiteDatabase database = swrveSQLiteOpenHelper_v1.getWritableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='server_cache'", null);
        assertEquals("Should be 1 table called server_cache in database v1.", 1, cursor.getCount());

        long rowId = insertSharedCacheEntryDb1(database, "SwrveSDK.installTime", "12345");
        assertEquals("RowId should have incremented to 1.", 1, rowId);
        rowId = insertSharedCacheEntryDb1(database, "seqnum", "12345");
        assertEquals("RowId should have incremented to 2.", 2, rowId);
        rowId = insertSharedCacheEntryDb1(database, "RegistrationId", "12345");
        assertEquals("RowId should have incremented to 3.", 3, rowId);
        rowId = insertSharedCacheEntryDb1(database, "AppVersion", "12345");
        assertEquals("RowId should have incremented to 4.", 4, rowId);
        rowId = insertSharedCacheEntryDb1(database, "GoogleAdvertisingId", "12345");
        assertEquals("RowId should have incremented to 5.", 5, rowId);
        rowId = insertSharedCacheEntryDb1(database, "GoogleAdvertisingLimitAdTrackingEnabled", "12345");
        assertEquals("RowId should have incremented to 6.", 6, rowId);
        rowId = insertSharedCacheEntryDb1(database, "device_id", "12345");
        assertEquals("RowId should have incremented to 7.", 7, rowId);

        database.close();

        // increment db version to 2 to trigger onUpgrade
        SwrveSQLiteOpenHelper_v2 swrveSQLiteOpenHelper_v2 = new SwrveSQLiteOpenHelper_v2(ApplicationProvider.getApplicationContext(), dbName);
        database = swrveSQLiteOpenHelper_v2.getWritableDatabase();
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='server_cache'", null);
        assertEquals("Should be no table called server_cache in database v2.", 0, cursor.getCount());
        cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table' and name='cache'", null);
        assertEquals("Should be one table called server_cache in database v2.", 1, cursor.getCount());

        assertCacheUserIdUpdated(database, SwrveSDK.getUserId(), "SwrveSDK.installTime");
        assertCacheUserIdUpdated(database, SwrveSDK.getUserId(), "seqnum");
        assertCacheUserIdUpdated(database, SwrveSDK.getUserId(), "RegistrationId");
        assertCacheUserIdUpdated(database, SwrveSDK.getUserId(), "AppVersion");
        assertCacheUserIdUpdated(database, SwrveSDK.getUserId(), "GoogleAdvertisingId");
        assertCacheUserIdUpdated(database, SwrveSDK.getUserId(), "GoogleAdvertisingLimitAdTrackingEnabled");
        assertCacheUserIdUpdated(database, "", "device_id"); // userId for deviceId is blank
        database.close();

        // increment db version to latest by using SwrveSQLiteOpenHelper
        SwrveSQLiteOpenHelper sqLiteOpenHelper = new SwrveSQLiteOpenHelper(ApplicationProvider.getApplicationContext(), dbName, 3);
        database = sqLiteOpenHelper.getWritableDatabase();
        assertCacheEntry(1, database, SwrveSDK.getUserId(), "swrve.etag", "ExistingEtag");
        assertCacheEntry(1, database, "", "SwrveSDK.installTime", "12345"); // blank userId on purpose
        assertCacheEntry(1, database, SwrveSDK.getUserId(), "SwrveSDK.userJoinedTime", "12345");
        assertCacheEntry(0, database, "", "SwrveSDK.installTime", "");

        database.close();
    }

    // database v1 repeated/stored the category value in the userId column in the old tablename 'server_cache'
    private long insertSharedCacheEntryDb1(SQLiteDatabase database, String categoryValue, String rawData) {
        ContentValues values = new ContentValues();
        values.put("user_id", categoryValue);
        values.put("category", categoryValue);
        values.put("raw_data", rawData);
        return database.insertOrThrow("server_cache", null, values);
    }

    private void assertCacheUserIdUpdated(SQLiteDatabase database, String userId, String categoryValue){
        Cursor cursor = database.rawQuery("SELECT * FROM cache WHERE category='" + categoryValue + "'", null);
        assertEquals("Should be one row with category=" + categoryValue, 1, cursor.getCount());
        cursor.moveToFirst();
        String userIdEntry = cursor.getString(cursor.getColumnIndex("user_id"));
        assertEquals(categoryValue + " should now have the current user: " + userId, userIdEntry, userId);
    }

    private void assertCacheEntry(int rows, SQLiteDatabase database, String userId, String categoryValue, String rawData) {
        Cursor cursor = database.rawQuery("SELECT * FROM cache WHERE category='" + categoryValue + "' AND raw_data='" + rawData + "'", null);
        String msg = "Should be " + rows + " row with user_id=" + userId + " category=" + categoryValue + " raw_data=" + rawData;
        assertEquals(msg, rows, cursor.getCount());
    }

    @Test
    public void testOnUpgradeEvents_1_to_Latest() {

        String dbName = "testOnUpgradeEvents_1_to_3";

        SwrveSQLiteOpenHelper_v1 swrveSQLiteOpenHelper_v1 = new SwrveSQLiteOpenHelper_v1(ApplicationProvider.getApplicationContext(), dbName);
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

        // increment db version to latest by using SwrveSQLiteOpenHelper
        SwrveSQLiteOpenHelper sqLiteOpenHelper = new SwrveSQLiteOpenHelper(ApplicationProvider.getApplicationContext(), dbName, 3);
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

    @Test
    public void testOnUpgradeUsers_1_to_Latest() {

        String dbName = "testOnUpgradeUsers_1_to_3";

        SwrveSQLiteOpenHelper_v1 swrveSQLiteOpenHelper_v1 = new SwrveSQLiteOpenHelper_v1(ApplicationProvider.getApplicationContext(), dbName);
        SQLiteDatabase database = swrveSQLiteOpenHelper_v1.getWritableDatabase();
        database.close();

        // increment db version to latest by using SwrveSQLiteOpenHelper
        SwrveSQLiteOpenHelper sqLiteOpenHelper = new SwrveSQLiteOpenHelper(ApplicationProvider.getApplicationContext(), dbName, 3);
        database = sqLiteOpenHelper.getWritableDatabase();

        Cursor cursor = database.rawQuery("SELECT * FROM users", null);
        String[] columnNames = cursor.getColumnNames();
        assertEquals("Should only be 3 columns 'swrve_user_id', 'external_user_id', and 'verified'", 3, columnNames.length);
        database.close();
    }

    @Test
    public void testOnUpgradeNotificationsAuthenticated_1_to_Latest() {

        String dbName = "testOnUpgradeNotificationsAuthenticated_1_to_3";

        SwrveSQLiteOpenHelper_v1 swrveSQLiteOpenHelper_v1 = new SwrveSQLiteOpenHelper_v1(ApplicationProvider.getApplicationContext(), dbName);
        SQLiteDatabase database = swrveSQLiteOpenHelper_v1.getWritableDatabase();
        database.close();

        // increment db version to latest by using SwrveSQLiteOpenHelper
        SwrveSQLiteOpenHelper sqLiteOpenHelper = new SwrveSQLiteOpenHelper(ApplicationProvider.getApplicationContext(), dbName, 3);
        database = sqLiteOpenHelper.getWritableDatabase();

        Cursor cursor = database.rawQuery("SELECT * FROM notifications_authenticated", null);
        String[] columnNames = cursor.getColumnNames();
        assertEquals("Should only be 2 columns 'notification_id', and 'time'", 2, columnNames.length);
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

    private static class SwrveSQLiteOpenHelper_v2 extends SQLiteOpenHelper {
        public static final int SWRVE_DB_VERSION = 2;

        public SwrveSQLiteOpenHelper_v2(Context context, String dbName) {
            super(context, dbName, null, SWRVE_DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // onCreate not needed for this test setup
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            String userId = SwrveSDK.getUserId();
            db.execSQL("ALTER TABLE events ADD COLUMN user_id TEXT");
            if (SwrveHelper.isNotNullOrEmpty(userId)) {
                db.execSQL("UPDATE events SET user_id=" + "'" + userId + "' " +
                        "WHERE user_id IS NULL OR user_id = ''");
            } else {
                db.execSQL("DELETE FROM events"); // these events are orphaned so should be removed.
            }

            db.execSQL("ALTER TABLE server_cache RENAME TO cache");
            if (SwrveHelper.isNotNullOrEmpty(userId)) {
                db.execSQL("UPDATE cache SET user_id='" + userId + "' WHERE user_id='SwrveSDK.installTime'");
                db.execSQL("UPDATE cache SET user_id='" + userId + "' WHERE user_id='seqnum'");
                db.execSQL("UPDATE cache SET user_id='" + userId + "' WHERE user_id='RegistrationId'");
                db.execSQL("UPDATE cache SET user_id='" + userId + "' WHERE user_id='AppVersion'");
                db.execSQL("UPDATE cache SET user_id='" + userId + "' WHERE user_id='GoogleAdvertisingId'");
                db.execSQL("UPDATE cache SET user_id='" + userId + "' WHERE user_id='GoogleAdvertisingLimitAdTrackingEnabled'");
            }
            db.execSQL("UPDATE cache SET user_id='' WHERE user_id='device_id'");
        }
    }
}
