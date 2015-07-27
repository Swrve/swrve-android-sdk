package com.swrve.sdk.localstorage;

import java.util.List;
import java.util.Map.Entry;

/**
 * Used internally to help with flushing from one storage object to another.
 */
public interface IFastInsertLocalStorage {
    void addMultipleEvent(List<String> eventsJSON);

    void setMultipleCacheEntries(List<Entry<String, Entry<String, String>>> cacheEntries);
}
