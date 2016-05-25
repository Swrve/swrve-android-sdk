package com.swrve.sdk.messaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Base in-app message element widget class.
 */
abstract class SwrveWidget implements Serializable {
    // Position of the widget
    protected SwrvePoint position;
    // Size of the widget
    protected SwrvePoint size;

    // Get size from the format data
    protected static SwrvePoint getSizeFrom(JSONObject data) throws JSONException {
        return new SwrvePoint(data.getJSONObject("w").getInt("value"), data.getJSONObject("h").getInt("value"));
    }

    // Get center from the format data
    protected static SwrvePoint getCenterFrom(JSONObject data) throws JSONException {
        return new SwrvePoint(data.getJSONObject("x").getInt("value"), data.getJSONObject("y").getInt("value"));
    }

    /**
     * @return the position of the widget.
     */
    public SwrvePoint getPosition() {
        return position;
    }

    protected void setPosition(SwrvePoint position) {
        this.position = position;
    }

    /**
     * @return the size of the widget.
     */
    public SwrvePoint getSize() {
        return size;
    }

    protected void setSize(SwrvePoint size) {
        this.size = size;
    }
}
