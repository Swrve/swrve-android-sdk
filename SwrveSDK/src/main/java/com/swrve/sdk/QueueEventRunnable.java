package com.swrve.sdk;

import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import java.util.Map;

class QueueEventRunnable implements Runnable {

    private SwrveMultiLayerLocalStorage multiLayerLocalStorage;
    private String userId;
    private String eventType;
    private Map<String, Object> parameters;
    private Map<String, String> payload;
    private int seqNum;

    public QueueEventRunnable(SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId, String eventType, Map<String, Object> parameters, Map<String, String> payload, int seqNum) {
        this.multiLayerLocalStorage = multiLayerLocalStorage;
        this.userId = userId;
        this.eventType = eventType;
        this.parameters = parameters;
        this.payload = payload;
        this.seqNum = seqNum;
    }

    @Override
    public void run() {
        String eventString = "";
        try {
            eventString = EventHelper.eventAsJSON(eventType, parameters, payload, seqNum, System.currentTimeMillis());
            parameters = null;
            payload = null;
            multiLayerLocalStorage.addEvent(userId, eventString);
            SwrveLogger.i("Event queued of type: %s and seqNum:%s for userId:%s", eventType, seqNum, userId);
        } catch (Exception e) {
            SwrveLogger.e("Unable to insert QueueEvent into local storage. EventString:" + eventString, e);
        }
    }
}

