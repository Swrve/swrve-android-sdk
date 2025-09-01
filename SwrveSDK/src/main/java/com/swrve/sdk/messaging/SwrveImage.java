package com.swrve.sdk.messaging;

import android.net.Uri;

import com.swrve.sdk.SwrveLogger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * In-app message background image.
 */
public class SwrveImage extends SwrveWidget {

    private String file; // Cached path of the image file on disk

    private SwrveVideoSettings swrveVideoSettings;

    private int mediaId;

    public SwrveImage(JSONObject imageData) throws JSONException {
        super(imageData);

        setPosition(getCenterFrom(imageData));
        setSize(getSizeFrom(imageData));

        if (imageData.has("image")) {
            this.file = imageData.getJSONObject("image").getString("value");
        }

        if (imageData.has("video_settings")) {
            this.swrveVideoSettings = new SwrveVideoSettings(imageData.getJSONObject("video_settings"));
        }

        if (imageData.has("media_id")) {
            this.mediaId = imageData.getInt("media_id");
        }
    }

    public String getFile() {
        return file;
    }

    public SwrveVideoSettings getSwrveVideoSettings() {
        return swrveVideoSettings;
    }

    public int getMediaId() {
        return mediaId;
    }
}
