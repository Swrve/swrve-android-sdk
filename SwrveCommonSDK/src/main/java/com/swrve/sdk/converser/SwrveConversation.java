package com.swrve.sdk.converser;

import android.util.Log;

import com.google.ciogson.Gson;
import com.swrve.sdk.SwrveBase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import com.swrve.sdk.converser.engine.model.ControlBase;
import com.swrve.sdk.converser.engine.model.ConversationDetail;
import com.swrve.sdk.messaging.SwrveCampaign;

/**
 * Created by shanemoore on 06/01/2015.
 */
public class SwrveConversation {
    private final String LOG_TAG = "SwrveConversation";
    // Swrve SDK reference
    protected SwrveBase<?, ?> conversationController;
    // Identifies the message in a campaign
    protected int id;
    // Customer defined name of the conversation as it appears in the web app
    protected String name;
    // Customer defined name of the conversation as it appears in the web app
    protected String description;
    // The subject of the conversation as it appears in the inbox
    protected String title;
    // The subtitle to the subject
    protected String subtitle;
    // Each of the conversations pages
    protected ArrayList<ConversationDetail> pages;

    // Priority of the message
    protected int priority = 9999;
    // Parent in-app campaign
    protected SwrveCampaign campaign;
    // Location of the images and button resources
    protected File cacheDir;

    public SwrveConversation(SwrveBase<?, ?> conversationController, SwrveCampaign campaign) {
        setCampaign(campaign);
        setConversationController(conversationController);
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
    public SwrveConversation(SwrveBase<?, ?> controller, SwrveCampaign campaign, JSONObject conversationData) throws JSONException {
        this(controller, campaign);

        setId(conversationData.getInt("id"));
        setName(conversationData.getString("name"));
        setDescription(conversationData.getString("description"));

        try{
            setTitle(conversationData.getString("title"));
        }catch(JSONException je){
            Log.w(LOG_TAG, "Could not get title for conversation", je);
            setTitle("");
        }
        try{
            setSubtitle(conversationData.getString("subtitle"));
        }catch(JSONException je){
            Log.w(LOG_TAG, "Could not get subtitle for conversation", je);
            setSubtitle("");
        }


        JSONArray states = conversationData.getJSONArray("states");
        ArrayList<ConversationDetail> pages = new ArrayList<ConversationDetail>();

        for (int i = 0; i < states.length(); i++) {
            JSONObject o = states.getJSONObject(i);
            ConversationDetail page = new ConversationDetail().fromJson(o);
            pages.add(page);
        }
        setPages(pages);
    }

    /**
     * @return Does the conversation support this orientation
     */
    public boolean supportsOrientation(Object o) {
        // TODO: STM Since conversations in the ConverserSDK work in both orientations, this should always return true. Is this OK?
        return true;
    }

    /**
     * @return has the conversation been downloaded fully yet
     */
    public boolean isDownloaded() {
        // TODO: Are conversations always downloaded at this point in time or do they have to go the same route as messages?
        return true;
    }

    /**
     * @return the first ConversationDetail (Page)
     */
    public ConversationDetail getfirstPage(){
        return pages.get(0);
    }

    /**
     * @return the ConversationDetail (Page) for a specific control (Button) which was pressed
     */
    public ConversationDetail getPageForControl(ControlBase control)
    {
        ConversationDetail targetPage = null;
        for (ConversationDetail page : pages){
            if(page.hasName(control.getTarget())){
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
     * @return the message priority.
     */
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
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
    public SwrveCampaign getCampaign() {
        return campaign;
    }

    protected void setCampaign(SwrveCampaign campaign) {
        this.campaign = campaign;
    }

    /**
     * @return the related description.
     */

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the related title.
     */
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the related subtitle.
     */
    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    /**
     * @return the related pages in the conversation.
     */
    public ArrayList<ConversationDetail> getPages() {
        return pages;
    }

    public void setPages(ArrayList<ConversationDetail> pages) {
        this.pages = pages;
    }


    /**
     * @return the SwrveTalk instance that manages the Conversation.
     */
    public SwrveBase<?, ?> getConversationController() {
        return conversationController;
    }

    /**
     * Set the Swrve SDK instance that will respond to the controller.
     *
     * @param conversationController
     */
    public void setConversationController(SwrveBase<?, ?> conversationController) {
        this.conversationController = conversationController;
        if (conversationController != null) {
            setCacheDir(conversationController.getCacheDir());
        }
    }


}
