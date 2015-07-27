package com.swrve.sdk.messaging;

import android.util.Log;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Im-app message inside a campaign, with different formats.
 */
public class SwrveMessage {
    protected static final String LOG_TAG = "SwrveSDK";

    // Swrve SDK reference
    protected SwrveBase<?, ?> messageController;
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

    public SwrveMessage(SwrveBase<?, ?> messageController, SwrveCampaign campaign) {
        this.campaign = campaign;
        this.formats = new ArrayList<SwrveMessageFormat>();
        setMessageController(messageController);
    }

    /**
     * Load message from JSON data.
     *
     * @param controller  SwrveTalk object that will manage the data from the campaign.
     * @param campaign    Related campaign.
     * @param messageData JSON data containing the message details.
     * @return SwrveMessage
     * Loaded SwrveMessage.
     * @throws JSONException
     */
    public SwrveMessage(SwrveBase<?, ?> controller, SwrveCampaign campaign, JSONObject messageData) throws JSONException {
        this(controller, campaign);
        setId(messageData.getInt("id"));
        setName(messageData.getString("name"));

        if (messageData.has("priority")) {
            setPriority(messageData.getInt("priority"));
        }

        JSONObject template = messageData.getJSONObject("template");
        JSONArray jsonFormats = template.getJSONArray("formats");

        for (int i = 0, j = jsonFormats.length(); i < j; i++) {
            JSONObject messageFormatData = jsonFormats.getJSONObject(i);
            SwrveMessageFormat messageFormat = createMessageFormat(controller, this, messageFormatData);
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

    protected SwrveMessageFormat createMessageFormat(SwrveBase<?, ?> controller, SwrveMessage swrveMessage, JSONObject messageFormatData) throws JSONException {
        return new SwrveMessageFormat(controller, swrveMessage, messageFormatData);
    }

    /**
     * @return the SwrveTalk instance that manages the message.
     */
    public SwrveBase<?, ?> getMessageController() {
        return messageController;
    }

    /**
     * Set the Swrve SDK instance that will respond to the message.
     *
     * @param messageController
     */
    public void setMessageController(SwrveBase<?, ?> messageController) {
        this.messageController = messageController;
        if (messageController != null) {
            setCacheDir(messageController.getCacheDir());
        }
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

    protected boolean assetInCache(String asset) {
        Set<String> assetsOnDisk = messageController.getAssetsOnDisk();
        return SwrveHelper.isNullOrEmpty(asset) || assetsOnDisk.contains(asset);
    }

    /**
     * @return true if all assets for this message have been downloaded.
     */
    public boolean isDownloaded() {
        if (this.formats != null) {
            Iterator<SwrveMessageFormat> itFormats = formats.iterator();
            while (itFormats.hasNext()) {
                SwrveMessageFormat format = itFormats.next();

                Iterator<SwrveButton> itButtons = format.buttons.iterator();
                while (itButtons.hasNext()) {
                    String buttonAsset = itButtons.next().getImage();
                    if (!this.assetInCache(buttonAsset)) {
                        Log.i(LOG_TAG, "Button asset not yet downloaded: " + buttonAsset);
                        return false;
                    }
                }

                Iterator<SwrveImage> itImages = format.images.iterator();
                while (itImages.hasNext()) {
                    String imageAsset = itImages.next().getFile();
                    if (!this.assetInCache(imageAsset)) {
                        Log.i(LOG_TAG, "Image asset not yet downloaded: " + imageAsset);
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
