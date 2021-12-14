package com.swrve.sdk.messaging;

import android.graphics.Color;
import android.graphics.Point;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.messaging.view.SwrveCalibration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * In-app message format with a given language, size and orientation.
 */
public class SwrveMessageFormat {
    // Name of the format
    protected String name;
    // Language for the format
    protected String language;
    // Scale for the format in the device
    protected float scale;
    // Size of the format
    protected Point size;
    // Orientation of the format
    protected SwrveOrientation orientation;
    // Background color of the template
    protected Integer backgroundColor;
    // List of buttons in the format
    protected List<SwrveButton> buttons;
    // List of Background images in the format
    protected List<SwrveImage> images;
    // Parent in-app message
    protected SwrveMessage message;
    protected SwrveCalibration calibration;

    public SwrveMessageFormat(SwrveMessage message, JSONObject messageFormatData) throws JSONException {
        this.message = message;
        this.buttons = new ArrayList<>();
        this.images = new ArrayList<>();
        this.scale = 1f;

        setName(messageFormatData.getString("name"));
        setLanguage(messageFormatData.getString("language"));
        // Orientation
        if (messageFormatData.has("orientation")) {
            setOrientation(SwrveOrientation.parse(messageFormatData.getString("orientation")));
        }

        // Scale
        if (messageFormatData.has("scale")) {
            setScale(Float.parseFloat(messageFormatData.getString("scale")));
        }

        // Background color
        if (messageFormatData.has("color")) {
            String strColor = messageFormatData.getString("color");
            if (!SwrveHelper.isNullOrEmpty(strColor)) {
                setBackgroundColor(Color.parseColor("#" + strColor));
            }
        }

        setSize(getSizeFrom(messageFormatData.getJSONObject("size")));

        SwrveLogger.i("Format name:%s size.x:%s size.y:%s scale:%s", getName(), size.x, size.y, scale);
        JSONArray jsonButtons = messageFormatData.getJSONArray("buttons");
        for (int i = 0, j = jsonButtons.length(); i < j; i++) {
            SwrveButton button = new SwrveButton(message, jsonButtons.getJSONObject(i));
            getButtons().add(button);
        }

        JSONArray jsonImages = messageFormatData.getJSONArray("images");
        for (int ii = 0, ji = jsonImages.length(); ii < ji; ii++) {
            SwrveImage image = new SwrveImage(jsonImages.getJSONObject(ii));
            getImages().add(image);
        }

        if (messageFormatData.has("calibration")) {
            this.calibration = new SwrveCalibration(messageFormatData.getJSONObject("calibration"));
        }
    }

    protected static Point getSizeFrom(JSONObject data) throws JSONException {
        return new Point(data.getJSONObject("w").getInt("value"), data.getJSONObject("h").getInt("value"));
    }

    /**
     * @return the parent message of the format.
     */
    public SwrveMessage getMessage() {
        return message;
    }

    /**
     * @return list of buttons contained in the format.
     */
    public List<SwrveButton> getButtons() {
        return buttons;
    }

    /**
     * @return list of images contained in the format.
     */
    public List<SwrveImage> getImages() {
        return images;
    }

    /**
     * @return the name of the format.
     */
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    /**
     * @return the language of the format.
     */
    public String getLanguage() {
        return language;
    }

    protected void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @return the size of the format.
     */
    public Point getSize() {
        return size;
    }

    protected void setSize(Point size) {
        this.size = size;
    }

    /**
     * @return the orientation of the format.
     */
    public SwrveOrientation getOrientation() {
        return orientation;
    }

    protected void setOrientation(SwrveOrientation orientation) {
        this.orientation = orientation;
    }

    /**
     * @return the scale for the format on the device.
     */
    public float getScale() {
        return scale;
    }

    protected void setScale(float scale) {
        this.scale = scale;
    }

    /**
     * @return the background color of the format.
     */
    public Integer getBackgroundColor() {
        return backgroundColor;
    }

    protected void setBackgroundColor(Integer backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public SwrveCalibration getCalibration() {
        return calibration;
    }
}
