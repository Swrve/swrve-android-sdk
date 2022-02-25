package com.swrve.sdk.messaging;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * In-app message background image.
 */
public class SwrveImage extends SwrveWidget {

    private String file; // Cached path of the image file on disk

    public SwrveImage(JSONObject imageData) throws JSONException {
        super(imageData);

        setPosition(getCenterFrom(imageData));
        setSize(getSizeFrom(imageData));

        if (imageData.has("image")) {
            this.file = imageData.getJSONObject("image").getString("value");
        }
    }

    public String getFile() {
        return file;
    }
}
