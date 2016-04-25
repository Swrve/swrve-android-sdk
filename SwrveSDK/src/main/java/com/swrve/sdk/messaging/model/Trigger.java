package com.swrve.sdk.messaging.model;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.swrve.sdk.SwrveLogger;

import java.lang.reflect.Type;
import java.util.List;

public class Trigger {
    private String eventName;

    private Conditions conditions;

    public static List<Trigger> fromJson(String json) {
        List<Trigger> triggers = null;
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
            Gson gson = gsonBuilder.create();
            Type listType = new TypeToken<List<Trigger>>(){}.getType();
            triggers = gson.fromJson(json, listType);
        }catch (JsonParseException ex) {
            SwrveLogger.e("SwrveSDK", "Could not parse trigger json:" + json, ex);
        }
        return triggers;
    }

    public String getEventName() {
        return eventName;
    }

    public Conditions getConditions() {
        return conditions;
    }

    @Override
    public String toString() {
        return "Trigger{" +
                "eventName='" + eventName + '\'' +
                ", conditions=" + conditions +
                '}';
    }
}
