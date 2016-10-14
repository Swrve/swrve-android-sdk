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
    private final String LOG_TAG = "SwrveConversation";

    // Parent in-app campaign
    protected transient SwrveConversationCampaign campaign;

    /**
     * Load message from JSON data.
     *
     * @param campaign         Related campaign.
     * @param conversationData JSON data containing the message details.
     * @param campaignManager
     * @throws JSONException
     */
    public SwrveConversation(SwrveConversationCampaign campaign, JSONObject conversationData, ISwrveCampaignManager campaignManager) throws JSONException {
        super(conversationData, campaignManager.getCacheDir());
        this.campaign = campaign;
    }

    /**
     * @return has the conversation been downloaded fully yet
     */
    public boolean areAssetsReady(Set<String> assetsOnDisk) {
        if (this.pages != null) {
            for (ConversationPage conversationPage : pages) {

                // check font and images from content
                for (ConversationAtom conversationAtom : conversationPage.getContent()) {
                    if (ConversationAtom.TYPE_CONTENT_IMAGE.equalsIgnoreCase(conversationAtom.getType().toString())) {
                        Content modelContent = (Content) conversationAtom;
                        if (!isAssetInCache(assetsOnDisk, modelContent.getValue())) {
                            SwrveLogger.i(LOG_TAG, "Conversation asset not yet downloaded: " + modelContent.getValue());
                            return false;
                        }
                    } else if (ConversationAtom.TYPE_CONTENT_HTML.equalsIgnoreCase(conversationAtom.getType().toString())) {
                        if (!isFontAssetInCache(assetsOnDisk, conversationAtom.getStyle())) {
                            return false;
                        }
                    } else if (ConversationAtom.TYPE_INPUT_STARRATING.equalsIgnoreCase(conversationAtom.getType().toString())) {
                        if (!isFontAssetInCache(assetsOnDisk, conversationAtom.getStyle())) {
                            return false;
                        }
                    } else if (ConversationAtom.TYPE_INPUT_MULTIVALUE.equalsIgnoreCase(conversationAtom.getType().toString())) {
                        MultiValueInput multiValueInput = (MultiValueInput) conversationAtom;
                        ConversationStyle style = multiValueInput.getStyle();
                        if (!isFontAssetInCache(assetsOnDisk, multiValueInput.getStyle())) {
                            return false;
                        }
                        // iterate through options
                        for (ChoiceInputItem item : multiValueInput.getValues()) {
                            if (!isFontAssetInCache(assetsOnDisk, item.getStyle())) {
                                return false;
                            }
                        }
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
        if (style != null && SwrveHelper.isNotNullOrEmpty(style.getFontFile())) {
            if (!isAssetInCache(assetsOnDisk, style.getFontFile())) {
                SwrveLogger.i(LOG_TAG, "Conversation font asset not yet downloaded: " + style.getFontFile());
                return false;
            }
        }
        return true;
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
