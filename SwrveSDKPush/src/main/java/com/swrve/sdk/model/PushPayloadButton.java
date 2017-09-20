package com.swrve.sdk.model;

import com.google.gson.annotations.SerializedName;

public class PushPayloadButton {

    public enum ActionType {
        @SerializedName("open_url")
        OPEN_URL,
        @SerializedName("open_app")
        OPEN_APP,
        @SerializedName("dismiss")
        DISMISS
    }

    private String title;

    private ActionType actionType;

    private String action;

    /** getters **/

    public String getTitle() {
        return title;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getAction() {
        return action;
    }
}
