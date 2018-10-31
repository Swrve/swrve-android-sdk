package com.swrve.sdk;

import android.content.Context;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.localstorage.InMemoryLocalStorage;
import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import java.util.Random;
import java.util.UUID;

import static com.swrve.sdk.ISwrveCommon.CACHE_DEVICE_ID;
import static com.swrve.sdk.ISwrveCommon.CACHE_SEQNUM;

class SwrveLocalStorageUtil {

    static synchronized String getDeviceId(SwrveMultiLayerLocalStorage multiLayerLocalStorage) {
        String id = multiLayerLocalStorage.getCacheEntry("", CACHE_DEVICE_ID); // device_id is with empty userId, its the same for all users
        if (id == null || id.length() <= 0) {
            String deviceId = UUID.randomUUID().toString();
            multiLayerLocalStorage.setCacheEntry("", CACHE_DEVICE_ID, deviceId); // device_id is saved with empty userId, its the same for all users
            return deviceId;
        } else {
            return id;
        }
    }

    static synchronized int getNextSequenceNumber(Context context, SwrveConfigBase config, SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId) {
        if (multiLayerLocalStorage == null || multiLayerLocalStorage.getSecondaryStorage() == null) {
            multiLayerLocalStorage = new SwrveMultiLayerLocalStorage(new InMemoryLocalStorage());
            SQLiteLocalStorage sqLiteLocalStorage = new SQLiteLocalStorage(context, config.getDbName(), config.getMaxSqliteDbSize());
            multiLayerLocalStorage.setSecondaryStorage(sqLiteLocalStorage);
        }
        return getNextSequenceNumber(multiLayerLocalStorage, userId);
    }

    private static int getNextSequenceNumber(SwrveMultiLayerLocalStorage storage, String userId) {
        String id = storage.getCacheEntry(userId, CACHE_SEQNUM);
        int seqnum = 1;
        if (!SwrveHelper.isNullOrEmpty(id)) {
            seqnum = Integer.parseInt(id) + 1;
        }
        storage.setCacheEntry(userId, CACHE_SEQNUM, Integer.toString(seqnum));
        return seqnum;
    }
}
