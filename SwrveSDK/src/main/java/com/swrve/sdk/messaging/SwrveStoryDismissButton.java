package com.swrve.sdk.messaging;

import org.json.JSONException;
import org.json.JSONObject;

public class SwrveStoryDismissButton {

    private int buttonId;
    private String name;
    private String color;
    private String pressedColor;
    private String focusedColor;
    private int size;
    private int marginTop;
    private String accessibilityText;

    public SwrveStoryDismissButton(JSONObject jsonObject) throws JSONException {

        if (jsonObject == null) {
            return;
        }

        this.buttonId = jsonObject.getInt("id");
        this.name = jsonObject.getString("name");
        this.color = jsonObject.getString("color");
        this.pressedColor = jsonObject.getString("pressed_color");
        if(jsonObject.has("focused_color")) {
            this.focusedColor = jsonObject.getString("focused_color");
        }
        this.size = jsonObject.getInt("size");
        this.marginTop = jsonObject.getInt("margin_top");
        this.accessibilityText = jsonObject.getString("accessibility_text");
    }

    public int getButtonId() {
        return buttonId;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getPressedColor() {
        return pressedColor;
    }

    public String getFocusedColor() {
        return focusedColor;
    }

    public int getSize() {
        return size;
    }

    public int getMarginTop() {
        return marginTop;
    }

    public String getAccessibilityText() {
        return accessibilityText;
    }
}
