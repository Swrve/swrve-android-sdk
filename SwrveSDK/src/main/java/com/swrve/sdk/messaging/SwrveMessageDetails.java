package com.swrve.sdk.messaging;

import java.util.List;

/**
 * This class provides meta data associated with in-app message campaigns.
 * See SwrveMessageListener.
 */
public class SwrveMessageDetails {

    final private String campaignSubject;
    final private long campaignId;
    final private long variantId;
    final private String messageName;
    final private List<SwrveMessageButtonDetails> buttons;

    public SwrveMessageDetails(String campaignSubject, long campaignId, long variantId, String messageName, List<SwrveMessageButtonDetails> buttons) {
        this.campaignSubject = campaignSubject;
        this.campaignId = campaignId;
        this.variantId = variantId;
        this.messageName = messageName;
        this.buttons = buttons;
    }
    /**
     * @return the campaign subject.
     */
    public String getCampaignSubject() {
        return campaignSubject;
    }

    /**
     * @return the campaign id.
     */
    public long getCampaignId() {
        return campaignId;
    }

    /**
     * @return the message id.
     */
    public long getVariantId() {
        return variantId;
    }

    /**
     * @return the message name.
     */
    public String getMessageName() {
        return messageName;
    }

    /**
     * @return a list of all buttons showing in the in app message.
     */
    public List<SwrveMessageButtonDetails> getButtons() {
        return buttons;
    }

}

