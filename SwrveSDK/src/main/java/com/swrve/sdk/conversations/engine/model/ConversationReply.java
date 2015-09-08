package com.swrve.sdk.conversations.engine.model;

import java.util.HashMap;

public class ConversationReply {
    private String control;
    private HashMap<String, Object> data;

    public ConversationReply() {
        data = new HashMap<String, Object>();
    }

    public String getControl() {
        return control;
    }

    public void setControl(String control) {
        this.control = control;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }
}
