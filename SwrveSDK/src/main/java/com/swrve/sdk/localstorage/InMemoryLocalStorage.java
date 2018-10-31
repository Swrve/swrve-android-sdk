package com.swrve.sdk.localstorage;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveUser;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Used internally to provide a volatile storage of data that may be saved later on the device.
 */
public class InMemoryLocalStorage implements LocalStorage {

    private static final int MAX_ELEMENTS = 2000;
    protected Map<String, List<SwrveEventItem>> eventsPerUserId = new HashMap<>();
    protected Map<String, Map<String, SwrveCacheItem>> cachePerUserId = new HashMap<>();

    @Override
    public synchronized long addEvent(String userId, String eventJSON) {
        long storedEventId = 0;
        if (userId == null || eventJSON == null) {
            SwrveLogger.e("Cannot set null value for event in userId:%s eventJSON:%s", userId, eventJSON);
        } else if (getEventsSize() >= MAX_ELEMENTS) {
            SwrveLogger.w("The number of In Memory event items has reached its limit. Cannot store anymore until a new session.");
        } else {
            List<SwrveEventItem> events = eventsPerUserId.get(userId);
            if(events == null) {
                events = new ArrayList<>();
            }
            SwrveEventItem newEvent = new SwrveEventItem();
            newEvent.userId = userId;
            newEvent.event = eventJSON;
            events.add(newEvent);
            eventsPerUserId.put(userId, events);
            storedEventId = newEvent.id;
        }
        return storedEventId;
    }

    @Override
    public synchronized void removeEvents(String userId, Collection<Long> ids) {
        List<SwrveEventItem> events = eventsPerUserId.get(userId);
        Iterator<SwrveEventItem> iterator = events.iterator();
        while (iterator.hasNext()) {
            SwrveEventItem event = iterator.next();
            if (ids.contains(event.id)) {
                iterator.remove();
            }
        }
    }

    @Override
    public synchronized LinkedHashMap<Long, String> getFirstNEvents(Integer n, String userId) {
        LinkedHashMap<Long, String> topEvents = new LinkedHashMap<>();
        List<SwrveEventItem> events = eventsPerUserId.get(userId);
        if(events !=null && events.size() > 0) {
            int countLeft = n;
            Iterator<SwrveEventItem> iterator = events.iterator();
            while (iterator.hasNext() && countLeft > 0) {
                SwrveEventItem event = iterator.next();
                topEvents.put(event.id, event.event);
                countLeft--;
            }
        }
        return topEvents;
    }

    @Override
    public synchronized SwrveCacheItem getCacheItem(String userId, String category) {
        Map<String, SwrveCacheItem> cache = cachePerUserId.get(userId);
        if (cache == null) {
            cache = new HashMap<>();
        }
        return cache.get(category);
    }

    @Override
    public synchronized void setCacheEntry(String userId, String category, String rawData) {
        if (userId == null || category == null || rawData == null) {
            SwrveLogger.e("Cannot set null value in cache entry for userId:%s category:%s rawData:%s.", userId, category, rawData);
        } else if (getCacheSize() >= MAX_ELEMENTS) {
            SwrveLogger.w("The number of In Memory cache items has reached its limit. Cannot store anymore until a new session.");
        } else {
            Map<String, SwrveCacheItem> cachePerCategory = cachePerUserId.get(userId);
            if(cachePerCategory == null) {
                cachePerCategory = new HashMap<>();
            }
            SwrveCacheItem cacheItem = cachePerCategory.get(category);
            if (cacheItem == null) {
                cacheItem = new SwrveCacheItem(userId, category, rawData);
            } else {
                cacheItem.userId = userId;
                cacheItem.category = category;
                cacheItem.rawData = rawData;
            }
            cachePerCategory.put(category, cacheItem);
            cachePerUserId.put(userId, cachePerCategory);
        }
    }

    @Override
    public synchronized String getSecureCacheEntryForUser(String userId, String category, String uniqueKey) throws SecurityException {
        SwrveCacheItem cacheItem = getCacheItem(userId, category);
        SwrveCacheItem cacheItemSignature = getCacheItem(userId, category + SIGNATURE_SUFFIX);
        if (cacheItem != null && cacheItemSignature != null) {
            String cachedContent = cacheItem.rawData;
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
        return null;
    }

    @Override
    public synchronized void setSecureCacheEntryForUser(String userId, String category, String rawData, String signature) {
        setCacheEntry(userId, category, rawData);
        setCacheEntry(userId, category + SIGNATURE_SUFFIX, signature);
    }

    @Override
    public void saveUser(SwrveUser swrveUser) {
         // not used for in memory storage
    }

    @Override
    public void deleteUser(String swrveUserId) {
        // not used for in memory storage
    }

    @Override
    public SwrveUser getUserByExternalUserId(String externalUserId) {
        // not used for in memory storage
        return null;
    }

    @Override
    public SwrveUser getUserBySwrveUserId(String swrveUserId) {
        // not used for in memory storage
        return null;
    }

    private int getEventsSize() {
        int size = 0;
        for (Map.Entry<String, List<SwrveEventItem>> entry : eventsPerUserId.entrySet()) {
            size += entry.getValue().size();
        }
        return size;
    }

    private int getCacheSize() {
        int size = 0;
        for (Map.Entry<String, Map<String, SwrveCacheItem>> entry : cachePerUserId.entrySet()) {
            size += entry.getValue().size();
        }
        return size;
    }

    @Override
    public void truncateNotificationsAuthenticated(int rows) {
        // not implemented. Go directly to SQLiteLocalStorage
    }

    @Override
    public void saveNotificationAuthenticated(int notificationId, long time) {
        // not implemented. Go directly to SQLiteLocalStorage
    }

    @Override
    public List<Integer> getNotificationsAuthenticated() {
        // not implemented. Go directly to SQLiteLocalStorage
        return null;
    }

    @Override
    public void deleteNotificationsAuthenticated() {
        // not implemented. Go directly to SQLiteLocalStorage
    }
}
