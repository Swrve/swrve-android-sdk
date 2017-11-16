package com.swrve.sdk;

import com.swrve.sdk.localstorage.LocalStorage;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import java.util.List;

/**
 * Used internally to send events.
 */
interface SwrveEventsManager {

    int storeAndSendEvents(List<String> eventsJson, LocalStorage localStorage) throws Exception;

    int sendStoredEvents(SwrveMultiLayerLocalStorage cachedLocalStorage);
}