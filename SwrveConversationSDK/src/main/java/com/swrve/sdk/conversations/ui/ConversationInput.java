package com.swrve.sdk.conversations.ui;

import com.swrve.sdk.conversations.engine.model.UserInputResult;

import java.util.Map;

public interface ConversationInput {

    void gatherValue(Map<String, Object> dataMap);

    /**
     * Each ConversationInput should be able to have its value set programmatically so the user sees certain things.
     */
    void setUserInput(UserInputResult r);
}
