package com.swrve.sdk;

import android.graphics.Bitmap;

public class SwrveNotificationDetails {

    private String title;
    private String body;
    private String expandedTitle;
    private String expandedBody;
    private String mediaUrl;
    private Bitmap mediaBitmap;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getExpandedTitle() {
        return expandedTitle;
    }

    public void setExpandedTitle(String expandedTitle) {
        this.expandedTitle = expandedTitle;
    }

    public String getExpandedBody() {
        return expandedBody;
    }

    public void setExpandedBody(String expandedBody) {
        this.expandedBody = expandedBody;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public Bitmap getMediaBitmap() {
        return mediaBitmap;
    }

    public void setMediaBitmap(Bitmap mediaBitmap) {
        this.mediaBitmap = mediaBitmap;
    }
}
