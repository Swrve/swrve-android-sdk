package com.swrve.sdk.locationcampaigns.model;

import com.google.gson.Gson;

import java.io.Serializable;

public class LocationCampaignPayload implements Serializable {

    private String geofenceId;
    private String campaignId;

    public String getGeofenceId() {
        return geofenceId;
    }

    public void setGeofenceId(String geofenceId) {
        this.geofenceId = geofenceId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public static LocationCampaignPayload fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, LocationCampaignPayload.class);
    }
}
