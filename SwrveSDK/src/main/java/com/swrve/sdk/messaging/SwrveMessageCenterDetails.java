package com.swrve.sdk.messaging;

import android.graphics.Bitmap;

import org.json.JSONException;
import org.json.JSONObject;

public class SwrveMessageCenterDetails {

    private final String subject;
    private final String description;
    private final String imageURL;
    private final String imageAccessibilityText;
    private final String imageSha;
    private final Bitmap image;

    public SwrveMessageCenterDetails(JSONObject messageCenterDetailsData) throws JSONException {
        this.subject = messageCenterDetailsData.isNull("subject") ? null : messageCenterDetailsData.getString("subject");
        this.description = messageCenterDetailsData.isNull("description") ? null : messageCenterDetailsData.getString("description");
        this.imageAccessibilityText = messageCenterDetailsData.isNull("accessibility_text") ? null : messageCenterDetailsData.getString("accessibility_text");
        this.imageSha = messageCenterDetailsData.isNull("image_asset") ? null : messageCenterDetailsData.getString("image_asset");
        this.imageURL = messageCenterDetailsData.isNull("dynamic_image_url") ? null : messageCenterDetailsData.getString("dynamic_image_url");
        this.image = null; // The bitmap is only loaded when needed
    }

    public SwrveMessageCenterDetails(String subject, String description, String imageURL, String imageAccessibilityText, String imageSha, Bitmap bitmap) {
        this.subject = subject;
        this.description = description;
        this.imageURL = imageURL;
        this.imageAccessibilityText = imageAccessibilityText;
        this.imageSha = imageSha;
        this.image = bitmap;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public String getImageURL() {
        return imageURL;
    }

    public String getImageAccessibilityText() {
        return imageAccessibilityText;
    }

    public String getImageSha() {
        return imageSha;
    }

    public Bitmap getImage() {
        return image;
    }
}
