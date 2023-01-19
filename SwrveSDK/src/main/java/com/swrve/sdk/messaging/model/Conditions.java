package com.swrve.sdk.messaging.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Conditions {

    public enum Op {
        @SerializedName("or")
        OR,
        @SerializedName("and")
        AND,
        @SerializedName("contains")
        CONTAINS,
        @SerializedName("number_gt")
        NUMBER_GT,
        @SerializedName("number_lt")
        NUMBER_LT,
        @SerializedName("number_between")
        NUMBER_BETWEEN,
        @SerializedName("number_not_between")
        NUMBER_NOT_BETWEEN,
        @SerializedName("number_eq")
        NUMBER_EQ,
        @SerializedName("eq")
        EQ
    }

    private Op op = null;
    private String key;
    private Object value;
    private List<Arg> args;

    public String getKey() {
        return key;
    }

    public Op getOp() {
        return op;
    }

    public Object getValue() {
        return value;
    }

    public List<Arg> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return "Conditions{" +
                "key='" + key + '\'' +
                ", op='" + op + '\'' +
                ", value='" + value + '\'' +
                ", args=" + args +
                '}';
    }
}
