package com.swrve.sdk.messaging;

import static com.swrve.sdk.QaCampaignInfo.CAMPAIGN_TYPE.IAM;

import com.swrve.sdk.ISwrveCampaignManager;
import com.swrve.sdk.QaCampaignInfo;
import com.swrve.sdk.SwrveAssetsQueueItem;
import com.swrve.sdk.SwrveCampaignDisplayer;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveTextTemplating;
import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Swrve campaign containing an in-app message targeted to the current device and user id.
 */
public class SwrveInAppCampaign extends SwrveBaseCampaign {

    protected SwrveMessage message;
                          
    public SwrveInAppCampaign(ISwrveCampaignManager campaignManager, SwrveCampaignDisplayer campaignDisplayer, JSONObject campaignData, Set<SwrveAssetsQueueItem> assetsQueue, Map<String, String> properties) throws JSONException {
        super(campaignManager, campaignDisplayer, campaignData);

        if (campaignData.has("message")) {
            JSONObject messageData = campaignData.getJSONObject("message");
            message = createMessage(this, messageData, campaignManager.getCacheDir());
            name = message.getName();
            priority = message.getPriority();
            queueAssets(assetsQueue, properties, message.getFormats());
            queueMessageCenterAssets(message.getMessageCenterDetails(), assetsQueue, properties);
        }
    }

    private void queueMessageCenterAssets(SwrveMessageCenterDetails swrveMessageCenterDetails, Set<SwrveAssetsQueueItem> assetsQueue, Map<String, String> properties) {
        if (swrveMessageCenterDetails == null) {
            return;
        }
        if (SwrveHelper.isNotNullOrEmpty(swrveMessageCenterDetails.getImageURL())) {
            try {
                String resolvedUrl = SwrveTextTemplating.apply(swrveMessageCenterDetails.getImageURL(), properties);
                assetsQueue.add(new SwrveAssetsQueueItem(getId(), SwrveHelper.sha1(resolvedUrl.getBytes()), resolvedUrl, true, true));
            } catch (SwrveSDKTextTemplatingException exception) {
                SwrveLogger.w("Campaign id:%s text templating could not be resolved for message center image url. %s", getId(), exception.getMessage());
            }
        }

        if (SwrveHelper.isNotNullOrEmpty(swrveMessageCenterDetails.getImageSha())) {
            assetsQueue.add(new SwrveAssetsQueueItem(getId(), swrveMessageCenterDetails.getImageSha(), swrveMessageCenterDetails.getImageSha(), true, false));
        }
    }

    private void queueAssets(Set<SwrveAssetsQueueItem> assetsQueue, Map<String, String> properties, List<SwrveMessageFormat> formats) {

        if (formats == null || formats.size() == 0 || assetsQueue == null) {
            return; // exit quickly
        }

        for (SwrveMessageFormat format : formats) {

            for (Map.Entry<Long, SwrveMessagePage> entry : format.getPages().entrySet()) {
                SwrveMessagePage page = entry.getValue();

                for (SwrveButton button : page.getButtons()) {
                    if (!SwrveHelper.isNullOrEmpty(button.getImage())) {
                        assetsQueue.add(new SwrveAssetsQueueItem(getId(), button.getImage(), button.getImage(), true, false));
                    }

                    if (!SwrveHelper.isNullOrEmpty(button.getDynamicImageUrl())) {
                        try {
                            String resolvedUrl = SwrveTextTemplating.apply(button.getDynamicImageUrl(), properties);
                            assetsQueue.add(new SwrveAssetsQueueItem(getId(), SwrveHelper.sha1(resolvedUrl.getBytes()), resolvedUrl, true, true));
                        } catch (SwrveSDKTextTemplatingException exception) {
                            SwrveLogger.w("Campaign id:%s text templating could not be resolved for button dynamic image url. %s", getId(), exception.getMessage());
                        }
                    }

                    if (button.getTheme() != null) {
                        SwrveButtonTheme buttonTheme = button.getTheme();
                        if (SwrveHelper.isNotNullOrEmpty(buttonTheme.getBgImage())) {
                            assetsQueue.add(new SwrveAssetsQueueItem(getId(), buttonTheme.getBgImage(), buttonTheme.getBgImage(), true, false));
                        }
                        if (SwrveHelper.isNotNullOrEmpty(buttonTheme.getFontFile()) && !SwrveTextUtils.isSystemFont(buttonTheme.getFontFile())) {
                            assetsQueue.add(new SwrveAssetsQueueItem(getId(), buttonTheme.getFontFile(), buttonTheme.getFontDigest(), false, false));
                        }

                        if (buttonTheme.getPressedState() != null) {
                            SwrveButtonThemeState pressedThemeState = buttonTheme.getPressedState();
                            if (SwrveHelper.isNotNullOrEmpty(pressedThemeState.getBgImage())) {
                                assetsQueue.add(new SwrveAssetsQueueItem(getId(), pressedThemeState.getBgImage(), pressedThemeState.getBgImage(), true, false));
                            }
                        }

                        if (buttonTheme.getFocusedState() != null) {
                            SwrveButtonThemeState focusedThemeState = buttonTheme.getFocusedState();
                            if (SwrveHelper.isNotNullOrEmpty(focusedThemeState.getBgImage())) {
                                assetsQueue.add(new SwrveAssetsQueueItem(getId(), focusedThemeState.getBgImage(), focusedThemeState.getBgImage(), true, false));
                            }
                        }
                    }
                }

                for (SwrveImage image : page.getImages()) {
                    if (!SwrveHelper.isNullOrEmpty(image.getFile())) {
                        assetsQueue.add(new SwrveAssetsQueueItem(getId(), image.getFile(), image.getFile(), true, false));
                    }

                    if (!SwrveHelper.isNullOrEmpty(image.getDynamicImageUrl())) {
                        try {
                            String resolvedUrl = SwrveTextTemplating.apply(image.getDynamicImageUrl(), properties);
                            assetsQueue.add(new SwrveAssetsQueueItem(getId(), SwrveHelper.sha1(resolvedUrl.getBytes()), resolvedUrl, true, true));
                        } catch (SwrveSDKTextTemplatingException exception) {
                            SwrveLogger.w("Campaign id:%s text templating could not be resolved for image dynamic image url. %s", getId(), exception.getMessage());
                        }
                    }

                    if (image.isMultiLine && SwrveHelper.isNotNullOrEmpty(image.getFontFile()) && SwrveHelper.isNotNullOrEmpty(image.getFontDigest())) {
                        if (!SwrveTextUtils.isSystemFont(image.getFontFile())) {
                            assetsQueue.add(new SwrveAssetsQueueItem(getId(), image.getFontFile(), image.getFontDigest(), false, false));
                        }
                    }
                }
            }
        }
    }

