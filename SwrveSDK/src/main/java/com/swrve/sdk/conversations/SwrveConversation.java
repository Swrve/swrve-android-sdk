package com.swrve.sdk.conversations;

import com.swrve.sdk.SwrveBaseConversation;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.engine.model.ButtonControl;
import com.swrve.sdk.conversations.engine.model.ChoiceInputItem;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ControlBase;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.ISwrveCampaignManager;
import com.swrve.sdk.conversations.engine.model.MultiValueInput;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;
import com.swrve.sdk.messaging.SwrveConversationCampaign;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Set;

public class SwrveConversation extends SwrveBaseConversation implements Serializable {

    // Parent in-app campaign
    protected transient SwrveConversationCampaign campaign;

    public SwrveConversation(SwrveConversationCampaign campaign, JSONObject conversationData, ISwrveCampaignManager campaignManager) throws JSONException {
        super(conversationData, campaignManager.getCacheDir());
        this.campaign = campaign;
    }

    /*
     * @return has the conversation been downloaded fully yet
     */
    public boolean areAssetsReady(Set<String> assetsOnDisk) {
        if (this.pages != null) {
            for (ConversationPage conversationPage : pages) {

                // check font and images from content
                for (ConversationAtom conversationAtom : conversationPage.getContent()) {
                    switch (conversationAtom.getType()) {
                        case CONTENT_IMAGE:
                            Content modelContent = (Content) conversationAtom;
                            if (!isAssetInCache(assetsOnDisk, modelContent.getValue())) {
                                SwrveLogger.i("Conversation asset not yet downloaded: %s", modelContent.getValue());
                                return false;
                            }
                            break;
                        case CONTENT_HTML:
                        case INPUT_STARRATING:
                            if (!isFontAssetInCache(assetsOnDisk, conversationAtom.getStyle())) {
                                return false;
                            }
                            break;
                        case INPUT_MULTIVALUE:
                            MultiValueInput multiValueInput = (MultiValueInput) conversationAtom;
                            if (!isFontAssetInCache(assetsOnDisk, multiValueInput.getStyle())) {
                                return false;
                            }
                            // iterate through options
                            for (ChoiceInputItem item : multiValueInput.getValues()) {
                                if (!isFontAssetInCache(assetsOnDisk, item.getStyle())) {
                                    return false;
                                }
                            }
                            break;
                        case UNKNOWN:
                            break;
                    }
                }

                // check font asset in button
                for (ButtonControl buttonControl : conversationPage.getControls()) {
                    if (!isFontAssetInCache(assetsOnDisk, buttonControl.getStyle())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isAssetInCache(Set<String> assetsOnDisk, String asset) {
        return !SwrveHelper.isNullOrEmpty(asset) && assetsOnDisk.contains(asset);
    }

    private boolean isFontAssetInCache(Set<String> assetsOnDisk, ConversationStyle style) {
        boolean isFontAssetReady = true;
        if(style != null && !style.isSystemFont() && SwrveHelper.isNotNullOrEmpty(style.getFontFile())) {
            if (!isAssetInCache(assetsOnDisk, style.getFontFile())) {
                SwrveLogger.i("Conversation font asset not yet downloaded: %s", style.getFontFile());
                isFontAssetReady = false;
            }
        }
        return isFontAssetReady;
    }

    /**
     * @return the first ConversationPage (Page)
     */
    public ConversationPage getFirstPage() {
        return pages.get(0);
    }

    /**
     * @return the ConversationPage (Page) for a specific control (Button) which was pressed
     */
    public ConversationPage getPageForControl(ControlBase control) {
        ConversationPage targetPage = null;
        for (ConversationPage page : pages) {
            if (page.hasTag(control.getTarget())) {
                targetPage = page;
                break;
            }
        }
        return targetPage;
    }

    /**
     * @return the related campaign.
     */
    public SwrveConversationCampaign getCampaign() {
        return campaign;
    }
}
