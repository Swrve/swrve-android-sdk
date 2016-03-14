package com.swrve.sdk.conversations;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveBaseConversation;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ControlBase;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.messaging.SwrveConversationCampaign;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

public class SwrveConversation extends SwrveBaseConversation implements Serializable {
    private final String LOG_TAG = "SwrveConversation";
    // SwrveSDK reference
    protected transient SwrveBase<?, ?> swrve;
    // Parent in-app campaign
    protected transient SwrveConversationCampaign campaign;

    /**
     * Load message from JSON data.
     *
     * @param swrve       SwrveTalk object that will manage the data from the campaign.
     * @param campaign         Related campaign.
     * @param conversationData JSON data containing the message details.
     * @throws JSONException
     */
    public SwrveConversation(SwrveBase<?, ?> swrve, SwrveConversationCampaign campaign, JSONObject conversationData) throws JSONException {
        super(conversationData, swrve.getCacheDir());
        this.swrve = swrve;
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

    protected boolean assetInCache(String asset) {
        Set<String> assetsOnDisk = swrve.getAssetsOnDisk();
        return !SwrveHelper.isNullOrEmpty(asset) && assetsOnDisk.contains(asset);
    }

    /**
     * @return has the conversation been downloaded fully yet
     */
    public boolean areAssetsReady() {
        if (this.pages != null) {
            for (ConversationPage conversationPage : pages) {
                for (ConversationAtom conversationAtom : conversationPage.getContent()) {
                    if (ConversationAtom.TYPE_CONTENT_IMAGE.equalsIgnoreCase(conversationAtom.getType().toString())) {
                        Content modelContent = (Content) conversationAtom;
                        if (!this.assetInCache(modelContent.getValue())) {
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
