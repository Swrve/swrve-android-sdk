package com.swrve.sdk;

import com.swrve.sdk.localstorage.MemoryCachedLocalStorage;
import com.swrve.sdk.localstorage.SQLiteLocalStorage;

import java.util.ArrayList;

/**
 * Used internally to send events.
 */
interface SwrveEventsManager {

    int storeAndSendEvents(ArrayList<String> eventsJson, MemoryCachedLocalStorage memoryCachedLocalStorage, SQLiteLocalStorage sqLiteLocalStorage) throws Exception;

    int sendStoredEvents(MemoryCachedLocalStorage cachedLocalStorage);
}