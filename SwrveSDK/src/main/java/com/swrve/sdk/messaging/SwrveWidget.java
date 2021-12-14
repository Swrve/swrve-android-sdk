package com.swrve.sdk.messaging;

import android.graphics.Point;

import com.swrve.sdk.messaging.view.SwrveTextViewStyle.TextAlignment;

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
    // if true, text value will have the below items applied to it
    protected boolean isMultiLine;
    // Font Size (used primarily for multi-line)
    protected float fontSize;
    // If it's text, is it scrollable? (used primarily for multi-line)
    protected boolean isScrollable;
    // What is the alignment of the text?
    protected TextAlignment horizontalAlignment;

    public SwrveWidget() {
    }

    public SwrveWidget(JSONObject data) throws JSONException {
        if (data.has("dynamic_image_url")) {
            setDynamicImageUrl(data.getString("dynamic_image_url"));
        }

        if (data.has("text")) {
            setText(data.getJSONObject("text").getString("value"));
        }

        if (data.has("multiline_text")) {
            setMultiLine(true);
            JSONObject multiLineData = data.getJSONObject("multiline_text");
            setText(multiLineData.getString("value"));
            Double font_size = multiLineData.getDouble("font_size");
            setFontSize(font_size.floatValue());
            setScrollable(multiLineData.getBoolean("scrollable"));
            setHorizontalAlignment(TextAlignment.parse(multiLineData.getString("h_align")));
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

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public boolean isScrollable() {
        return isScrollable;
    }

    public void setScrollable(boolean scrollable) {
        isScrollable = scrollable;
    }

    public TextAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(TextAlignment horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
    }

}
