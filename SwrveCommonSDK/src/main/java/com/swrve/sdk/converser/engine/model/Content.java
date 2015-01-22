package com.swrve.sdk.converser.engine.model;

public class Content extends ConversationAtom {
    protected String value;
    protected String height;

    public String getValue() {
        return value;
    }

    public String getHeight() {
        if (height == null || Integer.parseInt(height) <= 0) {
            return "0";
        } else {
            return height;
        }
    }
}
