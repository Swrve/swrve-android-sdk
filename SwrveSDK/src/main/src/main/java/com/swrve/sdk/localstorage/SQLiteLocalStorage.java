package com.swrve.sdk.localstorage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.swrve.sdk.SwrveHelper;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used internally to provide a persistent storage of data on the device.
 */
public class SQLiteLocalStorage implements ILocalStorage, IFastInsertLocalStorage {

    // Database
    public static final int SWRVE_DB_VERSION = 1;

    // Events JSON table
    public static final String TABLE_EVENTS_JSON = "events";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_EVENT = "event";

    // Cache table
    public static final String TABLE_CACHE = "server_cache";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_RAW_DATA = "raw_data";

    private SQLiteDatabase database;
    private SwrveSQLiteOpenHelper dbHelper;
    private AtomicBoolean connectionOpen;

    public SQLiteLocalStorage(Context context, String dbName, long maxDbSize) {
        this.dbHelper = new SwrveSQLiteOpenHelper(context, dbName);
        this.database = dbHelper.getWritableDatabase();
        this.database.setMaximumSize(maxDbSize);
        this.connectionOpen = new AtomicBoolean(true);
    }

    public void addEvent(String eventJSON) throws SQLException {
        if (connectionOpen.get()) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_EVENT, eventJSON);
            database.insertOrThrow(TABLE_EVENTS_JSON, null, values);
        }
    }

    public void removeEventsById(Collection<Long> ids) {
        if (connectionOpen.get()) {
            List<String> values = new ArrayList<String>(ids.size());
            for (long id : ids) {
                values.add(Long.toString(id));
            }

            database.delete(TABLE_EVENTS_JSON, COLUMN_ID + " IN (" + TextUtils.join(",  ", values) + ")", null);
        }
    }

    public LinkedHashMap<Long, String> getFirstNEvents(Integer n) {
        LinkedHashMap<Long, String> events = new LinkedHashMap<Long, String>();

        if (connectionOpen.get()) {
            Cursor cursor = null;
            try {
                // Select all entries
                cursor = database.query(TABLE_EVENTS_JSON, new String[]{COLUMN_ID, COLUMN_EVENT}, null, null, null, null, COLUMN_ID, n == null ? null : Integer.toString(n));

                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    // Create event out of row data
                    events.put(cursor.getLong(0), cursor.getString(1));
                    cursor.moveToNext();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return events;
    }

    @Override
    public void setCacheEntryForUser(String userId, String category, String rawData) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_RAW_DATA, rawData);
        insertOrUpdate(TABLE_CACHE, values, COLUMN_USER_ID + "= ? AND " + COLUMN_CATEGORY + "= ?", new String[]{userId, category});
    }

    @Override
    public void setSecureCacheEntryForUser(String userId, String category, String rawData, String signature) {
        setCacheEntryForUser(userId, category, rawData);
        setCacheEntryForUser(userId, category + SIGNATURE_SUFFIX, signature);
    }

    private void insertOrUpdate(String table, ContentValues values, String whereClause, String[] whereArgs) {
        if (connectionOpen.get()) {
            int affectedRows = database.update(table, values, whereClause, whereArgs);
            if (affectedRows == 0) {
                database.insertOrThrow(table, null, values);
            }
        }
    }

    @Override
    public String getCacheEntryForUser(String userId, String category) {
        String resultJSON = null;

        if (connectionOpen.get()) {
            Cursor cursor = null;
            try {
                cursor = database.query(TABLE_CACHE, new String[]{COLUMN_RAW_DATA}, COLUMN_USER_ID + "= \"" + userId + "\" AND " + COLUMN_CATEGORY + "= \"" + category + "\"", null, null, null, null, "1");

                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    // Create event out of row data
                    resultJSON = cursor.getString(0);
                    cursor.moveToNext();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return resultJSON;
    }

    @Override
    public String getSecureCacheEntryForUser(String userId, String category, String uniqueKey) throws SecurityException {
        String cachedContent = getCacheEntryForUser(userId, category);
        String cachedSignature = getCacheEntryForUser(userId, category + SIGNATURE_SUFFIX);
        try {
            String computedSignature = SwrveHelper.createHMACWithMD5(cachedContent, uniqueKey);

            if (SwrveHelper.isNullOrEmpty(computedSignature) || SwrveHelper.isNullOrEmpty(cachedSignature) || !cachedSignature.equals(computedSignature)) {
                throw new SecurityException("Signature validation failed");
            }
        } catch (NoSuchAlgorithmException e) {
        } catch (InvalidKeyException e) {
        }

        return cachedContent;
    }

    public void reset() {
        // Clean database
        if (connectionOpen.get()) {
            database.delete(TABLE_EVENTS_JSON, null, null);
            database.delete(TABLE_CACHE, null, null);
        }
    }

    @Override
    public Map<Entry<String, String>, String> getAllCacheEntries() {
        Map<Entry<String, String>, String> allCacheEntries = new HashMap<Entry<String, String>, String>();

        if (connectionOpen.get()) {
            Cursor cursor = null;
            try {
                cursor = database.query(TABLE_CACHE, new String[]{COLUMN_USER_ID, COLUMN_CATEGORY, COLUMN_RAW_DATA}, null, null, null, null, null);

                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    // Create event out of row data
                    allCacheEntries.put(new SimpleEntry<String, String>(cursor.getString(0), cursor.getString(1)), cursor.getString(2));
                    cursor.moveToNext();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return allCacheEntries;
    }

    // Fast flush
    @Override
    public void addMultipleEvent(List<String> eventsJSON) throws SQLException {
        if (connectionOpen.get()) {
            String sql = "INSERT INTO " + TABLE_EVENTS_JSON + " (" + COLUMN_EVENT + ") VALUES (?)";
            database.beginTransaction();
            SQLiteStatement stmt = null;
            try {
                stmt = database.compileStatement(sql);
                Iterator<String> eventsIt = eventsJSON.iterator();
                while (eventsIt.hasNext()) {
                    stmt.bindString(1, eventsIt.next());
                    stmt.execute();
                    stmt.clearBindings();
                }
                database.setTransactionSuccessful(); // Commit
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
                database.endTransaction();
            }
        }
    }

    @Override
    public void setMultipleCacheEntries(List<Entry<String, Entry<String, String>>> cacheEntries) throws SQLException {
        if (connectionOpen.get()) {
            database.beginTransaction();
            try {
                Iterator<Entry<String, Entry<String, String>>> cacheIt = cacheEntries.iterator();
                ContentValues values = new ContentValues();
                while (cacheIt.hasNext()) {
                    Entry<String, Entry<String, String>> cacheEntry = cacheIt.next();
                    String userId = cacheEntry.getKey();
                    String category = cacheEntry.getValue().getKey();
                    values.put(COLUMN_USER_ID, userId);
                    values.put(COLUMN_CATEGORY, category);
                    values.put(COLUMN_RAW_DATA, cacheEntry.getValue().getValue());
                    insertOrUpdate(TABLE_CACHE, values, COLUMN_USER_ID + "= ? AND " + COLUMN_CATEGORY + "= ?", new String[]{userId, category});
                }
                database.setTransactionSuccessful(); // Commit
            } finally {
                database.endTransaction();
            }
        }
    }

    @Override
    public void close() {
        dbHelper.close();
        database.close();
        connectionOpen.set(false);
    }

    private static class SwrveSQLiteOpenHelper extends SQLiteOpenHelper {

        public SwrveSQLiteOpenHelper(Context context, String dbName) {
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