package com.swrve.sdk.localstorage;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.VisibleForTesting;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveSDK;

final class SwrveSQLiteOpenHelper extends SQLiteOpenHelper {

    private static SwrveSQLiteOpenHelper instance;
    private static final Object OPEN_HELPER_LOCK = new Object();
    private Context context;

    // Database
    protected static final int SWRVE_DB_VERSION = 5;

    // User table
    protected static final String USER_TABLE_NAME = "users";
    protected static final String USER_COLUMN_SWRVE_USER_ID = "swrve_user_id";
    protected static final String USER_COLUMN_EXTERNAL_USER_ID = "external_user_id";
    protected static final String USER_COLUMN_VERFIED = "verified";

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

    // Authenticated notifications table
    protected static final String NOTIFICATIONS_AUTHENTICATED_TABLE_NAME = "notifications_authenticated";
    protected static final String NOTIFICATIONS_AUTHENTICATED_COLUMN_ID = "notification_id";
    protected static final String NOTIFICATIONS_AUTHENTICATED_COLUMN_TIME = "time";

    // External Campaign table
    protected static final String OFFLINE_CAMPAIGNS_TABLE_NAME = "offline_campaigns";
    protected static final String OFFLINE_CAMPAIGNS_COLUMN_SWRVE_USER_ID = "user_id";
    protected static final String OFFLINE_CAMPAIGNS_COLUMN_CAMPAIGN_ID = "campaign_id";
    protected static final String OFFLINE_CAMPAIGNS_COLUMN_JSON = "campaign_json";

    // Asset logs table
    protected static final String ASSET_LOGS_TABLE_NAME = "asset_logs";
    protected static final String ASSET_LOGS_COLUMN_NAME = "name";
    protected static final String ASSET_LOGS_COLUMN_DOWNLOAD_COUNT = "download_count";
    protected static final String ASSET_LOGS_COLUMN_LAST_DOWNLOAD_TIME = "last_download_time";

    SwrveSQLiteOpenHelper(Context context, String dbName, int version) {
        super(context, dbName, null, version);
        this.context = context;
    }

    static SwrveSQLiteOpenHelper getInstance(Context context, String dbName, int version) {
        synchronized (OPEN_HELPER_LOCK) {
            if (instance == null) {
                instance = new SwrveSQLiteOpenHelper(context, dbName, version);
            }
            return instance;
        }
    }

