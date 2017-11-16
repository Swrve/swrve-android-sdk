package com.swrve.sdk.localstorage;

class SwrveEventItem {
    public static long eventCount = 0;
    public long id;
    public String event;
    public String userId;
    public SwrveEventItem() {
        this.id = (eventCount++);
    }
}
