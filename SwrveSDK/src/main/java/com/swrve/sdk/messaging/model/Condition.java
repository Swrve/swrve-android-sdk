package com.swrve.sdk.messaging.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class Condition {

    @SerializedName("$and")
    private List<Map<String, String>> and;
    public List<Map<String, String>> getAnd() {
        return and;
    }
}
