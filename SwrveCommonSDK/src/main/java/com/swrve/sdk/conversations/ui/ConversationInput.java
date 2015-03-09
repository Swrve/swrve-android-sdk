package com.swrve.sdk.conversations.ui;

import com.swrve.sdk.conversations.engine.model.UserInputResult;

import java.io.Serializable;
import java.util.Map;

public interface ConversationInput {
    public void onReplyDataRequired(Map<String, Object> dataMap);

    /**
     * perform any validation needed
     *
     * @return null if validated ok. Otherwise a message detailing issue
     */
    public String validate();

    /**
     * Each ConversationInput should be able to have its value set programmatically so the user sees certain things.
     *
     * @return null if validated ok. Otherwise a message detailing issue
     */
    public void setUserInput(UserInputResult r);
}
