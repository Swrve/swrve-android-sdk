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
    // Identifies the message in a campaign
    protected int id;
    // Customer defined name of the conversation as it appears in the web app
    protected String name;
    // Each of the conversations pages
    protected ArrayList<ConversationPage> pages;
    // Location of the images and button resources
    protected File cacheDir;
    // Priority of the conversation
    protected int priority = 9999;

    /*
     * Load message from JSON data.
     */
    public SwrveBaseConversation(JSONObject conversationData, File cacheDir) throws JSONException {
        this.cacheDir = cacheDir;

        try {
            setId(conversationData.getInt("id"));
        } catch (Exception e) {
            try {
                setId(Integer.valueOf(conversationData.getString("id")));
            } catch (Exception c) {
                SwrveLogger.e("Could not cast String into ID");
            }
        }

        setName(conversationData.getString("name"));

        JSONArray pagesJson = conversationData.getJSONArray("pages");
        ArrayList<ConversationPage> pages = new ArrayList<>();

        for (int i = 0; i < pagesJson.length(); i++) {
            JSONObject o = pagesJson.getJSONObject(i);
            pages.add(ConversationPage.fromJson(o));
        }
        setPages(pages);

        if (conversationData.has("priority")) {
            setPriority(conversationData.getInt("priority"));
        }
    }

    /**
     * @return the first ConversationPage (Page)
     */
    public ConversationPage getFirstPage() {
        return pages.get(0);
    }

    /**
     * @param control Control to which to obtain the page from
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

    protected void setPages(ArrayList<ConversationPage> pages) {
        this.pages = pages;
    }

    /**
     * @return the message priority.
     */
    public int getPriority() {
        return priority;
    }

    protected void setPriority(int priority) {
        this.priority = priority;
    }
}
