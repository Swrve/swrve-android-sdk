package com.swrve.sdk.localstorage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveSDK;

final class SwrveSQLiteOpenHelper extends SQLiteOpenHelper {

    private static SwrveSQLiteOpenHelper instance;
    private static final Object OPEN_HELPER_LOCK = new Object();

    // Database
    protected static final int SWRVE_DB_VERSION = 2;

    // Events table
    protected static final String EVENTS_TABLE_NAME = "events";
    protected static final String EVENTS_COLUMN_ID = "_id";
    protected static final String EVENTS_COLUMN_EVENT = "event";
    protected static final String EVENTS_COLUMN_USER_ID = "user_id";

    // Cache table
    protected static final String CACHE_TABLE_NAME = "cache";
    protected static final String CACHE_COLUMN_USER_ID = "user_id";
    protected static final String CACHE_COLUMN_CATEGORY = "category";
    protected static final String CACHE_COLUMN_RAW_DATA = "raw_data";

    SwrveSQLiteOpenHelper(Context context, String dbName, int version) {
        super(context, dbName, null, version);
    }

    static SwrveSQLiteOpenHelper getInstance(Context context, String dbName, int version) {
        synchronized (OPEN_HELPER_LOCK) {
            if (instance == null) {
                instance = new SwrveSQLiteOpenHelper(context, dbName, version);
            }
            return instance;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + EVENTS_TABLE_NAME + " (" +
                EVENTS_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                EVENTS_COLUMN_USER_ID + " TEXT NOT NULL, " +
                EVENTS_COLUMN_EVENT + " TEXT NOT NULL" +
                ");");

        db.execSQL("CREATE TABLE " + CACHE_TABLE_NAME + " (" +
                CACHE_COLUMN_USER_ID + " TEXT NOT NULL, " +
                CACHE_COLUMN_CATEGORY + " TEXT NOT NULL, " +
                CACHE_COLUMN_RAW_DATA + " TEXT NOT NULL, " +
                "PRIMARY KEY (" + CACHE_COLUMN_USER_ID + "," + CACHE_COLUMN_CATEGORY + ")" +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // do not add break to this switch statement so execution will start at oldVersion, and run straight through to the latest
        switch (oldVersion) {
            case 1:
                String userId = SwrveSDK.getUserId();
                db.execSQL("ALTER TABLE events ADD COLUMN user_id TEXT");
                if(SwrveHelper.isNotNullOrEmpty(userId)) {
                    db.execSQL("UPDATE events SET user_id=" + "'" + userId + "' " +
                            "WHERE user_id IS NULL OR user_id = ''");
                } else {
                    db.execSQL("DELETE FROM events"); // these events are orphaned so should be removed.
                }

                db.execSQL("ALTER TABLE server_cache RENAME TO cache");
                if(SwrveHelper.isNotNullOrEmpty(userId)) {
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

    @Override
    public synchronized void close() {
        synchronized (OPEN_HELPER_LOCK) {
            super.close();
            instance = null;
        }
    }
}
