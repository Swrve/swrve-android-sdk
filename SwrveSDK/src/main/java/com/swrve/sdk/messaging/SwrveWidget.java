package com.swrve.sdk.messaging;

import android.graphics.Color;
import android.graphics.Point;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.messaging.SwrveTextViewStyle.TextAlignment;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base in-app message element widget class.
 */
abstract class SwrveWidget {

    // Position of the widget
    protected Point position;
    // Size of the widget
    protected Point size;

    // Personalized text (render this instead of the image)
    protected String text;
    // Dynamic Image Url
    protected String dynamicImageUrl;
    //Alternative text for use with accessibility voice over
    protected String accessibilityText;
    // if true, text value will have the below items applied to it
    protected boolean isMultiLine;
    // Font Size (used primarily for multi-line)
    protected float fontSize;
    // If it's text, is it scrollable? (used primarily for multi-line)
    protected boolean isScrollable;
    // What is the alignment of the text?
    protected TextAlignment horizontalAlignment;

    protected String fontFile;
    protected String fontDigest;
    protected SwrveTextViewStyle.FONT_NATIVE_STYLE fontNativeStyle;
    protected double lineHeight;
    protected int topPadding;
    protected int rightPadding;
    protected int bottomPadding;
    protected int leftPadding;
    protected String foregroundColor;
    protected String backgroundColor;
    private int iamZIndex;

    public SwrveWidget() {
    }

    public SwrveWidget(JSONObject data) throws JSONException {
        if (data.has("dynamic_image_url")) {
            this.dynamicImageUrl = data.getString("dynamic_image_url");
        }

        if (data.has("text")) {
            this.text = data.getJSONObject("text").getString("value");
        }

        if (data.has("accessibility_text")) {
            this.accessibilityText = data.getString("accessibility_text");
        }

        //added in app version 5
        if (data.has("multiline_text")) {
            this.isMultiLine = true;
            JSONObject multiLineData = data.getJSONObject("multiline_text");
            this.text = multiLineData.getString("value");
            Double font_size = multiLineData.getDouble("font_size");
            this.fontSize = font_size.floatValue();
            this.isScrollable = multiLineData.getBoolean("scrollable");
            this.horizontalAlignment = TextAlignment.parse(multiLineData.getString("h_align"));

            //custom fonts, color, padding and line height added in app version 6
            if (multiLineData.has("font_file")) {
                String fontFile = multiLineData.getString("font_file");
                this.fontFile = fontFile;

                if (SwrveTextUtils.isSystemFont(fontFile)) {
                    this.fontNativeStyle = SwrveTextViewStyle.FONT_NATIVE_STYLE.parse(multiLineData.getString("font_native_style"));
                }

                if (multiLineData.has("line_height")) {
                    this.lineHeight = multiLineData.getDouble("line_height");
                }

                if (multiLineData.has("font_digest")) {
                    this.fontDigest = multiLineData.getString("font_digest");
                }

                if (multiLineData.has("padding")) {
                    JSONObject padding = multiLineData.getJSONObject("padding");
                    this.topPadding = padding.getInt("top");
                    this.bottomPadding = padding.getInt("bottom");
                    this.rightPadding = padding.getInt("right");
                    this.leftPadding = padding.getInt("left");
                }

                if (multiLineData.has("font_color")) {
                    this.foregroundColor = multiLineData.getString("font_color");
                }

                if (multiLineData.has("bg_color")) {
                    this.backgroundColor = multiLineData.getString("bg_color");
                }
            }
        }

        if (data.has("iam_z_index")) {
            this.iamZIndex = data.getInt("iam_z_index");
        }
    }

    // Get size from the format data
    protected static Point getSizeFrom(JSONObject data) throws JSONException {
        return new Point(data.getJSONObject("w").getInt("value"), data.getJSONObject("h").getInt("value"));
    }

    // Get center from the format data
    protected static Point getCenterFrom(JSONObject data) throws JSONException {
        return new Point(data.getJSONObject("x").getInt("value"), data.getJSONObject("y").getInt("value"));
    }

    /**
     * @return the position of the widget.
     */
    public Point getPosition() {
        return position;
    }

    protected void setPosition(Point position) {
        this.position = position;
    }

    /**
     * @return the size of the widget.
     */
    public Point getSize() {
        return size;
    }

    protected void setSize(Point size) {
        this.size = size;
    }

    /**
     * @return the cached path of the button dynamic image url.
     */
    public String getDynamicImageUrl() {
        return dynamicImageUrl;
    }

    protected void setDynamicImageUrl(String dynamicImageUrl) {
        this.dynamicImageUrl = dynamicImageUrl;
    }

    /**
     * @return the button text to render.
     */
    public String getText() {
        return text;
    }

    public String getAccessibilityText() {return accessibilityText; }

    protected void setText(String text) {
        this.text = text;
    }

    public boolean isMultiLine() {
        return isMultiLine;
    }

    public void setMultiLine(boolean multiLine) {
        isMultiLine = multiLine;
    }

    public float getFontSize() {
        return fontSize;
    }

    public boolean isScrollable() {
        return isScrollable;
    }

    public TextAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public String getFontFile() {
        return fontFile;
    }

    public String getFontDigest() {
        return fontDigest;
    }

    public SwrveTextViewStyle.FONT_NATIVE_STYLE getFontNativeStyle() {
        return this.fontNativeStyle;
    }

    public double getLineHeight() {
        return lineHeight;
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

    public int getForegroundColor(int defaultForegroundColor) {
        if (SwrveHelper.isNotNullOrEmpty(foregroundColor)) {
            return Color.parseColor(foregroundColor);
        } else {
            return defaultForegroundColor;
        }
    }

    public int getBackgroundColor(int defaultBackgroundColor) {
        if (SwrveHelper.isNotNullOrEmpty(backgroundColor)) {
            return Color.parseColor(backgroundColor);
        } else {
            return defaultBackgroundColor;
        }
    }

    public int getIamZIndex() {
        return iamZIndex;
    }
}
