package com.swrve.sdk.messaging.model;

import com.google.gson.annotations.SerializedName;

public class Arg {

    public enum Op {
        @SerializedName("eq")
        EQ,
        @SerializedName("contains")
        CONTAINS,
        @SerializedName("number_gt")
        NUMBER_GT,
        @SerializedName("number_lt")
        NUMBER_LT,
        @SerializedName("number_eq")
        NUMBER_EQ,
        @SerializedName("number_not_between")
        NUMBER_NOT_BETWEEN,
        @SerializedName("number_between")
        NUMBER_BETWEEN
    }

    private String key;
    private Op op;
    private Object value;

    public String getKey() {
        return key;
    }

    public Op getOp() {
        return op;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Arg{" +
                "key='" + key + '\'' +
                ", op='" + op + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
