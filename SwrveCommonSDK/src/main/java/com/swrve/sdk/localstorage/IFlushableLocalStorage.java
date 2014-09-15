package com.swrve.sdk.localstorage;

/**
 * Used internally to define a storage object that is capable of flushing.
 */
public interface IFlushableLocalStorage {
    public void flushEvents(IFastInsertLocalStorage externalStorage);

    public void flushCache(IFastInsertLocalStorage externalStorage);
}
