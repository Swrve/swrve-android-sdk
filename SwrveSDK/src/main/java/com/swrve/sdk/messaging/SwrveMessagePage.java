package com.swrve.sdk.messaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * In-app message page
 */
public class SwrveMessagePage {

    private List<SwrveButton> buttons;
    private List<SwrveImage> images;
    private String pageName;
    private long pageId;
    private long swipeForward;
    private long swipeBackward;
    private int pageDuration;   // milliseconds

    public SwrveMessagePage(SwrveMessage message, JSONObject pageData) throws JSONException {

        this.buttons = new ArrayList<>();
        JSONArray jsonButtons = pageData.getJSONArray("buttons");
        int jsonButtonsLength = jsonButtons.length();
        for (int i = 0; i < jsonButtonsLength; i++) {
            SwrveButton button = new SwrveButton(message, jsonButtons.getJSONObject(i));
            buttons.add(button);
        }

        this.images = new ArrayList<>();
        JSONArray jsonImages = pageData.getJSONArray("images");
        int jsonImagesLength = jsonImages.length();
        for (int i = 0; i < jsonImagesLength; i++) {
            SwrveImage image = new SwrveImage(jsonImages.getJSONObject(i));
            images.add(image);
        }

        // for backward compatibility, single page IAM's are saved with schema that conforms to schema
        // that is equal or below inAppVersion 6. Therefore they do not have a page_id, page_name, etc
        if (pageData.has("page_id")) {
            this.pageId = pageData.getLong("page_id");
        }

        if (pageData.has("page_name")) {
            this.pageName = pageData.getString("page_name");
        }

        if (pageData.has("swipe_forward")) {
            this.swipeForward = pageData.getLong("swipe_forward");
        }

        if (pageData.has("swipe_backward")) {
            this.swipeBackward = pageData.getLong("swipe_backward");
        }

        if (pageData.has("page_duration")) {
            this.pageDuration = pageData.getInt("page_duration");
        }
    }

    public List<SwrveButton> getButtons() {
        return buttons;
    }

    public List<SwrveImage> getImages() {
        return images;
    }

    public String getPageName() {
        return pageName;
    }

    public long getPageId() {
        return pageId;
    }

    public long getSwipeForward() {
        return swipeForward;
    }

    public long getSwipeBackward() {
        return swipeBackward;
    }

    public int getPageDuration() {
        return pageDuration;
    }
}
