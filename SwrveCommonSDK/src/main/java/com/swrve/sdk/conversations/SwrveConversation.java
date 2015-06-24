package com.swrve.sdk.conversations;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.common.R;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ControlBase;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.messaging.SwrveConversationCampaign;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

public class SwrveConversation implements Serializable {
    private final String LOG_TAG = "SwrveConversation";
    // Swrve SDK reference
    protected transient SwrveBase<?, ?> conversationController;
    // Identifies the message in a campaign
    protected int id;
    // Customer defined name of the conversation as it appears in the web app
    protected String name;
    // Each of the conversations pages
    protected ArrayList<ConversationPage> pages;
    // Parent in-app campaign
    protected transient SwrveConversationCampaign campaign;
    // Location of the images and button resources
    protected File cacheDir;

    public SwrveConversation(SwrveBase<?, ?> controller, SwrveConversationCampaign campaign) {
        setCampaign(campaign);
        setConversationController(controller);
    }

    /**
     * Load message from JSON data.
     *
     * @param controller       SwrveTalk object that will manage the data from the campaign.
     * @param campaign         Related campaign.
     * @param conversationData JSON data containing the message details.
     * @return SwrveConversation
     * Loaded SwrveConversation.
     * @throws JSONException
     */
    public SwrveConversation(SwrveBase<?, ?> controller, SwrveConversationCampaign campaign, JSONObject conversationData) throws JSONException {
        this(controller, campaign);

        try {
            setId(conversationData.getInt("id"));
        } catch (Exception e) {
            try {
                setId(Integer.valueOf(conversationData.getString("id")));
            } catch (Exception c) {
                Log.e(LOG_TAG, "Could not cast String into ID");
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

    private void setConversationController(SwrveBase<?, ?> conversationController) {
        this.conversationController = conversationController;
        if (conversationController != null) {
            setCacheDir(conversationController.getCacheDir());
        }
    }

    protected boolean assetInCache(String asset) {
        Set<String> assetsOnDisk = conversationController.getAssetsOnDisk();
        return !SwrveHelper.isNullOrEmpty(asset) && assetsOnDisk.contains(asset);
    }

    /**
     * @return has the conversation been downloaded fully yet
     */
    public boolean isDownloaded() {
        if (this.pages != null) {
            for (ConversationPage conversationPage : pages) {
                for (ConversationAtom conversationAtom : conversationPage.getContent()) {
                    if (ConversationAtom.TYPE_CONTENT_IMAGE.equalsIgnoreCase(conversationAtom.getType().toString())) {
                        Content modelContent = (Content) conversationAtom;
                        if (!this.assetInCache(modelContent.getValue())) {
                            Log.i(LOG_TAG, "Conversation asset not yet downloaded: " + modelContent.getValue());
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
     * @return the message id.
     */
    public int getId() {
        return id;
    }

    protected void setId(int id) {
        this.id = id;
    }

    /**
     * @return the message name.
     */
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    /**
     * @return the directory where resources will be saved.
     */
    public File getCacheDir() {
        return cacheDir;
    }

    protected void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * @return the related campaign.
     */
    public SwrveConversationCampaign getCampaign() {
        return campaign;
    }

    protected void setCampaign(SwrveConversationCampaign campaign) {
        this.campaign = campaign;
    }

    /**
     * @return the related pages in the conversation.
     */
    public ArrayList<ConversationPage> getPages() {
        return pages;
    }

    public void setPages(ArrayList<ConversationPage> pages) {
        this.pages = pages;
    }
}
