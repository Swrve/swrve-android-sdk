package com.swrve.sdk.messaging;

import com.swrve.sdk.ISwrveCampaignManager;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * In-app message inside a campaign, with different formats.
 */
public class SwrveMessage {
    protected static final String LOG_TAG = "SwrveSDK";

    protected ISwrveCampaignManager campaignManager;
    // Identifies the message in a campaign
    protected int id;
    // Name of the message
    protected String name;
    // Priority of the message
    protected int priority = 9999;
    // Parent in-app campaign
    protected SwrveCampaign campaign;
    // List of available formats
    protected List<SwrveMessageFormat> formats;
    // Location of the images and button resources
    protected File cacheDir;

    public SwrveMessage(SwrveCampaign campaign, ISwrveCampaignManager campaignManager) {
        this.campaign = campaign;
        this.formats = new ArrayList<SwrveMessageFormat>();
        this.campaignManager = campaignManager;
        if (campaignManager != null) {
            setCacheDir(campaignManager.getCacheDir());
        }
    }

    /**
     * Load message from JSON data.
     *
     * @param campaign    Related campaign.
     * @param messageData JSON data containing the message details.
     * @param campaignManager
     * @throws JSONException
     */
    public SwrveMessage(SwrveCampaign campaign, JSONObject messageData, ISwrveCampaignManager campaignManager) throws JSONException {
        this(campaign, campaignManager);
        setId(messageData.getInt("id"));
        setName(messageData.getString("name"));

        if (messageData.has("priority")) {
            setPriority(messageData.getInt("priority"));
        }

        JSONObject template = messageData.getJSONObject("template");
        JSONArray jsonFormats = template.getJSONArray("formats");

        for (int i = 0, j = jsonFormats.length(); i < j; i++) {
            JSONObject messageFormatData = jsonFormats.getJSONObject(i);
            SwrveMessageFormat messageFormat = createMessageFormat(this, messageFormatData);
            getFormats().add(messageFormat);
        }
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
     * @return list of message formats for this device.
     */
    public List<SwrveMessageFormat> getFormats() {
        return formats;
    }

    protected void setFormats(List<SwrveMessageFormat> formats) {
        this.formats = formats;
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

    private SwrveMessageFormat createMessageFormat(SwrveMessage swrveMessage, JSONObject messageFormatData) throws JSONException {
        int defaultBackgroundColor = campaignManager.getConfig().getDefaultBackgroundColor();
        return new SwrveMessageFormat(swrveMessage, messageFormatData, defaultBackgroundColor);
    }

    public ISwrveCampaignManager getCampaignManager() {
        return campaignManager;
    }

    /**
     * Search for a format with the given orientation.
     *
     * @param orientation Portrait, Landscape or Both.
     * @return SwrveMessageFormat
     * Message format for the specified orientation.
     */
    public SwrveMessageFormat getFormat(SwrveOrientation orientation) {
        if (formats != null) {
            // Get given orientation
            Iterator<SwrveMessageFormat> itFormats = formats.iterator();
            while (itFormats.hasNext()) {
                SwrveMessageFormat proposedFormat = itFormats.next();
                if (proposedFormat.getOrientation() == orientation) {
                    return proposedFormat;
                }
            }
        }

        return null;
    }

    /**
     * Returns true if the message has a format for the given orientation.
     *
     * @param orientation
     * @return true if the message supports the given orientation.
     */
    public boolean supportsOrientation(SwrveOrientation orientation) {
        if (orientation == SwrveOrientation.Both) {
            return true;
        }
        return (getFormat(orientation) != null);
    }

    protected boolean assetInCache(Set<String> assetsOnDisk, String asset) {
        return SwrveHelper.isNullOrEmpty(asset) || assetsOnDisk.contains(asset);
    }

    /**
     * @return true if all assets for this message have been downloaded.
     */
    public boolean areAssetsReady(Set<String> assetsOnDisk) {
        if (this.formats != null) {
            Iterator<SwrveMessageFormat> itFormats = formats.iterator();
            while (itFormats.hasNext()) {
                SwrveMessageFormat format = itFormats.next();

                Iterator<SwrveButton> itButtons = format.buttons.iterator();
                while (itButtons.hasNext()) {
                    String buttonAsset = itButtons.next().getImage();
                    if (!this.assetInCache(assetsOnDisk, buttonAsset)) {
                        SwrveLogger.i(LOG_TAG, "Button asset not yet downloaded: " + buttonAsset);
                        return false;
                    }
                }

                Iterator<SwrveImage> itImages = format.images.iterator();
                while (itImages.hasNext()) {
                    String imageAsset = itImages.next().getFile();
                    if (!this.assetInCache(assetsOnDisk, imageAsset)) {
                        SwrveLogger.i(LOG_TAG, "Image asset not yet downloaded: " + imageAsset);
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
