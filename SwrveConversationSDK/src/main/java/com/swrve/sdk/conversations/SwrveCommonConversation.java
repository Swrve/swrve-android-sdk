package com.swrve.sdk.conversations;

import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.engine.model.ControlBase;
import com.swrve.sdk.conversations.engine.model.ConversationPage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

// This class has to be extended by the SwrveSDK to add campaign and the other commented code
public class SwrveCommonConversation implements Serializable {
    private final String LOG_TAG = "SwrveConversation";
    // Swrve SDK reference
    protected transient ISwrveConversationsSDK conversationController;
    // Identifies the message in a campaign
    protected int id;
    // Customer defined name of the conversation as it appears in the web app
    protected String name;
    // Each of the conversations pages
    protected ArrayList<ConversationPage> pages;
    // Location of the images and button resources
    protected File cacheDir;

    /**
     * Load message from JSON data.
     *
     * @param controller       SwrveTalk object that will manage the data from the campaign.
     * @param conversationData JSON data containing the message details.
     * @throws JSONException
     */
    public SwrveCommonConversation(ISwrveConversationsSDK controller, JSONObject conversationData) throws JSONException {
        //READD ON EXTENDED CLASSthis(controller, campaign);
        setConversationController(controller);

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

    protected void setConversationController(ISwrveConversationsSDK conversationController) {
        this.conversationController = conversationController;
        if (conversationController != null) {
            setCacheDir(conversationController.getCacheDir());
        }
    }

    /*//READD ON EXTENDED CLASS
    protected boolean assetInCache(String asset) {
        Set<String> assetsOnDisk = conversationController.getAssetsOnDisk();
        return !SwrveHelper.isNullOrEmpty(asset) && assetsOnDisk.contains(asset);
    }

    /**
     * @return has the conversation been downloaded fully yet
     * /
    public boolean isDownloaded() {
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
    }*/

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
     * @return the related pages in the conversation.
     */
    public ArrayList<ConversationPage> getPages() {
        return pages;
    }

    public void setPages(ArrayList<ConversationPage> pages) {
        this.pages = pages;
    }
}
