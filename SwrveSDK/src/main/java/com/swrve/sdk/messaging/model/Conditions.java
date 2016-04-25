package com.swrve.sdk.messaging.model;

import java.util.List;

public class Conditions {

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

    private List<Arg> args;

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
