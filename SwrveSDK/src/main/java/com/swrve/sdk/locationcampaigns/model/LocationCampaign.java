package com.swrve.sdk.locationcampaigns.model;

import android.support.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

public class LocationCampaign  {

    private String id;
    private final int version;
    private final LocationMessage message;

    @VisibleForTesting
    public LocationCampaign(String id, int version, LocationMessage message) {
        this.id = id;
        this.version = version;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public LocationMessage getMessage() {
        return message;
    }

    public static Map<String, LocationCampaign> fromJSON(String jsonString) {
        Gson gson = new Gson();
        Map<String, LocationCampaign> locationCampaigns = gson.fromJson(jsonString, new TypeToken<Map<String, LocationCampaign>>(){}.getType());
        for (Map.Entry<String, LocationCampaign> entry : locationCampaigns.entrySet())
        {
            entry.getValue().id = entry.getKey(); // populate the id field into the location campaign objects
        }
        return locationCampaigns;
    }

    @Override
    public String toString() {
        return "LocationCampaign{" +
                "id=" + id +
                ", version=" + version +
                ", message=" + message +
                '}';
    }
}