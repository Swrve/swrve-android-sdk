package com.swrve.sdk.conversations.engine.model;

import java.io.Serializable;
import java.util.ArrayList;

public class MultiValueInput extends ConversationAtom implements Serializable {
    private String description;
    private ArrayList<ChoiceInputItem> values;

    public ArrayList<ChoiceInputItem> getValues() {
        return values;
    }

    public String getDescription() {
        return description;
    }
}
