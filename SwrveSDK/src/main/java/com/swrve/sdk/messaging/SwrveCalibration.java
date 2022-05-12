package com.swrve.sdk.messaging;

import org.json.JSONException;
import org.json.JSONObject;

public class SwrveCalibration {

    private int width;
    private int height;
    private int baseFontSize;
    private String text;

    public SwrveCalibration(JSONObject jsonObject) throws JSONException {

        if (jsonObject != null && jsonObject.has("width")) {
            this.width = jsonObject.getInt("width");
        }

        if (jsonObject != null && jsonObject.has("height")) {
            this.height = jsonObject.getInt("height");
        }

        if (jsonObject != null && jsonObject.has("base_font_size")) {
            this.baseFontSize = jsonObject.getInt("base_font_size");
        }

        if (jsonObject != null && jsonObject.has("text")) {
            this.text = jsonObject.getString("text");
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBaseFontSize() {
        return baseFontSize;
    }

    public String getText() {
        return text;
    }
}
