package com.swrve.sdk.conversations.engine.model;

import java.io.Serializable;

public abstract class ControlBase extends ConversationAtom {
    protected ControlActions action;
    protected String target;

    public ControlBase(){

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
