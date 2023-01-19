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

    public static List<Trigger> fromJson(String json, int id) {
        List<Trigger> triggers = null;
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
            Gson gson = gsonBuilder.create();
            Type listType = new TypeToken<List<Trigger>>() {
            }.getType();
            triggers = gson.fromJson(json, listType);
            triggers = validateTriggers(triggers, id);
        } catch (JsonParseException ex) {
            SwrveLogger.e("Could not parse campaign[%s] trigger json:%s" + json, ex, id, json);
        }
        return triggers;
    }

    private static List<Trigger> validateTriggers(List<Trigger> triggers, int id) {
        for (Trigger trigger : triggers) {
            Conditions conditions = trigger.getConditions();
            if (conditions == null) {
                SwrveLogger.e("Invalid trigger in campaign[%s] trigger:%s", id, trigger);
                return null;
            } else if (conditions.getOp() == null && conditions.getValue() == null && conditions.getKey() == null && conditions.getArgs() == null) {
                continue; // no conditions is valid, check next trigger
            } else if (Conditions.Op.AND.equals(conditions.getOp()) || Conditions.Op.OR.equals(conditions.getOp())) {
                if (conditions.getArgs() == null || conditions.getArgs().size() == 0) {
                    SwrveLogger.e("Invalid trigger in campaign[%s] trigger:%s", id, trigger);
                    return null;
                } else {
                    for (Arg arg : conditions.getArgs()) {
                        if (arg.getKey() == null || arg.getOp() == null || arg.getValue() == null) {
                            SwrveLogger.e("Invalid trigger in campaign[%s] trigger:%s", id, trigger);
                            return null;
                        }
                    }
                }
            } else if (Conditions.Op.NUMBER_BETWEEN.equals(conditions.getOp()) || Conditions.Op.NUMBER_EQ.equals(conditions.getOp()) || Conditions.Op.NUMBER_LT.equals(conditions.getOp()) || Conditions.Op.NUMBER_NOT_BETWEEN.equals(conditions.getOp()) || Conditions.Op.NUMBER_GT.equals(conditions.getOp()) || Conditions.Op.EQ.equals(conditions.getOp()) || Conditions.Op.CONTAINS.equals(conditions.getOp())) {
                if (conditions.getKey() == null || conditions.getValue() == null) {
                    SwrveLogger.e("Invalid trigger in campaign[%s] trigger:%s", id, trigger);
                    return null;
                }
            } else {
                SwrveLogger.e("Invalid trigger in campaign[%s] trigger:%s", id, trigger);
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
