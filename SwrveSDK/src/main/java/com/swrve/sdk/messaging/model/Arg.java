package com.swrve.sdk.messaging.model;

import com.google.gson.annotations.SerializedName;

public class Arg {

    public enum Op {
        @SerializedName("eq")
        EQ
    }

    private String key;
    private Op op;
    private String value;

    public String getKey() {
        return key;
    }

    public Op getOp() {
        return op;
    }

    public String getValue() {
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
