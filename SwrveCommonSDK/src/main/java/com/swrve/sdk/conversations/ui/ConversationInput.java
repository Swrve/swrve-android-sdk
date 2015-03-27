package com.swrve.sdk.conversations.ui;

import com.swrve.sdk.conversations.engine.model.UserInputResult;

import java.util.Map;

public interface ConversationInput {

    public void onReplyDataRequired(Map<String, Object> dataMap);

    /**
     * Perform any validation needed
     *
     * @return true if validation passed ok. Otherwise false to indicate there were errors.
     */
    public boolean validate();

    /**
     * Each ConversationInput should be able to have its value set programmatically so the user sees certain things.
     */
    public void setUserInput(UserInputResult r);
}
