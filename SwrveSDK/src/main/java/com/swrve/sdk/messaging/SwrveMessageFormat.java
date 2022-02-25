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

    protected String name;
    protected float scale;
    protected Point size;
    protected SwrveOrientation orientation;
    protected Integer backgroundColor;
    protected List<SwrveButton> buttons;
    protected List<SwrveImage> images;
    protected SwrveMessage message;
    protected SwrveCalibration calibration;

    public SwrveMessageFormat(SwrveMessage message, JSONObject messageFormatData) throws JSONException {
        this.message = message;
        this.scale = 1f;
        this.name = messageFormatData.getString("name");
        JSONObject sizeData = messageFormatData.getJSONObject("size");
        this.size = new Point(sizeData.getJSONObject("w").getInt("value"), sizeData.getJSONObject("h").getInt("value"));
        SwrveLogger.i("Format name:%s size.x:%s size.y:%s scale:%s", name, size.x, size.y, scale);

        if (messageFormatData.has("orientation")) {
            this.orientation = SwrveOrientation.parse(messageFormatData.getString("orientation"));
        }

        if (messageFormatData.has("scale")) {
            this.scale = Float.parseFloat(messageFormatData.getString("scale"));
        }

        if (messageFormatData.has("color")) { // Background color
            String strColor = messageFormatData.getString("color");
            if (!SwrveHelper.isNullOrEmpty(strColor)) {
                this.backgroundColor = Color.parseColor("#" + strColor);
            }
        }

        this.buttons = new ArrayList<>();
        JSONArray jsonButtons = messageFormatData.getJSONArray("buttons");
        for (int i = 0, j = jsonButtons.length(); i < j; i++) {
            SwrveButton button = new SwrveButton(message, jsonButtons.getJSONObject(i));
            buttons.add(button);
        }

        this.images = new ArrayList<>();
        JSONArray jsonImages = messageFormatData.getJSONArray("images");
        for (int ii = 0, ji = jsonImages.length(); ii < ji; ii++) {
            SwrveImage image = new SwrveImage(jsonImages.getJSONObject(ii));
            images.add(image);
        }

        if (messageFormatData.has("calibration")) {
            this.calibration = new SwrveCalibration(messageFormatData.getJSONObject("calibration"));
        }
    }

    public SwrveMessage getMessage() {
        return message;
    }

    public List<SwrveButton> getButtons() {
        return buttons;
    }

    public List<SwrveImage> getImages() {
        return images;
    }

    public Point getSize() {
        return size;
    }

    public SwrveOrientation getOrientation() {
        return orientation;
    }

    public float getScale() {
        return scale;
    }

    public Integer getBackgroundColor() {
        return backgroundColor;
    }

    public SwrveCalibration getCalibration() {
        return calibration;
    }
}
