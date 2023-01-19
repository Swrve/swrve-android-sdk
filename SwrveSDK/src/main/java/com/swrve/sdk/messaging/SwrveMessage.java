package com.swrve.sdk.messaging;

import static com.swrve.sdk.SwrveAssetsTypes.MIMETYPES;

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

    private final int id;
    private final String name;
    private int priority = 9999;
    private SwrveMessageCenterDetails messageCenterDetails;
    private final SwrveInAppCampaign campaign;
    private final List<SwrveMessageFormat> formats;
    private final File cacheDir;
    private boolean control;

    public SwrveMessage(SwrveInAppCampaign campaign, JSONObject messageData, File cacheDir) throws JSONException {
        this.campaign = campaign;
        this.formats = new ArrayList<>();
        this.cacheDir = cacheDir;
        this.id = messageData.getInt("id");
        this.name = messageData.getString("name");

        if (messageData.has("priority")) {
            this.priority = messageData.getInt("priority");
        }

        if (messageData.has("message_center_details")) {
            this.messageCenterDetails = new SwrveMessageCenterDetails(messageData.getJSONObject("message_center_details"));
        }

        JSONObject template = messageData.getJSONObject("template");
        JSONArray jsonFormats = template.getJSONArray("formats");

        for (int i = 0, j = jsonFormats.length(); i < j; i++) {
            JSONObject messageFormatData = jsonFormats.getJSONObject(i);
            SwrveMessageFormat messageFormat = new SwrveMessageFormat(this, messageFormatData);
            getFormats().add(messageFormat);
        }

        if (messageData.has("control")) {
            this.control = messageData.getBoolean("control");
        }
    }

    public boolean isControl() {
        return control;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public List<SwrveMessageFormat> getFormats() {
        return formats;
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public SwrveInAppCampaign getCampaign() {
        return campaign;
    }

    public SwrveMessageCenterDetails getMessageCenterDetails() {
        return messageCenterDetails;
    }

    /**
     * Search for a format with the given orientation.
     *
     * @param orientation Portrait, Landscape or Both.
     * @return SwrveMessageFormat Message format for the specified orientation.
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
        boolean assetInCache = false;
        if (SwrveHelper.isNotNullOrEmpty(asset) && assetsOnDisk.contains(asset)) {
            assetInCache = true;
        } else if (SwrveHelper.isNotNullOrEmpty(asset)) {
            for (Map.Entry<String, String> entry : MIMETYPES.entrySet()) {
                String assetWithExtension = asset + entry.getValue();
                if (assetsOnDisk.contains(assetWithExtension)) {
                    assetInCache = true;
                    break;
                }
            }
        }
        return assetInCache;
    }

    /**
     * Checks if assets have been downloaded.
     *
     * @param assetsOnDisk Already downloaded assets on disk
     * @param properties   properties are used to resolve the dynamic image urls that may occur
     * @return true if all assets for this message have been downloaded.
     */
    public boolean areAssetsReady(Set<String> assetsOnDisk, Map<String, String> properties) {
        if (this.formats == null) {
            return true;
        }

        for (SwrveMessageFormat format : formats) {

            for (Map.Entry<Long, SwrveMessagePage> entry : format.getPages().entrySet()) {
                SwrveMessagePage page = entry.getValue();

                // check buttons
                for (SwrveButton button : page.getButtons()) {
                    String buttonAsset = button.getImage();
                    boolean hasButtonImage = this.assetInCache(assetsOnDisk, buttonAsset);

                    if (!hasButtonImage && SwrveHelper.isNotNullOrEmpty(button.getDynamicImageUrl())) {
                        try {
                            String resolvedUrl = SwrveTextTemplating.apply(button.getDynamicImageUrl(), properties);
                            if (this.assetInCache(assetsOnDisk, SwrveHelper.sha1(resolvedUrl.getBytes()))) {
                                hasButtonImage = true;
                            } else {
                                SwrveLogger.i("Button dynamic asset not yet downloaded: %s", resolvedUrl);
                                return false;
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

                // check images
                for (SwrveImage image : page.getImages()) {
                    String imageAsset = image.getFile();
                    boolean hasImage = this.assetInCache(assetsOnDisk, imageAsset);

                    if (!hasImage && SwrveHelper.isNotNullOrEmpty(image.getDynamicImageUrl())) {
                        try {
                            String resolvedUrl = SwrveTextTemplating.apply(image.getDynamicImageUrl(), properties);
                            if (this.assetInCache(assetsOnDisk, SwrveHelper.sha1(resolvedUrl.getBytes()))) {
                                hasImage = true;
                            } else {
                                SwrveLogger.i("Image dynamic asset not yet downloaded: %s", resolvedUrl);
                                return false;
                            }
                        } catch (Exception e) {
                            SwrveLogger.i("Could not resolve personalization", e);
                        }
                    }

                    if (!hasImage && !image.isMultiLine()) {
                        SwrveLogger.i("Image asset not yet downloaded: %s", imageAsset);
                        return false;
                    }

                    if (image.isMultiLine()) {
                        String font_file = image.getFontFile();
                        if (SwrveHelper.isNotNullOrEmpty(font_file) && !font_file.equals("_system_font_")) {
                            if (!this.assetInCache(assetsOnDisk, font_file)) {
                                SwrveLogger.i("Font asset not yet downloaded: %s", font_file);
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
