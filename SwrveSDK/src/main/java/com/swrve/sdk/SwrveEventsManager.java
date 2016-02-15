package com.swrve.sdk;

import com.swrve.sdk.localstorage.ILocalStorage;
import com.swrve.sdk.localstorage.MemoryCachedLocalStorage;
import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.rest.IRESTClient;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class SwrveEventsManager extends SwrveCommonEventsManager {
    protected SwrveEventsManager(IRESTClient restClient) {
        super(restClient);
    }
    /*
     * Stores the events passed in from ArrayList and attempts to send only these events. If successful, these events are removed from storage.
     */
    protected int storeAndSendEvents(ArrayList<String> eventsJson, MemoryCachedLocalStorage memoryCachedLocalStorage, SQLiteLocalStorage sqLiteLocalStorage) throws Exception {
        if (eventsJson == null || (eventsJson != null && eventsJson.size() == 0)) {
            return 0;
        }
        synchronized(lock) {
            LinkedHashMap<Long, String> storedEvents = storeEvents(eventsJson, sqLiteLocalStorage);
            LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>> combinedEvents = new LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>>();
            combinedEvents.put(memoryCachedLocalStorage, storedEvents);
            return sendEvents(combinedEvents);
        }
    }

    private LinkedHashMap<Long, String> storeEvents(ArrayList<String> eventsJson, SQLiteLocalStorage sqLiteLocalStorage) throws Exception {
        LinkedHashMap<Long, String> storedEvents = new LinkedHashMap<Long, String>();
        // Store named events coming from the list
        for (String eventAsJSON : eventsJson) {
            long id = sqLiteLocalStorage.addEventAndGetId(eventAsJSON);
            storedEvents.put(id, eventAsJSON);
        }

        return storedEvents;
    }

    /*
     * Attempts to sends events from local storage and deletes them if successful. Number of events sent configured from config.
     */
    protected int sendStoredEvents(MemoryCachedLocalStorage cachedLocalStorage) {
        synchronized(lock) {
            final LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>> combinedEvents = cachedLocalStorage.getCombinedFirstNEvents(SwrveCommon.getSwrveCommon().getMaxEventsPerFlush());
            return sendEvents(combinedEvents);
        }
    }
}
