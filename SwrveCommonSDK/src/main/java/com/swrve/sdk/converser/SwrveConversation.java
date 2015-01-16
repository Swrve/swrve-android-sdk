package com.swrve.sdk.converser;

import android.util.Log;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.converser.engine.model.Content;
import com.swrve.sdk.converser.engine.model.ControlBase;
import com.swrve.sdk.converser.engine.model.ConversationAtom;
import com.swrve.sdk.converser.engine.model.ConversationPage;
import com.swrve.sdk.messaging.SwrveCampaign;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

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
    protected ArrayList<ConversationPage> pages;

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

        try{
            setId(conversationData.getInt("id"));
        }catch(Exception e){
            try{
                setId(Integer.valueOf(conversationData.getString("id")));
            }catch(Exception c){
                Log.e(LOG_TAG, "Could not cast String into ID");
            }
        }

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
        ArrayList<ConversationPage> pages = new ArrayList<ConversationPage>();

        for (int i = 0; i < states.length(); i++) {
            JSONObject o = states.getJSONObject(i);
            ConversationPage page = new ConversationPage().fromJson(o);
            pages.add(page);
        }
        setPages(pages);
    }

    /**
     * @return Does the conversation support this orientation
     */
    public boolean supportsOrientation(Object o) {
        // Conversations always support both orientations
        return true;
    }

    protected boolean assetInCache(String asset) {
        Set<String> assetsOnDisk = conversationController.getAssetsOnDisk();
        return SwrveHelper.isNullOrEmpty(asset) || assetsOnDisk.contains(asset);
    }

    /**
     * @return has the conversation been downloaded fully yet
     */
    public boolean isDownloaded() {
        if(this.pages !=null) {
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
    public ConversationPage getfirstPage(){
        return pages.get(0);
    }

    /**
     * @return the ConversationPage (Page) for a specific control (Button) which was pressed
     */
    public ConversationPage getPageForControl(ControlBase control)
    {
        ConversationPage targetPage = null;
        for (ConversationPage page : pages){
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
    public ArrayList<ConversationPage> getPages() {
        return pages;
    }

    public void setPages(ArrayList<ConversationPage> pages) {
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
