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
public class MemoryEventLocalStorage implements IEventLocalStorage, IFlushableLocalStorage {

    private static final int MAX_ELEMENTS = 2000;
    private List<StoredEvent> events = new ArrayList<StoredEvent>();

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
    public void close() {
    }

    @Override
    public void reset() {
        events.clear();
    }

    private static class StoredEvent {
        public static long eventCount = 0;
        public long id;
        public String event;

        public StoredEvent() {
            this.id = (eventCount++);
        }
    }
}
