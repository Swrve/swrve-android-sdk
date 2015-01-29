package com.swrve.sdk.converser.engine.model;

public class ConverserInputResult {
    public String pageTag;
    public String type;
    public Object result;

    public String getType() {
        return type;
    }

    public String getPageTag() {
        return pageTag;
    }


    public String getResultAsString() {
        return result.toString();
    }
}
