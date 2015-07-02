package com.swrve.sdk.conversations.engine.model;

import java.io.Serializable;
import java.util.ArrayList;

public class MultiValueLongInput extends InputBase implements Serializable {
    private String description;
    private ArrayList<Item> values;

    public ArrayList<Item> getValues() {
        return values;
    }

    public String getDescription() {
        return description;
    }

    public static class Item implements Serializable {
        private String title;
        private String question_id;
        private boolean error;
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

        public boolean hasError() {
            return error;
        }

        public void setError(boolean error) {
            this.error = error;
        }
    }
}
