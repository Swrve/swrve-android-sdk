package com.swrve.sdk.messaging;

import com.swrve.sdk.ISwrveCampaignManager;
import com.swrve.sdk.QaCampaignInfo;
import com.swrve.sdk.SwrveAssetsQueueItem;
import com.swrve.sdk.SwrveCampaignDisplayer;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.swrve.sdk.QaCampaignInfo.CAMPAIGN_TYPE.IAM;

/**
 * Swrve campaign containing an in-app message targeted to the current device and user id.
 */
public class SwrveInAppCampaign extends SwrveBaseCampaign {

    protected List<SwrveMessage> messages;

    public SwrveInAppCampaign(ISwrveCampaignManager campaignManager, SwrveCampaignDisplayer campaignDisplayer, JSONObject campaignData, Set<SwrveAssetsQueueItem> assetsQueue) throws JSONException {
        super(campaignManager, campaignDisplayer, campaignData);
        this.messages = new ArrayList<>();

        if (campaignData.has("messages")) {
            JSONArray jsonMessages = campaignData.getJSONArray("messages");
            for (int k = 0, t = jsonMessages.length(); k < t; k++) {
                JSONObject messageData = jsonMessages.getJSONObject(k);
                SwrveMessage message = createMessage(this, messageData, campaignManager.getCacheDir());

                // If the message has some format
                List<SwrveMessageFormat> formats = message.getFormats();
                if (formats != null && formats.size() > 0) {
                    // Add assets to queue
                    if (assetsQueue != null) {
                        for (SwrveMessageFormat format : message.getFormats()) {
                            // Add all images to the download queue
                            for (SwrveButton button : format.getButtons()) {
                                if (!SwrveHelper.isNullOrEmpty(button.getImage())) {
                                    assetsQueue.add(new SwrveAssetsQueueItem(button.getImage(), button.getImage(), true));
                                }
                            }

                            for (SwrveImage image : format.getImages()) {
                                if (!SwrveHelper.isNullOrEmpty(image.getFile())) {
                                    assetsQueue.add(new SwrveAssetsQueueItem(image.getFile(), image.getFile(), true));
                                }
                            }
                        }
                    }
                    addMessage(message);
                }
            }
        }
    }

    /**
     * @return the campaign messages.
     */
    public List<SwrveMessage> getMessages() {
        return messages;
    }

    public int getVariantIdAtIndex(int index) {
        int variantId = -1;
        if (messages != null & messages.size() > 0) {
            variantId = messages.get(index).getId();
        }
        return variantId;
    }

    protected void setMessages(List<SwrveMessage> messages) {
        this.messages = messages;
    }

    protected void addMessage(SwrveMessage message) {
        this.messages.add(message);
    }

    /**
     * Search for a message related to the given trigger event at the given
     * time. This function will return null if too many messages were dismissed,
     * the campaign start is in the future, the campaign end is in the past or
     * the given event is not contained in the trigger set.
     *
     * @param event           trigger event
     * @param payload         payload to compare conditions against
     * @param now             device time
     * @param qaCampaignInfoMap will contain the reason the campaign showed or didn't show
     * @return SwrveMessage message setup to the given trigger or null
     * otherwise.
     */
    public SwrveMessage getMessageForEvent(String event, Map<String, String> payload, Date now, Map<Integer, QaCampaignInfo> qaCampaignInfoMap) {
        boolean canShowCampaign = campaignDisplayer.shouldShowCampaign(this, event, payload, now, qaCampaignInfoMap, messages.size());
        if (canShowCampaign) {
            SwrveLogger.i("%s matches a trigger in %s", event, id);
            return getNextMessage(qaCampaignInfoMap);
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
        int messagesCount = messages.size();
        if (messagesCount == 0) {
            SwrveLogger.i("No messages in campaign %s", id);
            return null;
        }

        for (SwrveMessage message : messages) {
            if (message.getId() == messageId) {
                return message;
            }
        }

        return null;
    }

    protected SwrveMessage getNextMessage(Map<Integer, QaCampaignInfo> qaCampaignInfoMap) {
        if (randomOrder) {
            List<SwrveMessage> randomMessages = new ArrayList<>(messages);
            Collections.shuffle(randomMessages);
            for (SwrveMessage msg : randomMessages) {
                if (msg.areAssetsReady(campaignManager.getAssetsOnDisk())) {
                    return msg;
                }
            }
        } else if (saveableState.next < messages.size()) {
            SwrveMessage msg = messages.get(saveableState.next);
            if (msg.areAssetsReady(campaignManager.getAssetsOnDisk())) {
                return messages.get(saveableState.next);
            }
        }

        String resultText = "Campaign " + this.getId() + " hasn't finished downloading.";
        if (qaCampaignInfoMap != null) {
            int variantId = getVariantIdAtIndex(0);
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
    public void messageWasShownToUser() {
        super.messageWasShownToUser();
        // Set next message to be shown
        if (!isRandomOrder()) {
            int nextMessage = (getNext() + 1) % getMessages().size();
            this.saveableState.next = nextMessage;
            SwrveLogger.i("Round Robin: Next message in campaign %s is %s", getId(), nextMessage);
        } else {
            SwrveLogger.i("Next message in campaign %s is random", getId());
        }
    }

    @Override
    public boolean supportsOrientation(SwrveOrientation orientation) {
        for (SwrveMessage message : messages) {
            if (message.supportsOrientation(orientation)) {
                return true;
            }
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
    public boolean areAssetsReady(Set<String> assetsOnDisk) {
        for (SwrveMessage message : messages) {
            if (!message.areAssetsReady(assetsOnDisk)) {
                return false;
            }
        }
        return true;
    }
}
