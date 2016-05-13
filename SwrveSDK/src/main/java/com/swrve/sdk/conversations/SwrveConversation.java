package com.swrve.sdk.conversations;

import com.swrve.sdk.SwrveBaseConversation;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ControlBase;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.ISwrveCampaignManager;
import com.swrve.sdk.messaging.SwrveConversationCampaign;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

public class SwrveConversation extends SwrveBaseConversation implements Serializable {
    private final String LOG_TAG = "SwrveConversation";
    protected transient SwrveConversationCampaign campaign; // Parent in-app campaign

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

        try {
            setId(conversationData.getInt("id"));
        } catch (Exception e) {
            try {
                setId(Integer.valueOf(conversationData.getString("id")));
            } catch (Exception c) {
                SwrveLogger.e(LOG_TAG, "Could not cast String into ID");
            }
        }

        setName(conversationData.getString("id"));

        JSONArray pagesJson = conversationData.getJSONArray("pages");
        ArrayList<ConversationPage> pages = new ArrayList<ConversationPage>();

        for (int i = 0; i < pagesJson.length(); i++) {
            JSONObject o = pagesJson.getJSONObject(i);
            pages.add(ConversationPage.fromJson(o));
        }
        setPages(pages);
    }

    protected boolean assetInCache(Set<String> assetsOnDisk, String asset) {
        return !SwrveHelper.isNullOrEmpty(asset) && assetsOnDisk.contains(asset);
    }

    /**
     * @return has the conversation been downloaded fully yet
     */
    public boolean areAssetsReady(Set<String> assetsOnDisk) {
        if (this.pages != null) {
            for (ConversationPage conversationPage : pages) {
                for (ConversationAtom conversationAtom : conversationPage.getContent()) {
                    if (ConversationAtom.TYPE_CONTENT_IMAGE.equalsIgnoreCase(conversationAtom.getType().toString())) {
                        Content modelContent = (Content) conversationAtom;
                        if (!this.assetInCache(assetsOnDisk, modelContent.getValue())) {
                            SwrveLogger.i(LOG_TAG, "Conversation asset not yet downloaded: " + modelContent.getValue());
                            return false;
                        }
                    }
                }
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
