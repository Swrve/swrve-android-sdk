package com.swrve.sdk.conversations.engine.model;

import java.io.Serializable;

public class ChoiceInputItem implements Serializable {
    private String answer_id;
    private String answer_text;

    public ChoiceInputItem(String answer_id, String answer_text) {
        this.answer_id = answer_id;
        this.answer_text = answer_text;
    }

    public String getAnswerID() {
        return answer_id;
    }

    public String getAnswerText() {
        return answer_text;
    }

    @Override
    // This guy is important for how he is displayed to the user
    public String toString() {
        return getAnswerText();
    }
}
