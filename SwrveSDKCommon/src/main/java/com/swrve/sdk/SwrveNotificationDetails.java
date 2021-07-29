package com.swrve.sdk;

import android.graphics.Bitmap;

/**
 * A POJO describing the basic details about a Swrve notification. A subset of all details.
 */
public class SwrveNotificationDetails {

    private String title;
    private String body;
    private String expandedTitle;
    private String expandedBody;
    private String mediaUrl;
    private Bitmap mediaBitmap;

    /**
     * Get the notification title
     *
     * @return The notification title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the notification title
     *
     * @param title The notification title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get the body text of the notification
     *
     * @return The body text of the notification
     */
    public String getBody() {
        return body;
    }

    /**
     * Set the body text of the notification
     *
     * @param body The body text
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Get the notification title when expanded
     *
     * @return The notification title
     */
    public String getExpandedTitle() {
        return expandedTitle;
    }

    /**
     * Set the notification title when expanded
     *
     * @param expandedTitle The expanded title
     */
    public void setExpandedTitle(String expandedTitle) {
        this.expandedTitle = expandedTitle;
    }

    /**
     * Get the expanded body text
     *
     * @return The expanded body text
     */
    public String getExpandedBody() {
        return expandedBody;
    }

    /**
     * Set the expanded body text
     *
     * @param expandedBody The expanded body text
     */
    public void setExpandedBody(String expandedBody) {
        this.expandedBody = expandedBody;
    }

    /**
     * Get the media url (image url displayed in the notification)
     *
     * @return The media url
     */
    public String getMediaUrl() {
        return mediaUrl;
    }

    /**
     * Set the media url (image url displayed in the notification)
     *
     * @param mediaUrl The media url
     */
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    /**
     * Get the media bitmap (image displayed in the notification)
     *
     * @return The media bitmap
     */
    public Bitmap getMediaBitmap() {
        return mediaBitmap;
    }

    /**
     * Set the media bitmap (image displayed in the notification)
     *
     * @param mediaBitmap The media bitmap.
     */
    public void setMediaBitmap(Bitmap mediaBitmap) {
        this.mediaBitmap = mediaBitmap;
    }
}
