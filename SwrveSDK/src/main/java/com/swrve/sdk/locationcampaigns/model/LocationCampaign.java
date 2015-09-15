package com.swrve.sdk.locationcampaigns.model;

import android.support.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

public class LocationCampaign  {

    private final int id;
    private final long start;
    private final long end;
    private final int version;
    private final LocationMessage message;

    @VisibleForTesting
    public LocationCampaign(int id, long start, long end, int version, LocationMessage message) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.version = version;
        this.message = message;
    }

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

    @Override
    public String toString() {
        return "LocationCampaign{" +
                "id=" + id +
                ", start=" + start +
                ", end=" + end +
                ", version=" + version +
                ", message=" + message +
                '}';
    }
}