    @VisibleForTesting
    static void closeInstance() {
        synchronized (OPEN_HELPER_LOCK) {
            if (instance != null) {
                instance.close();
                instance = null;
            }
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

        db.execSQL("CREATE TABLE " + USER_TABLE_NAME + " (" +
                USER_COLUMN_SWRVE_USER_ID + " TEXT NOT NULL, " +
                USER_COLUMN_EXTERNAL_USER_ID + " TEXT NOT NULL," +
                USER_COLUMN_VERFIED + " BOOL NOT NULL," +
                "PRIMARY KEY (" + USER_COLUMN_SWRVE_USER_ID + "," + USER_COLUMN_EXTERNAL_USER_ID + ")" +
                ");");

        db.execSQL("CREATE TABLE " + NOTIFICATIONS_AUTHENTICATED_TABLE_NAME + " (" +
                NOTIFICATIONS_AUTHENTICATED_COLUMN_ID + " INTEGER NOT NULL, " +
                NOTIFICATIONS_AUTHENTICATED_COLUMN_TIME + " INTEGER NOT NULL, " +
                "PRIMARY KEY (" + NOTIFICATIONS_AUTHENTICATED_COLUMN_ID + ")" +
                ");");
        db.execSQL("CREATE INDEX notifications_authenticated_time_idx ON " + NOTIFICATIONS_AUTHENTICATED_TABLE_NAME + "(" + NOTIFICATIONS_AUTHENTICATED_COLUMN_TIME + ");");

        db.execSQL("CREATE TABLE " + OFFLINE_CAMPAIGNS_TABLE_NAME + " (" +
                OFFLINE_CAMPAIGNS_COLUMN_SWRVE_USER_ID + " TEXT NOT NULL, " +
                OFFLINE_CAMPAIGNS_COLUMN_CAMPAIGN_ID + " TEXT NOT NULL," +
                OFFLINE_CAMPAIGNS_COLUMN_JSON + " TEXT NOT NULL," +
                "PRIMARY KEY (" + OFFLINE_CAMPAIGNS_COLUMN_SWRVE_USER_ID + "," + OFFLINE_CAMPAIGNS_COLUMN_CAMPAIGN_ID + ")" +
                ");");

        db.execSQL("CREATE TABLE " + ASSET_LOGS_TABLE_NAME + " (" +
                ASSET_LOGS_COLUMN_NAME + " TEXT NOT NULL, " +
                ASSET_LOGS_COLUMN_DOWNLOAD_COUNT + " INTEGER NOT NULL, " +
                ASSET_LOGS_COLUMN_LAST_DOWNLOAD_TIME + " INTEGER NOT NULL, " +
                "PRIMARY KEY (" + ASSET_LOGS_COLUMN_NAME + ")" +
                ");");
        db.execSQL("CREATE INDEX asset_logs_last_download_time_idx ON " + ASSET_LOGS_TABLE_NAME + "(" + ASSET_LOGS_COLUMN_LAST_DOWNLOAD_TIME + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String userId = SwrveSDK.getUserId();

        // **** PLEASE NOTE ********
        // do not add break to this switch statement so execution will start at oldVersion, and run straight through to the latest
        switch (oldVersion) {
            case 1:
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

            case 2:

                db.execSQL("CREATE TABLE users (swrve_user_id TEXT NOT NULL, " +
                        "external_user_id TEXT NOT NULL, " +
                        "verified BOOL NOT NULL, " +
                        "PRIMARY KEY (swrve_user_id, external_user_id)" +
                        ");");

                db.execSQL("CREATE TABLE notifications_authenticated (" +
                        "notification_id INTEGER NOT NULL, " +
                        "time INTEGER NOT NULL, " +
                        "PRIMARY KEY (notification_id)" +
                        ")");
                db.execSQL("CREATE INDEX notifications_authenticated_time_idx ON notifications_authenticated(time)");

                // Migrate campaigns_and_resources_etag from shared preferences to cache table
                SharedPreferences settings = context.getSharedPreferences("swrve_prefs", 0);
                String currentEtag = settings.getString("campaigns_and_resources_etag", "");
                if (SwrveHelper.isNotNullOrEmpty(userId)) {
                    ContentValues values = new ContentValues();
                    values.put("user_id", userId);
                    values.put("category", "swrve.etag");
                    values.put("raw_data", currentEtag);
                    db.insertOrThrow("cache", null, values);
                }
                SharedPreferences.Editor editor = settings.edit();
                editor.remove("campaigns_and_resources_etag");
                editor.apply();

                // Set userJoinedTime for current user. Remove SwrveSDK.installTime because appInstallTime is taken from the OS now.
                if (SwrveHelper.isNotNullOrEmpty(userId)) {
                    Cursor cursor = db.rawQuery("SELECT * FROM cache WHERE category='SwrveSDK.installTime'", null);
                    cursor.moveToFirst();
                    if (!cursor.isAfterLast()) {
                        int index = cursor.getColumnIndex("raw_data");
                        String installDate = cursor.getString(index);
                        if (SwrveHelper.isNotNullOrEmpty(installDate)) {
                            ContentValues values = new ContentValues();
                            values.put("user_id", userId);
                            values.put("category", "SwrveSDK.userJoinedTime");
                            values.put("raw_data", installDate);
                            db.insertOrThrow("cache", null, values);
                        }
                    }
                }
                db.execSQL("DELETE FROM cache WHERE user_id='' AND category='SwrveSDK.installTime'");

            case 3:

                db.execSQL("CREATE TABLE offline_campaigns (swrve_user_id TEXT NOT NULL, " +
                        "campaign_id TEXT NOT NULL, " +
                        "campaign_json_data TEXT NOT NULL, " +
                        "PRIMARY KEY (swrve_user_id, campaign_id)" +
                        ");");

            case 4:

                db.execSQL("CREATE TABLE asset_logs (" +
                        "name TEXT NOT NULL, " +
                        "download_count INTEGER NOT NULL, " +
                        "last_download_time INTEGER NOT NULL, " +
                        "PRIMARY KEY (name)" +
                        ")");
                db.execSQL("CREATE INDEX asset_logs_last_download_time_idx ON asset_logs(last_download_time)");
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
