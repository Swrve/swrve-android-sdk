package com.swrve.sdk.messaging;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * In-app message button.
 */
public class SwrveButton extends SwrveWidget {
    // Name of this button
    protected String name;
    // Cached path of the button image on disk
    protected String image;
    // Custom action string for the button
    protected String action;
    // Message identifier associated with this button
    protected SwrveMessage message;
    // ID of target installation app
    protected int appId;
    // Action associated with this button
    protected SwrveActionType actionType;
    // Personalized text (render this instead of the image)
    protected String text;
    // Dynamic Image Url
    protected String dynamicImageUrl;

    public SwrveButton() {
    }

    public SwrveButton(SwrveMessage message, JSONObject buttonData) throws JSONException {
        if (buttonData.has("name")) {
            setName(buttonData.getString("name"));
        }
        setPosition(getCenterFrom(buttonData));
        setSize(getSizeFrom(buttonData));

        if (buttonData.has("image_up")) {
            setImage(buttonData.getJSONObject("image_up").getString("value"));
        }

        if (buttonData.has("dynamic_image_url")) {
            setDynamicImageUrl(buttonData.getString("dynamic_image_url"));
        }

        setMessage(message);
        if (buttonData.has("game_id")) {
            String appIdStr = buttonData.getJSONObject("game_id").getString("value");
            if (appIdStr != null && !appIdStr.equals("")) {
                int appId = Integer.parseInt(appIdStr);
                setAppId(appId);
            }
        }
        setAction(buttonData.getJSONObject("action").getString("value"));
        setActionType(SwrveActionType.parse(buttonData.getJSONObject("type").getString("value")));

        if (buttonData.has("text")) {
            setText(buttonData.getJSONObject("text").getString("value"));
        }

    }

    /**
     * @return the name of the button.
     */
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    /**
     * @return the cached path of the button image.
     */
    public String getImage() {
        return image;
    }

    protected void setImage(String image) {
        this.image = image;
    }

    /**
     * @return the cached path of the button dynamic image url.
     */
    public String getDynamicImageUrl() {
        return dynamicImageUrl;
    }

    protected void setDynamicImageUrl(String dynamicImageUrl) {
        this.dynamicImageUrl = dynamicImageUrl;
    }

    /**
     * @return the button custom action.
     */
    public String getAction() {
        return action;
    }

    protected void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the parent message.
     */
    public SwrveMessage getMessage() {
        return message;
    }

    protected void setMessage(SwrveMessage message) {
        this.message = message;
    }

    /**
     * @return the app id to install.
     */
    public int getAppId() {
        return appId;
    }

    protected void setAppId(int appId) {
        this.appId = appId;
    }

    /**
     * @return the button action type.
     */
    public SwrveActionType getActionType() {
        return actionType;
    }

    protected void setActionType(SwrveActionType actionType) {
        this.actionType = actionType;
    }

    /**
     * @return the button text to render.
     */
    public String getText() {
        return text;
    }

    protected void setText(String text) {
        this.text = text;
    }
}
