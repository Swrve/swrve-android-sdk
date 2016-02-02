package com.swrve.sdk.messaging;

import com.swrve.sdk.SwrveLogger;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/*
 * Swrve campaign containing a conversation targeted to the current device and user id.
 */
public class SwrveConversationCampaign extends SwrveBaseCampaign implements Serializable {
    // List of conversations contained in the campaign
    protected SwrveConversation conversation;

    /**
     * Load a campaign from JSON data.
     *
     * @param controller   SwrveTalk object that will manage the data from the campaign.
     * @param campaignData JSON data containing the campaign details.
     * @param assetsQueue  Set where to save the resources to be loaded
     * @throws org.json.JSONException
     */
    public SwrveConversationCampaign(SwrveBase<?, ?> controller, JSONObject campaignData, Set<String> assetsQueue) throws JSONException {
        super(controller, campaignData);

        if(campaignData.has("conversation")) {
            JSONObject conversationData = campaignData.getJSONObject("conversation");
            this.conversation = createConversation(controller, this, conversationData);

            // Add assets to queue
            for (ConversationPage conversationPage : conversation.getPages()) {
                for (ConversationAtom conversationAtom : conversationPage.getContent()) {
                    if (ConversationAtom.TYPE_CONTENT_IMAGE.equalsIgnoreCase(conversationAtom.getType().toString())) {
                        Content modelContent = (Content) conversationAtom;
                        assetsQueue.add(modelContent.getValue());
                    }
                }
            }
        }
    }

    /**
     * @return the campaign conversations.
     */
    public SwrveConversation getConversation() {
        return conversation;
    }

    protected void setConversation(SwrveConversation conversation) {
        this.conversation = conversation;
    }

    /**
     * Search for a conversation related to the given trigger event at the given
     * time. This function will return null if too many messages were dismissed,
     * the campaign start is in the future, the campaign end is in the past or
     * the given event is not contained in the trigger set.
     *
     * @param event           trigger event
     * @param now             device time
     * @param campaignReasons will contain the reason the campaign returned no message
     * @return SwrveConversation message setup to the given trigger or null
     * otherwise.
     */
    public SwrveConversation getConversationForEvent(String event, Date now, Map<Integer, String> campaignReasons) {
        if (checkCampaignLimits(event, now, campaignReasons, 1, "conversation") && conversation != null && conversation.areAssetsReady()) {
            SwrveLogger.i(LOG_TAG, event + " matches a trigger in " + id);
            return this.conversation;
        }

        return null;
    }

    protected SwrveConversation createConversation(SwrveBase<?, ?> controller, SwrveConversationCampaign swrveCampaign, JSONObject conversationData) throws JSONException {
        return new SwrveConversation(controller, swrveCampaign, conversationData);
    }

    @Override
    public boolean supportsOrientation(SwrveOrientation orientation) {
        return true;
    }

    public boolean areAssetsReady() {
        return conversation.areAssetsReady();
    }
}
