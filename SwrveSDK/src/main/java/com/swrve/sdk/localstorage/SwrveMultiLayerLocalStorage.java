package com.swrve.sdk.localstorage;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveUser;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.swrve.sdk.localstorage.LocalStorage.SIGNATURE_SUFFIX;

/**
 * Used internally to provide a multi-layer primary and secondary LocalStorage
 */
public class SwrveMultiLayerLocalStorage {

    protected int NOTIFICATIONS_AUTHENICATED_MAX_ROWS = 100;

    private LocalStorage primaryStorage; // in-memory temporary and fast storage
    private LocalStorage secondaryStorage; // sql-ite database

    public static final Object EVENT_LOCK = new Object();
    private Object cacheLock = new Object();

    public SwrveMultiLayerLocalStorage(LocalStorage primaryStorage) {
        this.primaryStorage = primaryStorage;
    }

    public LocalStorage getPrimaryStorage() {
        return primaryStorage;
    }

    public LocalStorage getSecondaryStorage() {
        return secondaryStorage;
    }

    public void setSecondaryStorage(LocalStorage secondaryStorage) {
        this.secondaryStorage = secondaryStorage;
    }

    public String getCacheEntry(String userId, String category) {
        synchronized (cacheLock) {
            String rawData = null;
            SwrveCacheItem cacheItemPrimary = primaryStorage.getCacheItem(userId, category);
            if (cacheItemPrimary != null) {
                rawData = cacheItemPrimary.rawData;
            }
            if (rawData == null && secondaryStorage != null) {
                SwrveCacheItem cacheItemSecondary = secondaryStorage.getCacheItem(userId, category);
                if (cacheItemSecondary != null) {
                    rawData = cacheItemSecondary.rawData;
                    primaryStorage.setCacheEntry(userId, category, rawData); // update primary for speedy lookup next time
                }
            }
            return rawData;
        }
    }

    public String getSecureCacheEntryForUser(String userId, String category, String uniqueKey) throws SecurityException {
        String cachedContent = "";
        String cachedSignature = "";

        synchronized (cacheLock) {
            SwrveCacheItem cacheItemPrimary = primaryStorage.getCacheItem(userId, category);
            SwrveCacheItem cacheItemSignaturePrimary = primaryStorage.getCacheItem(userId, category + SIGNATURE_SUFFIX);
            if (cacheItemPrimary != null && cacheItemSignaturePrimary != null) {
                cachedContent = cacheItemPrimary.rawData;
                cachedSignature = cacheItemSignaturePrimary.rawData;
            }
            if (SwrveHelper.isNullOrEmpty(cachedContent) && secondaryStorage != null) {
                SwrveCacheItem cacheItemSecondary = secondaryStorage.getCacheItem(userId, category);
                SwrveCacheItem cacheItemSignatureSecondary = secondaryStorage.getCacheItem(userId, category + SIGNATURE_SUFFIX);
                if (cacheItemSecondary != null && cacheItemSignatureSecondary != null) {
                    cachedContent = cacheItemSecondary.rawData;
                    cachedSignature = cacheItemSignatureSecondary.rawData;
                }
            }
        }
        if (!SwrveHelper.isNullOrEmpty(cachedContent)) {
            try {
                String computedSignature = SwrveHelper.createHMACWithMD5(cachedContent, uniqueKey);

                if (SwrveHelper.isNullOrEmpty(computedSignature) || SwrveHelper.isNullOrEmpty(cachedSignature) || !cachedSignature.equals(computedSignature)) {
                    throw new SecurityException("Signature validation failed");
                } else {
                    return cachedContent;
                }
            } catch (NoSuchAlgorithmException e) {
                SwrveLogger.i("Computing signature failed because of invalid algorithm");
            } catch (InvalidKeyException e) {
                SwrveLogger.i("Computing signature failed because of an invalid key");
            }
        }
        return null;
    }

    // Note: This method should not be called from the UI thread
    public boolean hasQueuedEvents(String userId) {
        int numberOfEvents = 1; // only need to query for one event
        final LinkedHashMap<LocalStorage, LinkedHashMap<Long, String>> combinedEvents = getCombinedFirstNEvents(numberOfEvents, userId);
        return !combinedEvents.isEmpty();
    }

    public LinkedHashMap<LocalStorage, LinkedHashMap<Long, String>> getCombinedFirstNEvents(Integer n, String userId) {
        synchronized (EVENT_LOCK) {
            LinkedHashMap<LocalStorage, LinkedHashMap<Long, String>> result = new LinkedHashMap<>();
            int eventCount = 0;
            if (secondaryStorage != null) {
                LinkedHashMap<Long, String> events = secondaryStorage.getFirstNEvents(n, userId);
                eventCount = events.size();
                if (eventCount > 0) {
                    result.put(secondaryStorage, events);
                }
            }

            if (n - eventCount > 0) {
                LinkedHashMap<Long, String> events = primaryStorage.getFirstNEvents(n - eventCount, userId);
                int remainingEventCount = events.size();
                if (remainingEventCount > 0) {
                    result.put(primaryStorage, events);
                }
            }

            return result;
        }
    }

