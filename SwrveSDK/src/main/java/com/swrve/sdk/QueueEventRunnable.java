package com.swrve.sdk;

import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class QueueEventRunnable implements Runnable {

    private SwrveMultiLayerLocalStorage multiLayerLocalStorage;
    private String userId;
    private String eventType;
    private Map<String, Object> parameters;
    private Map<String, String> payload;

    public QueueEventRunnable(SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId, String eventType, Map<String, Object> parameters, Map<String, String> payload) {
        this.multiLayerLocalStorage = multiLayerLocalStorage;
        this.userId = userId;
        this.eventType = eventType;
        this.parameters = parameters;
        this.payload = payload;
    }

    @Override
    public void run() {
        String eventString = "";
        try {
            int seqNum = SwrveCommon.getInstance().getNextSequenceNumber();
            long time = System.currentTimeMillis();
            eventString = EventHelper.eventAsJSON(eventType, parameters, payload, seqNum, time);
            multiLayerLocalStorage.addEvent(userId, eventString);
            SwrveLogger.i("Event queued of type: %s and seqNum:%s for userId:%s", eventType, seqNum, userId);

            List<String> events = new ArrayList<>();
            events.add(eventString);
            QaUser.wrappedEvents(events);
        } catch (Exception e) {
            SwrveLogger.e("Unable to insert QueueEvent into local storage. EventString:" + eventString, e);
        }
    }
}

