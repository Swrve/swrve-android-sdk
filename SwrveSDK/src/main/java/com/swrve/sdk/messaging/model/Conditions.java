package com.swrve.sdk.messaging.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Conditions {

    public enum Op {
        @SerializedName("and")
        AND,
        @SerializedName("eq")
        EQ
    }

    private Op op = null;
    private String key;
    private String value;
    private List<Arg> args;

    public String getKey() {
        return key;
    }

    public Op getOp() {
        return op;
    }

    public String getValue() {
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
