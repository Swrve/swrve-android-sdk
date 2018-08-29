package com.swrve.sdk.notifications.model;

import com.google.gson.annotations.SerializedName;

public class SwrveNotificationButton {

    public enum ActionType {
        @SerializedName("open_url")
        OPEN_URL,
        @SerializedName("open_app")
        OPEN_APP,
        @SerializedName("open_campaign")
        OPEN_CAMPAIGN,
        @SerializedName("dismiss")
        DISMISS
    }

    private String title;
    private ActionType actionType;
    private String action;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
