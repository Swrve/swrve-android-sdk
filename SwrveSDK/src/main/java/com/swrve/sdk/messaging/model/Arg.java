package com.swrve.sdk.messaging.model;

public class Arg {
    private String key;
    private String op;
    private String value;

    public String getKey() {
        return key;
    }

    public String getOp() {
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
