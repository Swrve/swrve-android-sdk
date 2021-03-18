package com.swrve.sdk.messaging;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveTextTemplating;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-app message inside a campaign, with different formats.
 */
public class SwrveMessage implements SwrveBaseMessage {
    // Identifies the message in a campaign
    protected int id;
    // Name of the message
    protected String name;
    // Priority of the message
    protected int priority = 9999;
    // Parent in-app campaign
    protected SwrveInAppCampaign campaign;
    // List of available formats
    protected List<SwrveMessageFormat> formats;
    // Location of the images and button resources
    protected File cacheDir;

    public SwrveMessage(SwrveInAppCampaign campaign, File cacheDir) {
        this.campaign = campaign;
        this.formats = new ArrayList<>();
        setCacheDir(cacheDir);
    }

    /*
     * Load message from JSON data.
     *
     * @param campaign    Related campaign.
     * @param messageData JSON data containing the message details.
     * @param cacheDir    Folder where to find the downloaded assets
     * @throws JSONException
     */
    public SwrveMessage(SwrveInAppCampaign campaign, JSONObject messageData, File cacheDir) throws JSONException {
        this(campaign, cacheDir);
        setId(messageData.getInt("id"));
        setName(messageData.getString("name"));

        if (messageData.has("priority")) {
            setPriority(messageData.getInt("priority"));
        }

        JSONObject template = messageData.getJSONObject("template");
        JSONArray jsonFormats = template.getJSONArray("formats");

        for (int i = 0, j = jsonFormats.length(); i < j; i++) {
            JSONObject messageFormatData = jsonFormats.getJSONObject(i);
            SwrveMessageFormat messageFormat = new SwrveMessageFormat(this, messageFormatData);
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
    public SwrveInAppCampaign getCampaign() {
        return campaign;
    }

    protected void setCampaign(SwrveInAppCampaign campaign) {
        this.campaign = campaign;
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
            for (SwrveMessageFormat proposedFormat : formats) {
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
     * @param orientation A SwrveOrientation
     * @return true if the message supports the given orientation.
     */
    public boolean supportsOrientation(SwrveOrientation orientation) {
        if (orientation == SwrveOrientation.Both) {
            return true;
        }
        return (getFormat(orientation) != null);
    }

    protected boolean assetInCache(Set<String> assetsOnDisk, String asset) {
        return SwrveHelper.isNotNullOrEmpty(asset) && assetsOnDisk.contains(asset);
    }

    /**
     * @param assetsOnDisk Already downloaded assets on disk
     * @return true if all assets for this message have been downloaded.
     */
    @Deprecated
    public boolean areAssetsReady(Set<String> assetsOnDisk) {
        return areAssetsReady(assetsOnDisk, null);
    }

    /**
     * @param assetsOnDisk Already downloaded assets on disk
     * @param properties   properties, when applied are used to resolve the dynamic image urls that may occur
     * @return true if all assets for this message have been downloaded.
     */
    public boolean areAssetsReady(Set<String> assetsOnDisk, Map<String, String> properties) {
        if (this.formats != null) {
            for (SwrveMessageFormat format : formats) {
                for (SwrveButton button : format.buttons) {
                    String buttonAsset = button.getImage();
                    boolean hasButtonImage = this.assetInCache(assetsOnDisk, buttonAsset);

                    if (SwrveHelper.isNotNullOrEmpty(button.getDynamicImageUrl())) {
                        try {
                            String resolvedUrl = SwrveTextTemplating.apply(button.getDynamicImageUrl(), properties);
                            if (this.assetInCache(assetsOnDisk, SwrveHelper.sha1(resolvedUrl.getBytes()))) {
                                hasButtonImage = true;
                            }
                        } catch (Exception e) {
                            SwrveLogger.i("Could not resolve personalization", e);
                        }
                    }

                    if (!hasButtonImage) {
                        SwrveLogger.i("Button asset not yet downloaded: %s", buttonAsset);
                        return false;
                    }
                }

                for (SwrveImage image : format.images) {
                    String imageAsset = image.getFile();
                    boolean hasImage = this.assetInCache(assetsOnDisk, imageAsset);

                    if (SwrveHelper.isNotNullOrEmpty(image.getDynamicImageUrl())) {
                        try {
                            String resolvedUrl = SwrveTextTemplating.apply(image.getDynamicImageUrl(), properties);
                            if (this.assetInCache(assetsOnDisk, SwrveHelper.sha1(resolvedUrl.getBytes()))) {
                                hasImage = true;
                            }
                        } catch (Exception e) {
                            SwrveLogger.i("Could not resolve personalization", e);
                        }
                    }

                    if (!hasImage) {
                        SwrveLogger.i("Button asset not yet downloaded: %s", hasImage);
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