    /**
     * @return the campaign messages.
     */
    public SwrveMessage getMessage() {
        return message;
    }

    public int getVariantId() {
        int variantId = -1;
        if (message != null) {
            variantId = message.getId();
        }
        return variantId;
    }

    protected void setMessage(SwrveMessage message) {
        this.message = message;
    }

    /**
     * Search for a message related to the given trigger event at the given
     * time. This function will return null if too many messages were dismissed,
     * the campaign start is in the future, the campaign end is in the past or
     * the given event is not contained in the trigger set.
     *
     * @param event             trigger event
     * @param payload           payload to compare conditions against
     * @param now               device time
     * @param qaCampaignInfoMap will contain the reason the campaign showed or didn't show
     * @return SwrveMessage message setup to the given trigger or null
     * otherwise.
     */
    public SwrveMessage getMessageForEvent(String event, Map<String, String> payload, Date now, Map<Integer, QaCampaignInfo> qaCampaignInfoMap) {
        return getMessageForEvent(event, payload, now, qaCampaignInfoMap, null);
    }

    /**
     * Search for a message related to the given trigger event at the given
     * time. This function will return null if too many messages were dismissed,
     * the campaign start is in the future, the campaign end is in the past or
     * the given event is not contained in the trigger set.
     *
     * @param event             trigger event
     * @param payload           payload to compare conditions against
     * @param now               device time
     * @param qaCampaignInfoMap will contain the reason the campaign showed or didn't show
     * @param properties        personalization properties which can be applied to the getMessageEvent
     * @return SwrveMessage message setup to the given trigger or null
     * otherwise.
     */
    public SwrveMessage getMessageForEvent(String event, Map<String, String> payload, Date now, Map<Integer, QaCampaignInfo> qaCampaignInfoMap, Map<String, String> properties) {
        int messageSize = (message == null) ? 0 : 1;
        boolean canShowCampaign = campaignDisplayer.shouldShowCampaign(this, event, payload, now, qaCampaignInfoMap, messageSize);
        if (canShowCampaign) {
            SwrveLogger.i("%s matches a trigger in %s", event, id);
            return getNextMessage(qaCampaignInfoMap, properties);
        }
        return null;
    }

    /**
     * Search for a message with the given message id.
     *
     * @param messageId message id to look for
     * @return SwrveMessage message with the given id. If not found returns
     * null.
     */
    public SwrveMessage getMessageForId(int messageId) {
        if (message == null) {
            SwrveLogger.i("No messages in campaign %s", id);
            return null;
        }

        if (message.getId() == messageId) {
            return message;
        }

        return null;
    }

    protected SwrveMessage getNextMessage(Map<Integer, QaCampaignInfo> qaCampaignInfoMap, Map<String, String> properties) {
        if (this.message != null && this.message.areAssetsReady(campaignManager.getAssetsOnDisk(), properties)) {
            return this.message;
        }

        String resultText = "Campaign " + this.getId() + " hasn't finished downloading.";
        if (qaCampaignInfoMap != null) {
            int variantId = getVariantId();
            qaCampaignInfoMap.put(id, new QaCampaignInfo(id, variantId, IAM, false, resultText));
        }
        SwrveLogger.i(resultText);

        return null;
    }

    protected SwrveMessage createMessage(SwrveInAppCampaign swrveCampaign, JSONObject messageData, File cacheDir) throws JSONException {
        return new SwrveMessage(swrveCampaign, messageData, cacheDir);
    }

    /**
     * Notify that a message was shown to the user.
     */
    @Override
    public void messageWasHandledOrShownToUser() {
        super.messageWasHandledOrShownToUser();
    }

    @Override
    public boolean supportsOrientation(SwrveOrientation orientation) {
        if (message.supportsOrientation(orientation)) {
            return true;
        }
        return false;
    }

    @Override
    public QaCampaignInfo.CAMPAIGN_TYPE getCampaignType() {
        return QaCampaignInfo.CAMPAIGN_TYPE.IAM;
    }

    /**
     * Notify that a message was dismissed.
     */
    public void messageDismissed() {
        setMessageMinDelayThrottle();
    }

    @Override
    public boolean areAssetsReady(Set<String> assetsOnDisk, Map<String, String> properties) {
        if (!message.areAssetsReady(assetsOnDisk, properties)) {
            return false;
        }
        return true;
    }
}
