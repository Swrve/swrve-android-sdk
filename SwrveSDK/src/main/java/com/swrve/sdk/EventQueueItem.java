package com.swrve.sdk;

import java.util.Map;

class EventQueueItem {

    protected final String userId;
    protected final String eventType;
    protected final Map<String, Object> parameters;
    protected final Map<String, String> payload;
    protected final boolean triggerEventListener;

    EventQueueItem(String userId, String eventType, Map<String, Object> parameters, Map<String, String> payload, boolean triggerEventListener) {
        this.userId = userId;
        this.eventType = eventType;
        this.parameters = parameters;
        this.payload = payload;
        this.triggerEventListener = triggerEventListener;
    }
}
