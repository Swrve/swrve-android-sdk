package com.swrve.sdk.messaging.model;

import java.util.List;

public class Conditions {

    private String op;
    private List<Arg> args;

    public String getOp() {
        return op;
    }

    public List<Arg> getArgs() {
        return args;
    }
}
