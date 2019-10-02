package com.swrve.sdk.notifications.model;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.swrve.sdk.SwrveLogger;

import java.util.List;

public class SwrveNotification {

    public static SwrveNotification fromJson(String json) {

        SwrveNotification payload = null;
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
            Gson gson = gsonBuilder.create();
            payload = gson.fromJson(json, SwrveNotification.class);
        } catch (JsonParseException ex) {
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
    private SwrveNotificationMedia media;
    private SwrveNotificationExpanded expanded;
    private List<SwrveNotificationButton> buttons;
    private String channelId;
    private SwrveNotificationChannel channel;
    private SwrveNotificationCampaign campaign;

    public int getVersion() {
        return version;
    }

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

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getAccent() {
        return accent;
    }

    public void setAccent(String accent) {
        this.accent = accent;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public int getNotificationId() {
        return notificationId;
    }


    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public VisibilityType getVisibility() {
        return visibility;
    }

    public void setVisibility(VisibilityType visibility) {
        this.visibility = visibility;
    }

    public String getLockScreenMsg() {
        return lockScreenMsg;
    }

    public void setLockScreenMsg(String lockScreenMsg) {
        this.lockScreenMsg = lockScreenMsg;
    }

    public SwrveNotificationMedia getMedia() {
        return media;
    }

    public void setMedia(SwrveNotificationMedia media) {
        this.media = media;
    }

    public SwrveNotificationExpanded getExpanded() {
        return expanded;
    }

    public void setExpanded(SwrveNotificationExpanded expanded) {
        this.expanded = expanded;
    }

    public List<SwrveNotificationButton> getButtons() {
        return buttons;
    }

    public void setButtons(List<SwrveNotificationButton> buttons) {
        this.buttons = buttons;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public SwrveNotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(SwrveNotificationChannel channel) {
        this.channel = channel;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public SwrveNotificationCampaign getCampaign() {  return campaign; }

    public void setCampaign(SwrveNotificationCampaign campaign) { this.campaign = campaign; }
}
