package com.swrve.sdk.messaging.model;

public class Trigger {
    private String eventName;

    private Conditions conditions;

    public String getEventName() {
        return eventName;
    }

    public Conditions getConditions() {
        return conditions;
    }
}
