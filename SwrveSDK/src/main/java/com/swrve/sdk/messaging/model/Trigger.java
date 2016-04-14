package com.swrve.sdk.messaging.model;

public class Trigger {
    private String eventName;

    private Condition conditions;

    public String getEventName() {
        return eventName;
    }

    public Condition getConditions() {
        return conditions;
    }
}
