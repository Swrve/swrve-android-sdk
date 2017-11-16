package com.swrve.sdk.localstorage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.CACHE_COLUMN_CATEGORY;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.CACHE_COLUMN_RAW_DATA;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.CACHE_COLUMN_USER_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.CACHE_TABLE_NAME;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.EVENTS_COLUMN_EVENT;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.EVENTS_COLUMN_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.EVENTS_COLUMN_USER_ID;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.EVENTS_TABLE_NAME;
import static com.swrve.sdk.localstorage.SwrveSQLiteOpenHelper.SWRVE_DB_VERSION;

/**
 * Used internally to provide a persistent storage of data on the device.
 */
public class SQLiteLocalStorage implements LocalStorage {

    protected SQLiteDatabase database;

    public SQLiteLocalStorage(Context context, String dbName, long maxDbSize) {
        SwrveSQLiteOpenHelper sqLiteOpenHelper = SwrveSQLiteOpenHelper.getInstance(context, dbName, SWRVE_DB_VERSION);
        this.database = sqLiteOpenHelper.getWritableDatabase();
        this.database.setMaximumSize(maxDbSize);
    }

    @Override
    public long addEvent(String userId, String eventJSON) throws SQLException {
        long rowId = 0;
        if (database.isOpen()) {
            ContentValues values = new ContentValues();
            values.put(EVENTS_COLUMN_USER_ID, userId);
            values.put(EVENTS_COLUMN_EVENT, eventJSON);
            rowId = database.insertOrThrow(EVENTS_TABLE_NAME, null, values);
        }
        return rowId;
    }

    @Override
    public synchronized void removeEvents(String userId, Collection<Long> ids) {
        try {
            // userId not needed here.
            if (database.isOpen()) {
                List<String> values = new ArrayList<>(ids.size());
                for (long id : ids) {
                    values.add(Long.toString(id));
                }
                database.delete(EVENTS_TABLE_NAME, EVENTS_COLUMN_ID + " IN (" + TextUtils.join(",  ", values) + ")", null);
            }
        } catch (Exception e) {
            SwrveLogger.e("Exception deleting events for userId:" + userId + " id's:[" + ids + "]", e);
        }
    }

