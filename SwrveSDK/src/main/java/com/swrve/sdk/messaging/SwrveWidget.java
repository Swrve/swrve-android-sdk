package com.swrve.sdk.messaging;

import android.graphics.Point;

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
}
