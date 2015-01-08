package com.swrve.sdk.converser.engine.model;

import java.util.ArrayList;

public class MultiValueInput extends InputBase {

    private String description;
    private ArrayList<MultiValueItem> values;

    public ArrayList<MultiValueItem> getValues() {
        return values;
    }


    public String getDescription() {
        return description;
    }

    public static class MultiValueItem {

        private String name;
        private String value;

        public MultiValueItem(String name, String value) {
            super();

            this.name = name;
            this.value = value;

        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }
}
