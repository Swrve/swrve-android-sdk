package com.swrve.sdk.converser.engine.model;

import java.io.Serializable;
import java.util.ArrayList;

public class MultiValueLongInput extends InputBase implements Serializable {
    private ArrayList<Item> values;

    public ArrayList<Item> getValues() {
        return values;
    }

    public static class Item  implements Serializable {
        private String title;
        private ArrayList<String> options;

        public String getTitle() {
            return title;
        }

        public ArrayList<String> getOptions() {
            return options;
        }
    }
}
