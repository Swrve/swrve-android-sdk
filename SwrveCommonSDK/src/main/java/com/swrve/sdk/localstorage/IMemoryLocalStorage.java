package com.swrve.sdk.localstorage;

/**
 * Used internally to define a storage object that sits in memory.
 */
public interface IMemoryLocalStorage extends ILocalStorage {
    String getSharedCacheEntry(String category);
    void setAndFlushSharedEntry(String category, String rawData);
}
