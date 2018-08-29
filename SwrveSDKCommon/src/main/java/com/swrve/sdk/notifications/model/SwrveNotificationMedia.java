package com.swrve.sdk.notifications.model;

import com.google.gson.annotations.SerializedName;

public class SwrveNotificationMedia {

    public enum MediaType {
        @SerializedName("image")
        IMAGE
    }

    private String title;
    private String subtitle;
    private String body;
    private MediaType type;
    private String url;
    private MediaType fallbackType;
    private String fallbackUrl;
    private String fallbackSd;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public MediaType getFallbackType() {
        return fallbackType;
    }

    public void setFallbackType(MediaType fallbackType) {
        this.fallbackType = fallbackType;
    }

    public void setFallbackUrl(String fallbackUrl) {
        this.fallbackUrl = fallbackUrl;
    }

    public String getFallbackUrl() {
        return fallbackUrl;
    }

    public String getFallbackSd() {
        return fallbackSd;
    }

    public void setFallbackSd(String fallbackSd) {
        this.fallbackSd = fallbackSd;
    }
}
