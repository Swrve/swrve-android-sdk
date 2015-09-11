package com.swrve.sdk.locationcampaigns.model;

import com.google.gson.Gson;

import java.io.Serializable;

public class LocationPayload implements Serializable {

    private String geofenceId;
    private String campaignId;

    public String getGeofenceId() {
        return geofenceId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public static LocationPayload fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, LocationPayload.class);
    }
}
