package com.swrve.sdk.converser.engine.model;

import java.io.Serializable;
import java.util.ArrayList;

public class MultiValueInput extends InputBase implements Serializable {
    private String description;
    private ArrayList<ChoiceInputItem> values;

    public ArrayList<ChoiceInputItem> getValues() {
        return values;
    }

    public String getDescription() {
        return description;
    }
}
