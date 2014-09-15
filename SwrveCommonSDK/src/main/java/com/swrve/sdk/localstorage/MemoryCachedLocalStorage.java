package com.swrve.sdk.localstorage;

import android.util.Log;

import com.swrve.sdk.SwrveHelper;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Used internally to provide a multi-layer cache of events and resource diffs.
 */
public class MemoryCachedLocalStorage implements ILocalStorage {
    private ILocalStorage cache;
    private ILocalStorage secondaryStorage;

    private Object eventLock = new Object();
    private Object cacheLock = new Object();

    public MemoryCachedLocalStorage(ILocalStorage cache, ILocalStorage secondaryStorage) {
        this.cache = cache;
        this.secondaryStorage = secondaryStorage;
    }

    public ILocalStorage getSecondaryStorage() {
        return secondaryStorage;
    }

    public void setSecondaryStorage(ILocalStorage secondaryStorage) {
        this.secondaryStorage = secondaryStorage;
    }

    public ILocalStorage getCacheStorage() {
        return cache;
    }

    public void setCacheStorage(ILocalStorage cacheStorage) {
        this.cache = cacheStorage;
    }

    @Override
    public String getCacheEntryForUser(String userId, String category) {
        synchronized (cacheLock) {
            String result = cache.getCacheEntryForUser(userId, category);
            if (result == null && secondaryStorage != null) {
                result = secondaryStorage.getCacheEntryForUser(userId, category);
            }
            return result;
        }
    }

    @Override
    public String getSecureCacheEntryForUser(String userId, String category, String uniqueKey) throws SecurityException {
        String cachedContent = null;
        String cachedSignature = null;

        synchronized (cacheLock) {
            cachedContent = cache.getCacheEntryForUser(userId, category);
            cachedSignature = cache.getCacheEntryForUser(userId, category + SIGNATURE_SUFFIX);
            if (SwrveHelper.isNullOrEmpty(cachedContent) && secondaryStorage != null) {
                // Was not cached in memory, read from disk
                cachedContent = secondaryStorage.getCacheEntryForUser(userId, category);
                cachedSignature = secondaryStorage.getCacheEntryForUser(userId, category + SIGNATURE_SUFFIX);
            }
        }
        if (!SwrveHelper.isNullOrEmpty(cachedContent)) {
            try {
                String computedSignature = SwrveHelper.createHMACWithMD5(cachedContent, uniqueKey);

                if (SwrveHelper.isNullOrEmpty(computedSignature) || SwrveHelper.isNullOrEmpty(cachedSignature) || !cachedSignature.equals(computedSignature)) {
                    throw new SecurityException("Signature validation failed");
                }
            } catch (NoSuchAlgorithmException e) {
                Log.i("SwrveSDK", "Computing signature failed because of invalid algorithm");
            } catch (InvalidKeyException e) {
                Log.i("SwrveSDK", "Computing signature failed because of an invalid key");
            }
        }

        return cachedContent;
    }

    public LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>> getCombinedFirstNEvents(Integer n) {
        synchronized (eventLock) {
            LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>> result = new LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>>();
            int eventCount = 0;
            if (secondaryStorage != null) {
                LinkedHashMap<Long, String> events = secondaryStorage.getFirstNEvents(n);
                eventCount = events.size();
                if (eventCount > 0) {
                    result.put(secondaryStorage, events);
                }
            }

            if (n - eventCount > 0) {
                LinkedHashMap<Long, String> events = cache.getFirstNEvents(n - eventCount);
                int remainingEventCount = events.size();
                if (remainingEventCount > 0) {
                    result.put(cache, events);
                }
            }

            return result;
        }
    }

    @Override
    public void addEvent(String eventJSON) throws Exception {
        synchronized (eventLock) {
            cache.addEvent(eventJSON);
        }
    }

    @Override
    public void removeEventsById(Collection<Long> ids) {
        synchronized (eventLock) {
            cache.removeEventsById(ids);
        }
    }

    @Override
    public LinkedHashMap<Long, String> getFirstNEvents(Integer ids) {
        synchronized (eventLock) {
            return cache.getFirstNEvents(ids);
        }
    }

    @Override
    public void setCacheEntryForUser(String userId, String category, String rawData) {
        synchronized (cacheLock) {
            cache.setCacheEntryForUser(userId, category, rawData);
        }
    }

    @Override
    public void setSecureCacheEntryForUser(String userId, String category, String rawData, String signature) {
        synchronized (cacheLock) {
            cache.setCacheEntryForUser(userId, category, rawData);
            cache.setCacheEntryForUser(userId, category + SIGNATURE_SUFFIX, signature);
        }
    }

    public void setAndFlushSecureSharedEntryForUser(String userId, String category, String rawData, String uniqueKey) {
        synchronized (cacheLock) {
            try {
                String signature = SwrveHelper.createHMACWithMD5(rawData, uniqueKey);

                // Save to memory and secondary storage
                cache.setSecureCacheEntryForUser(userId, category, rawData, signature);
                if (secondaryStorage != null) {
                    secondaryStorage.setSecureCacheEntryForUser(userId, category, rawData, signature);
                }
            } catch (NoSuchAlgorithmException e) {
                Log.i("SwrveSDK", "Computing signature failed because of invalid algorithm");
                cache.setCacheEntryForUser(userId, category, rawData);
                if (secondaryStorage != null) {
                    secondaryStorage.setCacheEntryForUser(userId, category, rawData);
                }
            } catch (InvalidKeyException e) {
                Log.i("SwrveSDK", "Computing signature failed because of an invalid key");
            }
        }
    }

    public void setAndFlushSharedEntry(String category, String rawData) {
        synchronized (cacheLock) {
            // Save to memory and secondary storage
            cache.setCacheEntryForUser(category, category, rawData);
            if (secondaryStorage != null) {
                secondaryStorage.setCacheEntryForUser(category, category, rawData);
            }
        }
    }

    public String getSharedCacheEntry(String category) {
        return getCacheEntryForUser(category, category);
    }

    public void flush() throws Exception {
        if (cache != secondaryStorage && cache instanceof IFlushableLocalStorage && secondaryStorage instanceof IFastInsertLocalStorage) {
            IFlushableLocalStorage flushableStorage = ((IFlushableLocalStorage) cache);
            IFastInsertLocalStorage targetStorage = ((IFastInsertLocalStorage) secondaryStorage);
            synchronized (eventLock) {
                flushableStorage.flushEvents(targetStorage);
            }
            synchronized (cacheLock) {
                flushableStorage.flushCache(targetStorage);
            }
        }
    }


    @Override
    public Map<Entry<String, String>, String> getAllCacheEntries() {
        return cache.getAllCacheEntries();
    }

    public Map<Entry<String, String>, String> getCombinedCacheEntries() {
        Map<Entry<String, String>, String> result = cache.getAllCacheEntries();
        if (secondaryStorage != null) {
            result.putAll(secondaryStorage.getAllCacheEntries());
        }
        return result;
    }

    @Override
    public void close() {
        cache.close();
        if (secondaryStorage != null) {
            secondaryStorage.close();
        }
    }

    @Override
    public void reset() {
        cache.reset();
        if (secondaryStorage != null) {
            secondaryStorage.reset();
        }
    }
}
