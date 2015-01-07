package com.swrve.sdk.converser.engine.model;

public class TextInput extends InputBase {

    public static final String KEYBOARD_URL = "url";
    public static final String KEYBOARD_EMAIL = "email";
    public static final String KEYBOARD_PHONE = "phone";
    public static final String KEYBOARD_NUMBER = "number";
    private String placeholder;
    private int lines;
    private String kbd = null;
    private String description;

    public String getPlaceholder() {
        return placeholder;
    }

    public int getLines() {
        return lines;
    }

    public String getKeyboardType() {
        return kbd;
    }

    public String getDescription() {
        return description;
    }

}
