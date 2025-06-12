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

    protected int id;
    protected int priority = 9999;
    protected SwrveEmbeddedCampaign campaign;
    protected SwrveMessageCenterDetails messageCenterDetails;
    protected List<String> buttons;
    protected String data;
    protected EMBEDDED_CAMPAIGN_TYPE type;
    protected String name;
    protected boolean control;

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
            this.data = messageData.getString("data");
        }

        if (messageData.has("type")) {
            setType(messageData.getString("type"));
        }

        if (messageData.has("name")) {
            this.name = messageData.getString("name");
        }

        if(messageData.has("message_center_details")) {
            this.messageCenterDetails = new SwrveMessageCenterDetails(messageData.getJSONObject("message_center_details"));
        }

        if (messageData.has("control")) {
            this.control = messageData.getBoolean("control");
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

    public int getCampaignId() {
        return campaign.getId();
    }

    public SwrveCampaignState getState() {
        return campaign.getSaveableState();
    }

    public SwrveCampaignState.Status getStatus() {
        return campaign.getStatus();
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
     * @return the raw embedded campaign data without personalization.
     * Use SwrveSDK.getPersonalizedEmbeddedMessageData() to get the personalized data.
     */
    public String getData() {
        return data;
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

    /**
     * @return the embedded campaign name
     */
    public String getName() {
        return name;
    }

    /**
     * @return whether campaign is control or treatment
     */
    public boolean isControl() {
        return control;
    }

}
