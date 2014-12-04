package com.swrve.sdk.localstorage;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Used internally to define a common storage for events.
 */
public interface IEventLocalStorage {

    void addEvent(String eventJSON) throws Exception;

    void removeEventsById(Collection<Long> ids);

    LinkedHashMap<Long, String> getFirstNEvents(Integer n);

    void close();

    void reset();
}
