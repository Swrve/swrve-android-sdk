package com.swrve.sdk.conversations.engine.model;

import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

public abstract class ControlBase extends ConversationAtom {
    protected ControlActions action;
    protected String target;

    public ControlBase(String tag, TYPE type, ConversationStyle style, ControlActions action, String target) {
        super(tag, type, style);
        this.action = action;
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    public ControlActions getActions() {
        return action;
    }

    public boolean hasActions() {
        return action != null;
    }

}
