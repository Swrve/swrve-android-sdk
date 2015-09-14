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

    /**
     * json example:
     * {
     *      "geoFenceId": "e12a97f53b624328b9e919489da2ab9e",
     *      "campaignId": "456"
     * }
     * @param json the json to parse
     * @return a deserialized json Object
     */
    public static LocationPayload fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, LocationPayload.class);
    }

    @Override
    public String toString() {
        return "LocationPayload{" +
                "geofenceId='" + geofenceId + '\'' +
                ", campaignId='" + campaignId + '\'' +
                '}';
    }
}
