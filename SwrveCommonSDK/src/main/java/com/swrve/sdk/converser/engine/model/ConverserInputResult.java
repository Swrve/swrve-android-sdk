package com.swrve.sdk.converser.engine.model;

public class ConverserInputResult {
    public String type;
    public Object result;

    public String getType() {
        return type;
    }

    public String getResultAsString() {
        return result.toString();
    }
}
