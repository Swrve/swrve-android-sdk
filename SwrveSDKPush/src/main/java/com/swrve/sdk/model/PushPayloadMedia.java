package com.swrve.sdk.model;

import com.google.gson.annotations.SerializedName;

public class PushPayloadMedia {

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


    /** getters **/

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getBody() {
        return body;
    }

    public MediaType getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public MediaType getFallbackType() {
        return fallbackType;
    }

    public String getFallbackUrl() {
        return fallbackUrl;
    }

    public String getFallbackSd() {
        return fallbackSd;
    }
}
