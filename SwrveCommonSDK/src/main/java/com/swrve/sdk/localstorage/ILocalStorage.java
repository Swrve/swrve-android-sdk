package com.swrve.sdk.localstorage;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Used internally to define a common storage for events and other persistent data.
 */
public interface ILocalStorage {
    static final String SIGNATURE_SUFFIX = "_SGT";

    // Event storage methods
    void addEvent(String eventJSON) throws Exception;

    void removeEventsById(Collection<Long> ids);

    LinkedHashMap<Long, String> getFirstNEvents(Integer n);

    // Resource storage methods
    String getCacheEntryForUser(String userId, String category);

    String getSecureCacheEntryForUser(String userId, String category, String uniqueKey) throws SecurityException;

    void setCacheEntryForUser(String userId, String category, String rawData);

    void setSecureCacheEntryForUser(String userId, String category, String rawData, String signature);

    Map<Entry<String, String>, String> getAllCacheEntries();

    void close();

    void reset();
}
