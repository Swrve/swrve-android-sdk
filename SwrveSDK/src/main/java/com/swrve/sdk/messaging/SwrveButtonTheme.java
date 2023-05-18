package com.swrve.sdk.messaging;

import org.json.JSONException;
import org.json.JSONObject;

public class SwrveButtonTheme {

    private final int fontSize;
    private final String fontFile;
    private String fontDigest;
    private String fontNativeStyle; // Only used with system font
    private final int topPadding;
    private final int rightPadding;
    private final int bottomPadding;
    private final int leftPadding;
    private final int cornerRadius;
    private final String fontColor;
    private String bgColor;
    private int borderWidth;
    private String borderColor;
    private String bgImage;
    private final boolean truncate;
    private final SwrveButtonThemeState pressedState;
    private SwrveButtonThemeState focusedState;
    private String hAlign;

    public SwrveButtonTheme(JSONObject jsonData) throws JSONException {
        this.fontSize = jsonData.getInt("font_size");
        this.fontFile = jsonData.getString("font_file");
        if (jsonData.has("font_native_style") && !jsonData.isNull("font_native_style")) {
            this.fontNativeStyle = jsonData.getString("font_native_style");
        }
        if (jsonData.has("font_digest")) {
            this.fontDigest = jsonData.getString("font_digest");
        }
        JSONObject padding = jsonData.getJSONObject("padding");
        this.topPadding = padding.getInt("top");
        this.bottomPadding = padding.getInt("bottom");
        this.rightPadding = padding.getInt("right");
        this.leftPadding = padding.getInt("left");
        this.cornerRadius = jsonData.getInt("corner_radius");
        this.fontColor = jsonData.getString("font_color");
        if (jsonData.has("bg_color") && !jsonData.isNull("bg_color")) {
            this.bgColor = jsonData.getString("bg_color");
        }
        if (jsonData.has("border_width") && !jsonData.isNull("border_width")) {
            this.borderWidth = jsonData.getInt("border_width");
        }
        if (jsonData.has("border_color") && !jsonData.isNull("border_color")) {
            this.borderColor = jsonData.getString("border_color");
        }
        if (jsonData.has("bg_image") && !jsonData.isNull("bg_image")) {
            this.bgImage = jsonData.getJSONObject("bg_image").getString("value");
        }
        this.truncate = jsonData.getBoolean("truncate");
        this.pressedState = new SwrveButtonThemeState(jsonData.getJSONObject("pressed_state"));
        if (jsonData.has("focused_state") && !jsonData.isNull("focused_state")) {
            this.focusedState = new SwrveButtonThemeState(jsonData.getJSONObject("focused_state"));
        }
        this.hAlign = jsonData.getString("h_align");
    }

    public int getFontSize() {
        return fontSize;
    }

    public String getFontFile() {
        return fontFile;
    }

    public String getFontDigest() {
        return fontDigest;
    }

    public String getFontNativeStyle() {
        return fontNativeStyle;
    }

    public int getTopPadding() {
        return topPadding;
    }

    public int getRightPadding() {
        return rightPadding;
    }

    public int getBottomPadding() {
        return bottomPadding;
    }

    public int getLeftPadding() {
        return leftPadding;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public String getFontColor() {
        return fontColor;
    }

    public String getBgColor() {
        return bgColor;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public String getBgImage() {
        return bgImage;
    }

    public boolean isTruncate() {
        return truncate;
    }

    public SwrveButtonThemeState getPressedState() {
        return pressedState;
    }

    public SwrveButtonThemeState getFocusedState() {
        return focusedState;
    }

    public String getHAlign() {
        return hAlign;
    }
}
