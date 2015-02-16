package com.swrve.sdk.conversations.engine.model;

import java.io.Serializable;
import java.util.ArrayList;

public class MultiValueLongInput extends InputBase implements Serializable {
    private ArrayList<Item> values;
    private String title;

    public ArrayList<Item> getValues() {
        return values;
    }

    public static class Item implements Serializable {
        private String title;
        private String question_id;
        private ArrayList<ChoiceInputItem> options;

        public String getTitle() {
            return title;
        }

        public ArrayList<ChoiceInputItem> getOptions() {
            return options;
        }

        public String getQuestionId() {
            return question_id;
        }
    }
}
