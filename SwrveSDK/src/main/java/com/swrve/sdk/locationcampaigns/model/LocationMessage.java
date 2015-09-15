package com.swrve.sdk.locationcampaigns.model;


import android.support.annotation.VisibleForTesting;

public class LocationMessage {

    private int id;
    private String body;

    @VisibleForTesting
    public LocationMessage(int id, String body) {
        this.id = id;
        this.body = body;
    }

    public int getId() {
        return id;
    }
    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "LocationMessage{" +
                "id=" + id +
                ", body='" + body + '\'' +
                '}';
    }
}
