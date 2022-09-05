package com.swrve.sdk.messaging;

import com.swrve.sdk.ISwrveCampaignManager;
import com.swrve.sdk.QaCampaignInfo;
import com.swrve.sdk.QaCampaignInfo.CAMPAIGN_TYPE;
import com.swrve.sdk.SwrveAssetsQueueItem;
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

    public SwrveConversationCampaign(ISwrveCampaignManager campaignManager, SwrveCampaignDisplayer campaignDisplayer, JSONObject campaignData, Set<SwrveAssetsQueueItem> assetsQueue) throws JSONException {
        super(campaignManager, campaignDisplayer, campaignData);

        if (campaignData.has("conversation")) {
            JSONObject conversationData = campaignData.getJSONObject("conversation");
            this.conversation = createConversation(this, conversationData, campaignManager);
            this.priority = conversation.getPriority();

            for (ConversationPage conversationPage : conversation.getPages()) {

                // Add image and font assets to queue from content
                for (ConversationAtom conversationAtom : conversationPage.getContent()) {
                    switch (conversationAtom.getType()) {
                        case CONTENT_IMAGE:
                            queueImageAsset(assetsQueue, (Content) conversationAtom);
                            break;
                        case CONTENT_HTML:
                        case INPUT_STARRATING:
                            queueFontAsset(assetsQueue, conversationAtom.getStyle());
                            break;
                        case INPUT_MULTIVALUE:
                            MultiValueInput multiValueInput = (MultiValueInput) conversationAtom;
                            queueFontAsset(assetsQueue, multiValueInput.getStyle());
                            // iterate through options
                            for (ChoiceInputItem item : multiValueInput.getValues()) {
                                queueFontAsset(assetsQueue, item.getStyle());
                            }
                            break;
                    }
                }

                // Add font assets to queue from button control
                for (ButtonControl buttonControl : conversationPage.getControls()) {
                    queueFontAsset(assetsQueue, buttonControl.getStyle());
                }
            }
        }
    }

    private void queueImageAsset(Set<SwrveAssetsQueueItem> assetQueue, Content content) {
        assetQueue.add(new SwrveAssetsQueueItem(getId(), content.getValue(), content.getValue(), true, false));
    }

    private void queueFontAsset(Set<SwrveAssetsQueueItem> assetQueue, ConversationStyle style) {
        if (style != null && SwrveHelper.isNotNullOrEmpty(style.getFontFile()) && SwrveHelper.isNotNullOrEmpty(style.getFontDigest()) && !style.isSystemFont()) {
            assetQueue.add(new SwrveAssetsQueueItem(getId(), style.getFontFile(), style.getFontDigest(), false, false));
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
     * @param event             trigger event
     * @param payload           payload to compare conditions against
     * @param now               device time
     * @param qaCampaignInfoMap will contain the reason the campaign showed or didn't show
     * @return SwrveConversation message setup to the given trigger or null otherwise.
     */
    public SwrveConversation getConversationForEvent(String event, Map<String, String> payload, Date now, Map<Integer, QaCampaignInfo> qaCampaignInfoMap) {
        boolean shouldShowCampaign = campaignDisplayer.shouldShowCampaign(this, event, payload, now, qaCampaignInfoMap, 1);
        boolean canShowCampaign = shouldShowCampaign && conversation != null && conversation.areAssetsReady(campaignManager.getAssetsOnDisk());
        if (canShowCampaign) {
            SwrveLogger.i("%s matches a trigger in %s", event, id);
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
    public CAMPAIGN_TYPE getCampaignType() {
        return CAMPAIGN_TYPE.CONVERSATION;
    }

    @Override
    public boolean areAssetsReady(Set<String> assetsOnDisk, Map<String, String> properties) {
        return conversation.areAssetsReady(assetsOnDisk);
    }
}
