package com.swrve.sdk.localstorage;

import android.util.Log;

import com.swrve.sdk.SwrveHelper;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Used internally to provide a multi-layer cache of events and resource diffs.
 */
public class MemoryCachedEventLocalStorage implements IEventLocalStorage {
    private IEventLocalStorage cache;
    private IEventLocalStorage secondaryStorage;

    private Object eventLock = new Object();

    public MemoryCachedEventLocalStorage(IEventLocalStorage cache, IEventLocalStorage secondaryStorage) {
        this.cache = cache;
        this.secondaryStorage = secondaryStorage;
    }

    public IEventLocalStorage getSecondaryStorage() {
        return secondaryStorage;
    }

    public void setSecondaryStorage(IEventLocalStorage secondaryStorage) {
        this.secondaryStorage = secondaryStorage;
    }

    public IEventLocalStorage getCacheStorage() {
        return cache;
    }

    public LinkedHashMap<IEventLocalStorage, LinkedHashMap<Long, String>> getCombinedFirstNEvents(Integer n) {
        synchronized (eventLock) {
            LinkedHashMap<IEventLocalStorage, LinkedHashMap<Long, String>> result = new LinkedHashMap<IEventLocalStorage, LinkedHashMap<Long, String>>();
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

    public void flush() throws Exception {
        if (cache != secondaryStorage && cache instanceof IFlushableLocalStorage && secondaryStorage instanceof IFastInsertLocalStorage) {
            IFlushableLocalStorage flushableStorage = ((IFlushableLocalStorage) cache);
            IFastInsertLocalStorage targetStorage = ((IFastInsertLocalStorage) secondaryStorage);
            synchronized (eventLock) {
                flushableStorage.flushEvents(targetStorage);
            }
        }
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
