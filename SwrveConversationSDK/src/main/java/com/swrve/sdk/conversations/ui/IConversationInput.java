package com.swrve.sdk.conversations.ui;

import com.swrve.sdk.conversations.engine.model.UserInputResult;

public interface IConversationInput {

    /**
     * Each ConversationInput should be able to have its value set programmatically so the user sees certain things.
     */
    void setUserInput(UserInputResult r);
}
