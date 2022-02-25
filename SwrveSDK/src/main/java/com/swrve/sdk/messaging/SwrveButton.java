package com.swrve.sdk.messaging;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * In-app message button.
 */
public class SwrveButton extends SwrveWidget {

    private String name;
    private String image; // Cached path of the button image on disk
    private String action; // Custom action string for the button
    private SwrveMessage message;
    private int appId;
    private SwrveActionType actionType;

    public SwrveButton(SwrveMessage message, JSONObject buttonData) throws JSONException {
        super(buttonData);

        if (buttonData.has("name")) {
            this.name = buttonData.getString("name");
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

        this.action = buttonData.getJSONObject("action").getString("value");
        this.actionType = SwrveActionType.parse(buttonData.getJSONObject("type").getString("value"));
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
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

}
