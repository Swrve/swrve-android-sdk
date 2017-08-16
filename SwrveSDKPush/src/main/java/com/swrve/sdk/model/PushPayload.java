package com.swrve.sdk.model;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.swrve.sdk.SwrveLogger;

import java.io.Serializable;
import java.util.List;

public class PushPayload implements Serializable {

    public static PushPayload fromJson(String json) {

        PushPayload payload = null;
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
            Gson gson = gsonBuilder.create();
            payload = gson.fromJson(json, PushPayload.class);
        }catch (JsonParseException ex) {
            SwrveLogger.e("Could not parse Rich Push json: %s", ex, json);
        }
        return payload;
    }

    public enum VisibilityType {
        @SerializedName("public")
        PUBLIC,
        @SerializedName("private")
        PRIVATE,
        @SerializedName("secret")
        SECRET
    }

    private int version;

    private String title;

    private String subtitle;

    private String ticker;

    private String iconUrl;

    private String accent;

    private int priority;

    private int notificationId;

    private VisibilityType visibility;

    private String lockScreenMsg;

    private PayloadMedia media;

    private PayloadExpanded expanded;

    private List<PayloadButton> buttons;

    private String channelId;

    /** getters **/

    public int getVersion() {
        return version;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getAccent() {
        return accent;
    }

    public int getPriority() {
        return priority;
    }

    public String getTicker() {
        return ticker;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public VisibilityType getVisibility() {
        return visibility;
    }

    public String getLockScreenMsg() {
        return lockScreenMsg;
    }

    public PayloadMedia getMedia() {
        return media;
    }

    public PayloadExpanded getExpanded() {
        return expanded;
    }

    public List<PayloadButton> getButtons() {
        return buttons;
    }

    public String getChannelId() {
        return channelId;
    }
}