    @Override
    public LinkedHashMap<Long, String> getFirstNEvents(Integer n, String userId) {
        LinkedHashMap<Long, String> events = new LinkedHashMap<>();
        if (userId == null) {
            SwrveLogger.e("Cannot use null value userId for getFirstNEvents. userId:%s.", userId);
        } else if (database.isOpen()) {
            Cursor cursor = null;
            try {
                String table = EVENTS_TABLE_NAME;
                String[] columns = new String[]{EVENTS_COLUMN_ID, EVENTS_COLUMN_EVENT};
                String whereClause = EVENTS_COLUMN_USER_ID + " = ?";
                String[] whereArgs = {userId};
                String groupBy = null, having = null;
                String limit = (n == null ? null : Integer.toString(n));
                cursor = database.query(table, columns, whereClause, whereArgs, groupBy, having, EVENTS_COLUMN_ID, limit);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    events.put(cursor.getLong(0), cursor.getString(1));
                    cursor.moveToNext();
                }
            } catch (Exception ex) {
                SwrveLogger.e("Error getting " + n + " events for user:" + userId, ex);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return events;
    }

    @Override
    public void setCacheEntry(String userId, String category, String rawData) {
        if (userId == null || category == null || rawData == null) {
            SwrveLogger.e("Cannot set null value in cache entry for userId:%s category:%s rawData:%s.", userId, category, rawData);
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(CACHE_COLUMN_USER_ID, userId);
            values.put(CACHE_COLUMN_CATEGORY, category);
            values.put(CACHE_COLUMN_RAW_DATA, rawData);
            insertOrUpdate(CACHE_TABLE_NAME, values, CACHE_COLUMN_USER_ID + "= ? AND " + CACHE_COLUMN_CATEGORY + "= ?", new String[]{userId, category});
        } catch (Exception e) {
            SwrveLogger.e("Exception setting cache for userId:" + userId + " category:" + category + " rawData:" + rawData, e);
        }
    }

    @Override
    public void setSecureCacheEntryForUser(String userId, String category, String rawData, String signature) {
        setCacheEntry(userId, category, rawData);
        setCacheEntry(userId, category + SIGNATURE_SUFFIX, signature);
    }

    private void insertOrUpdate(String table, ContentValues values, String whereClause, String[] whereArgs) {
        if (database.isOpen()) {
            int affectedRows = database.update(table, values, whereClause, whereArgs);
            if (affectedRows == 0) {
                database.insertOrThrow(table, null, values);
            }
        }
    }

    @Override
    public SwrveCacheItem getCacheItem(String userId, String category) {
        SwrveCacheItem cacheItem = null;
        if (userId == null || category == null) {
            SwrveLogger.e("Cannot use null value in getCacheItem. userId:%s category:%s rawData:%s.", userId, category);
        } else if (database.isOpen()) {
            Cursor cursor = null;
            try {
                cursor = database.query(CACHE_TABLE_NAME, new String[]{CACHE_COLUMN_RAW_DATA}, CACHE_COLUMN_USER_ID + "= \"" + userId + "\" AND " + CACHE_COLUMN_CATEGORY + "= \"" + category + "\"", null, null, null, null, "1");

                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    String rawData = cursor.getString(0);
                    cursor.moveToNext();
                    cacheItem = new SwrveCacheItem(userId, category, rawData);
                }
            } catch (Exception e) {
                SwrveLogger.e("Exception occurred getting cache userId:" + userId + " category:" + category, e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return cacheItem;
    }

    @Override
    public String getSecureCacheEntryForUser(String userId, String category, String uniqueKey) throws SecurityException {
        SwrveCacheItem cacheItem = getCacheItem(userId, category);
        if (cacheItem == null) {
            return null;
        }
        String cachedContent = cacheItem.rawData;
        if(cachedContent == null) {
            return null;
        }
        SwrveCacheItem cacheItemSignature = getCacheItem(userId, category + SIGNATURE_SUFFIX);
        if (cacheItemSignature == null) {
            return null;
        }
        String cachedSignature = cacheItemSignature.rawData;
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

    public void saveMultipleEventItems(List<SwrveEventItem> eventList) throws SQLException {
        if (database.isOpen()) {
            String sql = "INSERT INTO " + EVENTS_TABLE_NAME + " (" + EVENTS_COLUMN_EVENT + ", " + EVENTS_COLUMN_USER_ID + ") VALUES (?, ?)";
            database.beginTransaction();
            SQLiteStatement statement = null;
            try {
                statement = database.compileStatement(sql);
                Iterator<SwrveEventItem> iterator = eventList.iterator();
                while (iterator.hasNext()) {
                    SwrveEventItem eventItem = iterator.next();
                    statement.bindString(1, eventItem.event);
                    statement.bindString(2, eventItem.userId);
                    statement.execute();
                    statement.clearBindings();
                }
                database.setTransactionSuccessful(); // Commit
            } finally {
                if (statement != null) {
                    statement.close();
                }
                database.endTransaction();
            }
        }
    }

    public void saveMultipleCacheItems(Map<String, SwrveCacheItem> cacheItemMap) throws SQLException {
        if (database.isOpen()) {
            database.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                for (Map.Entry<String, SwrveCacheItem> itemEntry : cacheItemMap.entrySet()) {
                    String userId = itemEntry.getValue().userId;
                    String category = itemEntry.getValue().category;
                    String rawData = itemEntry.getValue().rawData;
                    values.put(CACHE_COLUMN_USER_ID, userId);
                    values.put(CACHE_COLUMN_CATEGORY, category);
                    values.put(CACHE_COLUMN_RAW_DATA, rawData);
                    insertOrUpdate(CACHE_TABLE_NAME, values, CACHE_COLUMN_USER_ID + "= ? AND " + CACHE_COLUMN_CATEGORY + "= ?", new String[]{userId, category});
                }
                database.setTransactionSuccessful(); // Commit
            } finally {
                database.endTransaction();
            }
        }
    }
}