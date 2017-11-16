package com.swrve.sdk.localstorage;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * Used internally to define a common storage for events and other persistent data.
 */
public interface LocalStorage {

    String SIGNATURE_SUFFIX = "_SGT";

    long addEvent(String userId, String eventJSON) throws Exception;
    void removeEvents(String userId, Collection<Long> ids);
    LinkedHashMap<Long, String> getFirstNEvents(Integer n, String userId);

    SwrveCacheItem getCacheItem(String userId, String category);
    void setCacheEntry(String userId, String category, String rawData);

    String getSecureCacheEntryForUser(String userId, String category, String uniqueKey) throws SecurityException;
    void setSecureCacheEntryForUser(String userId, String category, String rawData, String signature);
}
