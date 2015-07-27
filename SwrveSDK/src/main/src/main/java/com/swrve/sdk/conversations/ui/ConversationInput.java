package com.swrve.sdk.conversations.ui;

import com.swrve.sdk.conversations.engine.model.UserInputResult;

import java.util.Map;

public interface ConversationInput {

    public void onReplyDataRequired(Map<String, Object> dataMap);

    /**
     * Check input passes validation.
     *
     * @return true if validation passed ok. Otherwise false to indicate there were errors.
     */
    public boolean isValid();

    /**
     * Each ConversationInput should be able to have its value set programmatically so the user sees certain things.
     */
    public void setUserInput(UserInputResult r);
}