    public long addEvent(String userId, String eventJSON) throws Exception {
        synchronized (EVENT_LOCK) {
            return primaryStorage.addEvent(userId, eventJSON);
        }
    }

    public void setAndFlushSecureSharedEntryForUser(String userId, String category, String rawData, String uniqueKey) {
        synchronized (cacheLock) {
            try {
                String signature = SwrveHelper.createHMACWithMD5(rawData, uniqueKey);

                // Save to memory and secondary storage
                primaryStorage.setSecureCacheEntryForUser(userId, category, rawData, signature);
                if (secondaryStorage != null) {
                    secondaryStorage.setSecureCacheEntryForUser(userId, category, rawData, signature);
                }
            } catch (NoSuchAlgorithmException e) {
                SwrveLogger.i("Computing signature failed because of invalid algorithm");
                primaryStorage.setCacheEntry(userId, category, rawData);
                if (secondaryStorage != null) {
                    secondaryStorage.setCacheEntry(userId, category, rawData);
                }
            } catch (InvalidKeyException e) {
                SwrveLogger.i("Computing signature failed because of an invalid key");
            }
        }
    }

    // Save to primary memory and flush to secondary storage
    public void setCacheEntry(String userId, String category, String rawData) {
        synchronized (cacheLock) {
            primaryStorage.setCacheEntry(userId, category, rawData);
            if (secondaryStorage != null) {
                secondaryStorage.setCacheEntry(userId, category, rawData);
            }
        }
    }

    public void flush() {
        if (primaryStorage != secondaryStorage &&
                primaryStorage instanceof InMemoryLocalStorage &&
                secondaryStorage instanceof SQLiteLocalStorage) {

            InMemoryLocalStorage temporaryStorage = (InMemoryLocalStorage) primaryStorage;
            SQLiteLocalStorage permanentStorage = (SQLiteLocalStorage) secondaryStorage;

            synchronized (EVENT_LOCK) {
                flushEvents(temporaryStorage.eventsPerUserId, permanentStorage);
            }

            synchronized (cacheLock) {
                flushCache(temporaryStorage.cachePerUserId, permanentStorage);
            }
        }
    }

    private synchronized void flushEvents(Map<String, List<SwrveEventItem>> eventsPerUserId, SQLiteLocalStorage permanentStorage) {
        for (Map.Entry<String, List<SwrveEventItem>> entry : eventsPerUserId.entrySet()) {
            permanentStorage.saveMultipleEventItems(entry.getValue());
        }
        eventsPerUserId.clear();
    }

    private synchronized void flushCache(Map<String, Map<String, SwrveCacheItem>> cachePerUserId, SQLiteLocalStorage permanentStorage) {
        for (Map.Entry<String, Map<String, SwrveCacheItem>> entry : cachePerUserId.entrySet()) {
            permanentStorage.saveMultipleCacheItems(entry.getValue());
        }
        cachePerUserId.clear();
    }

    public SwrveUser getUserBySwrveUserId(String swrveUserId) {
        SwrveUser swrveUser = null;
        if (secondaryStorage != null) {
            swrveUser = secondaryStorage.getUserBySwrveUserId(swrveUserId);
        }
        return swrveUser;
    }

    public SwrveUser getUserByExternalUserId(String externalUserId) {
        SwrveUser swrveUser = null;
        if (secondaryStorage != null) {
            swrveUser = secondaryStorage.getUserByExternalUserId(externalUserId);
        }
        return swrveUser;
    }

    public void saveUser(SwrveUser swrveUser) {
        if (secondaryStorage != null) {
            secondaryStorage.saveUser(swrveUser);
        }
    }

    public void deleteUser(String swrveUserId) {
        if (secondaryStorage != null) {
            secondaryStorage.deleteUser(swrveUserId);
        }
    }

    public void saveNotificationAuthenticated(int notificationId) {
        if (secondaryStorage != null) {
            secondaryStorage.saveNotificationAuthenticated(notificationId, System.currentTimeMillis());
            // After adding an entry truncate the table to remove the oldest.
            secondaryStorage.truncateNotificationsAuthenticated(NOTIFICATIONS_AUTHENICATED_MAX_ROWS);
        }
    }

    public List<Integer> getNotificationsAuthenticated() {
        List<Integer> notifications = new ArrayList<>();
        if (secondaryStorage != null) {
            notifications = secondaryStorage.getNotificationsAuthenticated();
        }
        return notifications;
    }

    public void deleteNotificationsAuthenticated() {
        if (secondaryStorage != null) {
            secondaryStorage.deleteNotificationsAuthenticated();
        }
    }

    public void saveOfflineCampaign(String userId, String campaignId, String campaignData) {
        if (secondaryStorage != null) {
            secondaryStorage.saveOfflineCampaign(userId, campaignId, campaignData);
        }
    }

    public String getOfflineCampaign(String userId, String campaignId) {
        String campaignData = null;
        if (secondaryStorage != null) {
            campaignData = secondaryStorage.getOfflineCampaign(userId, campaignId);
        }
        return campaignData;
    }
}
