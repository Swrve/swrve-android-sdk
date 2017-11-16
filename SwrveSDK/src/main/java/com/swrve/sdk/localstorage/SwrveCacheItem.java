package com.swrve.sdk.localstorage;

class SwrveCacheItem {
    public String userId;
    public String category;
    public String rawData;

    SwrveCacheItem(String userId, String category, String rawData) {
        this.userId = userId;
        this.category = category;
        this.rawData = rawData;
    }
}
