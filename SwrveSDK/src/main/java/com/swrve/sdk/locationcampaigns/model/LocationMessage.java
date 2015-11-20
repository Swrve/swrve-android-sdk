package com.swrve.sdk.locationcampaigns.model;


import android.support.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class LocationMessage {

    private final int id;
    private final String body;
    private final String payload;

    @VisibleForTesting
    public LocationMessage(int id, String body, String payload) {
        this.id = id;
        this.body = body;
        this.payload = payload;
    }

    public int getId() {
        return id;
    }
    public String getBody() {
        return body;
    }
    public String getPayload() {
        return payload;
    }

    public static String toJSON(LocationMessage locationMessage) {
        Gson gson = new Gson();
        return gson.toJson(locationMessage);
    }

    public static LocationMessage fromJSON(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, LocationMessage.class);
    }

    public Map<String, String> getPayloadMap() {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> map = gson.fromJson(payload, type);
        return map;
    }

    @Override
    public String toString() {
        return "LocationMessage{" +
                "id=" + id +
                ", body='" + body + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}
