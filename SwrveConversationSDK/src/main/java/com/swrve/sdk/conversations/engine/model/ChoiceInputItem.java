package com.swrve.sdk.conversations.engine.model;

import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

import java.io.Serializable;

public class ChoiceInputItem implements Serializable {
    private String answer_id;
    private String answer_text;
    private ConversationStyle style;
    
    public ChoiceInputItem(String answer_id, String answer_text, ConversationStyle style) {
        this.answer_id = answer_id;
        this.answer_text = answer_text;
        this.style = style;
    }

    public String getAnswerID() {
        return answer_id;
    }

    public String getAnswerText() {
        return answer_text;
    }

    public ConversationStyle getStyle() {
        return style;
    }

    public void setStyle(ConversationStyle style) {
        this.style = style;
    }

    @Override
    public String toString() {
        return getAnswerText(); // This is displayed to the user
    }
}
