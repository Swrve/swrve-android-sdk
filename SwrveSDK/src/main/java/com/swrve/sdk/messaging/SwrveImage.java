package com.swrve.sdk.messaging;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * In-app message background image.
 */
public class SwrveImage extends SwrveWidget {
    // Cached path of the image file on disk
    protected String file;

    public SwrveImage(JSONObject imageData) throws JSONException {
        setPosition(getCenterFrom(imageData));
        setSize(getSizeFrom(imageData));
        setFile(imageData.getJSONObject("image").getString("value"));
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
}
