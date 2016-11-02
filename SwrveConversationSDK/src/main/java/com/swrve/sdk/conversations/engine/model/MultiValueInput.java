package com.swrve.sdk.conversations.engine.model;

import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

import java.io.Serializable;
import java.util.ArrayList;

public class MultiValueInput extends ConversationAtom implements Serializable {
    private String description;
    private ArrayList<ChoiceInputItem> values;

    public MultiValueInput(String tag, TYPE type, ConversationStyle style, String description, ArrayList<ChoiceInputItem> values) {
        super(tag, type, style);
        this.description = description;
        this.values = values;
    }

    public ArrayList<ChoiceInputItem> getValues() {
        return values;
    }

    public String getDescription() {
        return description;
    }
}
