package com.swrve.sdk.messaging;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * In-app message background image.
 */
public class SwrveImage extends SwrveWidget {
    // Cached path of the image file on disk
    protected String file;
    // Personalised text (render this instead of the image)
    protected String text;

    public SwrveImage(JSONObject imageData) throws JSONException {
        setPosition(getCenterFrom(imageData));
        setSize(getSizeFrom(imageData));
        setFile(imageData.getJSONObject("image").getString("value"));

        if (imageData.has("text")) {
            setText(imageData.getJSONObject("text").getString("value"));
        }
    }

    /**
     * @return the cached path of the image file.
     */
    public String getFile() {
        return file;
    }

    protected void setFile(String file) {
        this.file = file;
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
}
