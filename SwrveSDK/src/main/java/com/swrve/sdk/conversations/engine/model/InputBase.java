package com.swrve.sdk.conversations.engine.model;

public class InputBase extends ConversationAtom {
    private boolean optional = false;
    private boolean error;

    public boolean isOptional() {
        return optional;
    }

    public boolean hasError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }
}
