package com.swrve.sdk.localstorage;

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

/**
 * Used internally to provide a volatile storage of data that may be saved later on the device.
 */
public class MemoryLocalStorage implements ILocalStorage, IFlushableLocalStorage {

    private static final int MAX_ELEMENTS = 2000;
    private List<StoredEvent> events = new ArrayList<StoredEvent>();
    private Map<String, StoredCacheEntry> serverCache = new HashMap<String, StoredCacheEntry>();

    @Override
    public synchronized void addEvent(String eventJSON) throws Exception {
        if (events.size() < MAX_ELEMENTS) {
            StoredEvent newEvent = new StoredEvent();
            newEvent.event = eventJSON;
            events.add(newEvent);
        }
    }

    @Override
    public synchronized void removeEventsById(Collection<Long> ids) {
        Iterator<StoredEvent> iter = events.iterator();
        while (iter.hasNext()) {
            StoredEvent event = iter.next();
            if (ids.contains(event.id))
                iter.remove();
        }
    }

    @Override
    public synchronized LinkedHashMap<Long, String> getFirstNEvents(Integer n) {
        LinkedHashMap<Long, String> topEvents = new LinkedHashMap<Long, String>();
        int countLeft = n;
        Iterator<StoredEvent> iter = events.iterator();
        while (iter.hasNext() && countLeft > 0) {
            StoredEvent event = iter.next();
            topEvents.put(event.id, event.event);
            countLeft--;
        }
        return topEvents;
    }

    @Override
    public synchronized String getCacheEntryForUser(String userId, String category) {
        String uniqueId = userId + "##" + category;
        StoredCacheEntry foundEntry = serverCache.get(uniqueId);

        if (foundEntry != null)
            return foundEntry.rawData;
        else
            return null;
    }

    @Override
    public synchronized String getSecureCacheEntryForUser(String userId, String category, String uniqueKey) throws SecurityException {
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

    @Override
    public synchronized void setCacheEntryForUser(String userId, String category, String rawData) {
        String uniqueId = userId + "##" + category;
        StoredCacheEntry savedEntry = serverCache.get(uniqueId);

        if (savedEntry == null) {
            if (serverCache.size() < MAX_ELEMENTS) {
                savedEntry = new StoredCacheEntry();
                serverCache.put(uniqueId, savedEntry);
            }
        }

        if (savedEntry != null) {
            savedEntry.userId = userId;
            savedEntry.category = category;
            savedEntry.rawData = rawData;
        }
    }

    @Override
    public synchronized void setSecureCacheEntryForUser(String userId, String category, String rawData, String signature) {
        setCacheEntryForUser(userId, category, rawData);
        setCacheEntryForUser(userId, category + SIGNATURE_SUFFIX, signature);
    }

    @Override
    public synchronized Map<Entry<String, String>, String> getAllCacheEntries() {
        Map<Entry<String, String>, String> allCacheEntries = new HashMap<Entry<String, String>, String>();
        Iterator<String> itCache = serverCache.keySet().iterator();
        while (itCache.hasNext()) {
            String key = itCache.next();
            StoredCacheEntry entry = serverCache.get(key);
            allCacheEntries.put(new SimpleEntry<String, String>(entry.userId, entry.category), entry.rawData);
        }

        return allCacheEntries;
    }

    // Flush
    @Override
    public synchronized void flushEvents(IFastInsertLocalStorage externalStorage) {
        // Exchange events
        Iterator<StoredEvent> eventIter = events.iterator();
        List<String> eventsToFlush = new ArrayList<String>();
        while (eventIter.hasNext()) {
            StoredEvent event = eventIter.next();
            eventsToFlush.add(event.event);
        }
        externalStorage.addMultipleEvent(eventsToFlush);
        events.clear();
    }

    @Override
    public synchronized void flushCache(IFastInsertLocalStorage externalStorage) {
        // Exchange cache
        Iterator<String> cacheIter = serverCache.keySet().iterator();
        List<Entry<String, Entry<String, String>>> cacheEntries = new ArrayList<Entry<String, Entry<String, String>>>();
        while (cacheIter.hasNext()) {
            StoredCacheEntry cacheEntry = serverCache.get(cacheIter.next());
            cacheEntries.add(new SimpleEntry<String, Entry<String, String>>(cacheEntry.userId, new SimpleEntry<String, String>(cacheEntry.category, cacheEntry.rawData)));
        }
        externalStorage.setMultipleCacheEntries(cacheEntries);
        serverCache.clear();
    }

    @Override
    public void close() {
    }

    @Override
    public void reset() {
        events.clear();
        serverCache.clear();
    }

    private static class StoredEvent {
        public static long eventCount = 0;
        public long id;
        public String event;

        public StoredEvent() {
            this.id = (eventCount++);
        }
    }

    private static class StoredCacheEntry {
        public String userId;
        public String category;
        public String rawData;
    }
}
