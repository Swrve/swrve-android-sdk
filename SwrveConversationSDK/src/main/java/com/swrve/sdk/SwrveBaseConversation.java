package com.swrve.sdk;

import com.swrve.sdk.conversations.engine.model.ControlBase;
import com.swrve.sdk.conversations.engine.model.ConversationPage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

// This class has to be extended by the SwrveSDK to add campaign and the other commented code
public class SwrveBaseConversation implements Serializable {
    private final String LOG_TAG = "SwrveConversation";
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
     * @param conversationData JSON data containing the message details.
     * @throws JSONException
     */
    public SwrveBaseConversation(JSONObject conversationData, File cacheDir) throws JSONException {

        this.cacheDir = cacheDir;

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
