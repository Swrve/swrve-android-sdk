package com.swrve.sdk;

import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import java.util.UUID;

import static com.swrve.sdk.ISwrveCommon.CACHE_DEVICE_ID;
import static com.swrve.sdk.ISwrveCommon.CACHE_DEVICE_PROP_KEY;

class SwrveLocalStorageUtil {

    static synchronized String getDeviceId(SwrveMultiLayerLocalStorage multiLayerLocalStorage) {
        String id = multiLayerLocalStorage.getCacheEntry(CACHE_DEVICE_PROP_KEY, CACHE_DEVICE_ID); // device_id is with empty userId, its the same for all users
        if (id == null || id.length() <= 0) {
            String deviceId = UUID.randomUUID().toString();
            multiLayerLocalStorage.setCacheEntry(CACHE_DEVICE_PROP_KEY, CACHE_DEVICE_ID, deviceId); // device_id is saved with empty userId, its the same for all users
            return deviceId;
        } else {
            return id;
        }
    }
}
