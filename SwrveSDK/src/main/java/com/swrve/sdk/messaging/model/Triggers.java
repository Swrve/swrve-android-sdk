package com.swrve.sdk.messaging.model;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.swrve.sdk.SwrveLogger;

import java.util.List;

public class Triggers {

    public static Triggers fromJson(String json) {
        Triggers triggers = null;
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
            Gson gson = gsonBuilder.create();
            triggers = gson.fromJson(json, Triggers.class);
        }catch (JsonParseException ex) {
            SwrveLogger.e("SwrveSDK", "Could not parse trigger json:" + json, ex);
        }
        return triggers;
    }

    private List<Trigger> triggers;

    public List<Trigger> getTriggers() {
        return triggers;
    }

}
