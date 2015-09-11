package com.swrve.sdk.locationcampaigns.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

public class LocationCampaign  {

    private int id;
    private long start;
    private long end;
    private int version;
    private LocationMessage message;

    public int getId() {
        return id;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
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
        return locationCampaigns;
    }
}