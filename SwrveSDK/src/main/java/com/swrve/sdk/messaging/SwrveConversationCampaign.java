package com.swrve.sdk.messaging;

import com.swrve.sdk.ISwrveCampaignManager;
import com.swrve.sdk.SwrveCampaignDisplayer;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.engine.model.ButtonControl;
import com.swrve.sdk.conversations.engine.model.ChoiceInputItem;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.MultiValueInput;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

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

    protected SwrveConversation conversation;

    /**
     * Load a campaign from JSON data.
     */
    public SwrveConversationCampaign(ISwrveCampaignManager campaignManager, SwrveCampaignDisplayer campaignDisplayer, JSONObject campaignData,
                                     Set<String> assetImageQueue, Set<String> assetFontQueue) throws JSONException {
        super(campaignManager, campaignDisplayer, campaignData);

        if(campaignData.has("conversation")) {
            JSONObject conversationData = campaignData.getJSONObject("conversation");
            this.conversation = createConversation(this, conversationData, campaignManager);

            for (ConversationPage conversationPage : conversation.getPages()) {

                // Add image and font assets to queue from content
                for (ConversationAtom conversationAtom : conversationPage.getContent()) {
                    switch (conversationAtom.getType()) {
                        case CONTENT_IMAGE:
                            queueImageAsset(assetImageQueue, (Content) conversationAtom);
                            break;
                        case CONTENT_HTML:
                        case INPUT_STARRATING:
                            queueFontAsset(assetFontQueue, conversationAtom.getStyle());
                            break;
                        case INPUT_MULTIVALUE:
                            MultiValueInput multiValueInput = (MultiValueInput) conversationAtom;
                            queueFontAsset(assetFontQueue, multiValueInput.getStyle());
                            // iterate through options
                            for (ChoiceInputItem item : multiValueInput.getValues()) {
                                queueFontAsset(assetFontQueue, item.getStyle());
                            }
                            break;
                    }
                }

                // Add font assets to queue from button control
                for (ButtonControl buttonControl : conversationPage.getControls()) {
                    queueFontAsset(assetFontQueue, buttonControl.getStyle());
                }
            }
        }
    }

    private void queueImageAsset(Set<String> assetQueue, Content content) {
        assetQueue.add(content.getValue());
    }

    private void queueFontAsset(Set<String> assetQueue, ConversationStyle style) {
        if (style != null && SwrveHelper.isNotNullOrEmpty(style.getFontFile())) {
            assetQueue.add(style.getFontFile());
        }
    }


    public SwrveConversation getConversation() {
        return conversation;
    }

    /**
     * Search for a conversation related to the given trigger event at the given
     * time. This function will return null if too many messages were dismissed,
     * the campaign start is in the future, the campaign end is in the past or
     * the given event is not contained in the trigger set.
     *
     * @param event           trigger event
     * @param payload         payload to compare conditions against
     * @param now             device time
     * @param campaignDisplayResult will contain the reason the campaign returned no message
     * @return SwrveConversation message setup to the given trigger or null otherwise.
     */
    public SwrveConversation getConversationForEvent(String event, Map<String, String> payload, Date now, Map<Integer, SwrveCampaignDisplayer.Result> campaignDisplayResult) {
        boolean shouldShowCampaign = campaignDisplayer.shouldShowCampaign(this, event, payload, now, campaignDisplayResult, 1);
        boolean canShowCampaign = shouldShowCampaign && conversation != null && conversation.areAssetsReady(campaignManager.getAssetsOnDisk());
        if (canShowCampaign) {
            SwrveLogger.i(LOG_TAG, event + " matches a trigger in " + id);
            return this.conversation;
        }

        return null;
    }

    protected SwrveConversation createConversation(SwrveConversationCampaign swrveCampaign, JSONObject conversationData, ISwrveCampaignManager campaignManager) throws JSONException {
        return new SwrveConversation(swrveCampaign, conversationData, campaignManager);
    }

    @Override
    public boolean supportsOrientation(SwrveOrientation orientation) {
        return true;
    }

    @Override
    public boolean areAssetsReady(Set<String> assetsOnDisk) {
        return conversation.areAssetsReady(assetsOnDisk);
    }
}
