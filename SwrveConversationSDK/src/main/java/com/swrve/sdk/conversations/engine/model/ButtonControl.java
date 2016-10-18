package com.swrve.sdk.conversations.engine.model;

import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

public class ButtonControl extends ControlBase {
    protected String description;

    public ButtonControl(String tag, TYPE type, ConversationStyle style, ControlActions action, String target, String description) {
        super(tag, type, style, action, target);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
