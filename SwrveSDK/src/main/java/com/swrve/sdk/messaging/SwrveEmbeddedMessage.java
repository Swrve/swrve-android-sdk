package com.swrve.sdk.messaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SwrveEmbeddedMessage implements SwrveBaseMessage {

    public enum EMBEDDED_CAMPAIGN_TYPE {
        OTHER {
            public String toString() {
                return "other";
            }
        },
        JSON {
            public String toString() {
                return "json";
            }
        }
    }

    // Identifies the message in a campaign
    protected int id;
    // Priority of the message
    protected int priority = 9999;
    // Parent in-app campaign
    protected SwrveEmbeddedCampaign campaign;
    // List of buttonNames provided
    protected List<String> buttons;
    // The data provided for the embeddedCampaign
    protected String data;
    // The type of data
    protected EMBEDDED_CAMPAIGN_TYPE type;

    /*
     * Load embedded message from JSON data.
     *
     * @param campaign    Related campaign.
     * @param messageData JSON data containing the message details.
     * @throws JSONException
     */
    public SwrveEmbeddedMessage(SwrveEmbeddedCampaign campaign, JSONObject messageData) throws JSONException {
        setCampaign(campaign);
        setId(messageData.getInt("id"));

        if (messageData.has("priority")) {
            setPriority(messageData.getInt("priority"));
        }

        if (messageData.has("buttons")) {
            JSONArray array = messageData.getJSONArray("buttons");
            List<String> list = new ArrayList<>();
            for(int i = 0; i < array.length(); i++){
                list.add(array.optString(i));
            }
            setButtons(list);
        }

        if (messageData.has("data")) {
            setData(messageData.getString("data"));
        }

        if (messageData.has("type")) {
            setType(messageData.getString("type"));
        }
    }

    /**
     * @return the embedded message id.
     */
    public int getId() {
        return id;
    }

    protected void setId(int id) {
        this.id = id;
    }

    /**
     * @return the message priority.
     */
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * @return the related campaign.
     */
    public SwrveEmbeddedCampaign getCampaign() {
        return campaign;
    }

    @Override
    public boolean supportsOrientation(SwrveOrientation orientation) {
        return true;
    }

    protected void setCampaign(SwrveEmbeddedCampaign campaign) {
        this.campaign = campaign;
    }

    /**
     * @return the names of the expected buttons
     */
    public List<String> getButtons() {
        return buttons;
    }

    protected void setButtons(List<String> buttonNames) {
        this.buttons = buttonNames;
    }

    /**
     * @return the embedded campaign data
     */
    public String getData() {
        return data;
    }

    protected void setData(String data) {
        this.data = data;
    }

    /**
     * @return the type of data in embeddedCampaignData
     */
    public EMBEDDED_CAMPAIGN_TYPE getType() {
        return type;
    }

    protected void setType(String type) {
        if (type.equalsIgnoreCase(EMBEDDED_CAMPAIGN_TYPE.JSON.toString())) {
            this.type = EMBEDDED_CAMPAIGN_TYPE.JSON;
        }

        if (type.equalsIgnoreCase(EMBEDDED_CAMPAIGN_TYPE.OTHER.toString())) {
            this.type = EMBEDDED_CAMPAIGN_TYPE.OTHER;
        }

    }

}
