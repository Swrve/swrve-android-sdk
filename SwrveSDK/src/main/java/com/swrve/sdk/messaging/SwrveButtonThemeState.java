package com.swrve.sdk.messaging;

import org.json.JSONException;
import org.json.JSONObject;

public class SwrveButtonThemeState {

    private String fontColor;
    private String bgColor;
    private String borderColor;
    private String bgImage;

    public SwrveButtonThemeState(JSONObject jsonData) throws JSONException {
        if (jsonData.has("font_color") && !jsonData.isNull("font_color")) {
            this.fontColor = jsonData.getString("font_color");
        }
        if (jsonData.has("bg_color") && !jsonData.isNull("bg_color")) {
            this.bgColor = jsonData.getString("bg_color");
        }
        if (jsonData.has("border_color") && !jsonData.isNull("border_color")) {
            this.borderColor = jsonData.getString("border_color");
        }
        if (jsonData.has("bg_image") && !jsonData.isNull("bg_image")) {
            this.bgImage = jsonData.getJSONObject("bg_image").getString("value");
        }
    }

    public String getFontColor() {
        return fontColor;
    }

    public String getBgColor() {
        return bgColor;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public String getBgImage() {
        return bgImage;
    }
}
