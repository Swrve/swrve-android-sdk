package io.converser.android.engine.model;

import java.util.ArrayList;

public class SliderInput extends InputBase {

    protected ArrayList<SliderValue> values;

    public ArrayList<SliderValue> getValues() {
        return values;
    }


    public static class SliderValue {

        private String label;
        private int value;


        public SliderValue(String label, int value) {
            super();
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public int getValue() {
            return value;
        }


    }
}
