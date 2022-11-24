package com.swrve.sdk.messaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * In-app message button.
 */
public class SwrveButton extends SwrveWidget {

    private String name;
    private long buttonId;
    private String image; // Cached path of the button image on disk
    private String action; // Custom action string for the button
    private SwrveMessage message;
    private int appId;
    private SwrveActionType actionType;
    private String accessibilityText; //Alternative text for use with accessibility voice over
    private JSONArray events;
    private JSONArray userUpdates;

    public SwrveButton(SwrveMessage message, JSONObject buttonData) throws JSONException {
        super(buttonData);

        if (buttonData.has("name")) {
            this.name = buttonData.getString("name");
        }

        if (buttonData.has("button_id")) {
            this.buttonId = buttonData.getLong("button_id");
        }

        setPosition(getCenterFrom(buttonData));
        setSize(getSizeFrom(buttonData));

        if (buttonData.has("image_up")) {
            this.image = buttonData.getJSONObject("image_up").getString("value");
        }

        this.message = message;

        if (buttonData.has("game_id")) {
            String appIdStr = buttonData.getJSONObject("game_id").getString("value");
            if (appIdStr != null && !appIdStr.equals("")) {
                int appId = Integer.parseInt(appIdStr);
                this.appId = appId;
            }
        }

        if(buttonData.has("accessibility_text")) {
            this.accessibilityText = buttonData.getString("accessibility_text");
        }

        this.action = buttonData.getJSONObject("action").getString("value");
        this.actionType = SwrveActionType.parse(buttonData.getJSONObject("type").getString("value"));

        if (buttonData.has("events")) {
            this.events = buttonData.getJSONArray("events");
        }

        if (buttonData.has("user_updates")) {
            this.userUpdates = buttonData.getJSONArray("user_updates");
        }
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getAccessibilityText() {
        return accessibilityText;
    }

    public String getAction() {
        return action;
    }

    public SwrveMessage getMessage() {
        return message;
    }

    public int getAppId() {
        return appId;
    }

    public SwrveActionType getActionType() {
        return actionType;
    }

    public long getButtonId() {
        return buttonId;
    }

    public JSONArray getEvents() {
        return events;
    }

    public JSONArray getUserUpdates() {
        return userUpdates;
    }
}
