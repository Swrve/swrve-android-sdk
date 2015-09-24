package com.swrve.sdk.localstorage;

/**
 * Used internally to define a storage object that is capable of flushing.
 */
public interface IFlushableLocalStorage {

    void flushEvents(IFastInsertLocalStorage externalStorage);

    void flushCache(IFastInsertLocalStorage externalStorage);
}
