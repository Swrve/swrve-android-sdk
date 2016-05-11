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

    private static final String LOG_TAG = "SwrveSDK";

    private String eventName;
    private Conditions conditions;

    public static List<Trigger> fromJson(String json, int id) {
        List<Trigger> triggers = null;
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
            Gson gson = gsonBuilder.create();
            Type listType = new TypeToken<List<Trigger>>(){}.getType();
            triggers = gson.fromJson(json, listType);
            triggers = validateTriggers(triggers, id);
        }catch (JsonParseException ex) {
            SwrveLogger.e(LOG_TAG, "Could not parse campaign[" + id + "] trigger json:" + json, ex);
        }
        return triggers;
    }

    private static List<Trigger> validateTriggers(List<Trigger> triggers, int id) {
        for (Trigger trigger : triggers) {
            Conditions conditions = trigger.getConditions();
            if (conditions == null) {
                SwrveLogger.e(LOG_TAG, "Invalid trigger in campaign[" + id + "] trigger:" + trigger);
                return null;
            } else if (conditions.getOp() == null && conditions.getValue() == null && conditions.getKey() == null && conditions.getArgs() == null) {
                continue; // no conditions is valid, check next trigger
            } else if (Conditions.Op.AND.equals(conditions.getOp())) {
                if (conditions.getArgs() == null || conditions.getArgs().size() == 0) {
                    SwrveLogger.e(LOG_TAG, "Invalid trigger in campaign[" + id + "] trigger:" + trigger);
                    return null;
                } else {
                    for (Arg arg : conditions.getArgs()) {
                        if (arg.getKey() == null || arg.getOp() == null || !Arg.Op.EQ.equals(arg.getOp()) || arg.getValue() == null) {
                            SwrveLogger.e(LOG_TAG, "Invalid trigger in campaign[" + id + "] trigger:" + trigger);
                            return null;
                        }
                    }
                }
            } else if (Conditions.Op.EQ.equals(conditions.getOp())) {
                if (conditions.getKey() == null || conditions.getValue() == null) {
                    SwrveLogger.e(LOG_TAG, "Invalid trigger in campaign[" + id + "] trigger:" + trigger);
                    return null;
                }
            } else {
                SwrveLogger.e(LOG_TAG, "Invalid trigger in campaign[" + id + "] trigger:" + trigger);
                return null;
            }
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
