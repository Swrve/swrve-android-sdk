package com.swrve.sdk.messaging;

import com.swrve.sdk.ISwrveCampaignManager;
import com.swrve.sdk.SwrveCampaignDisplayer;
import com.swrve.sdk.SwrveCampaignDisplayer.Result;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.swrve.sdk.SwrveCampaignDisplayer.DisplayResult.CAMPAIGN_NOT_DOWNLOADED;

/**
 * Swrve campaign containing an in-app message targeted to the current device and user id.
 */
public class SwrveCampaign extends SwrveBaseCampaign {

    protected List<SwrveMessage> messages;

    /**
     * Load a campaign from JSON data.
     *
     * @param campaignManager
     * @param campaignDisplayer
     * @param campaignData JSON data containing the campaign details.
     * @param assetsQueue  Set where to save the resources to be loaded
     * @throws JSONException
     */
    public SwrveCampaign(ISwrveCampaignManager campaignManager, SwrveCampaignDisplayer campaignDisplayer, JSONObject campaignData, Set<String> assetsQueue) throws JSONException {
        super(campaignManager, campaignDisplayer, campaignData);
        this.messages = new ArrayList<SwrveMessage>();

        if (campaignData.has("messages")) {
            JSONArray jsonMessages = campaignData.getJSONArray("messages");
            for (int k = 0, t = jsonMessages.length(); k < t; k++) {
                JSONObject messageData = jsonMessages.getJSONObject(k);
                SwrveMessage message = createMessage(campaignManager, this, messageData);

                // If the message has some format
                if (message.getFormats().size() > 0) {
                    // Add assets to queue
                    if (assetsQueue != null) {
                        for (SwrveMessageFormat format : message.getFormats()) {
                            // Add all images to the download queue
                            for (SwrveButton button : format.getButtons()) {
                                if (!SwrveHelper.isNullOrEmpty(button.getImage())) {
                                    assetsQueue.add(button.getImage());
                                }
                            }

                            for (SwrveImage image : format.getImages()) {
                                if (!SwrveHelper.isNullOrEmpty(image.getFile())) {
                                    assetsQueue.add(image.getFile());
                                }
                            }
                        }
                    }
                }

                // Only add message if it has any format
                if (message.getFormats().size() > 0) {
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
     * @param campaignDisplayResult will contain the reason the campaign returned no message
     * @return SwrveMessage message setup to the given trigger or null
     * otherwise.
     */
    public SwrveMessage getMessageForEvent(String event, Map<String, String> payload, Date now, Map<Integer, Result> campaignDisplayResult) {
        boolean canShowCampaign = campaignDisplayer.shouldShowCampaign(this, event, payload, now, campaignDisplayResult, messages.size());
        if (canShowCampaign) {
            SwrveLogger.i(LOG_TAG, event + " matches a trigger in " + id);
            return getNextMessage(campaignDisplayResult);
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
            SwrveLogger.i(LOG_TAG, "No messages in campaign " + id);
            return null;
        }

        Iterator<SwrveMessage> messageIt = messages.iterator();
        while (messageIt.hasNext()) {
            SwrveMessage message = messageIt.next();
            if (message.getId() == messageId)
                return message;
        }

        return null;
    }

    protected SwrveMessage getNextMessage(Map<Integer, Result> campaignDisplayResult) {
        if (randomOrder) {
            List<SwrveMessage> randomMessages = new ArrayList<SwrveMessage>(messages);
            Collections.shuffle(randomMessages);
            Iterator<SwrveMessage> itRandom = randomMessages.iterator();
            while (itRandom.hasNext()) {
                SwrveMessage msg = itRandom.next();
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
        if (campaignDisplayResult != null) {
            campaignDisplayResult.put(id, campaignDisplayer.buildResult(CAMPAIGN_NOT_DOWNLOADED, resultText));
        }
        SwrveLogger.i(LOG_TAG, resultText);

        return null;
    }

    protected SwrveMessage createMessage(ISwrveCampaignManager campaignManager, SwrveCampaign swrveCampaign, JSONObject messageData) throws JSONException {
        return new SwrveMessage(swrveCampaign, messageData, campaignManager);
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
            SwrveLogger.i(LOG_TAG, "Round Robin: Next message in campaign " + getId() + " is " + nextMessage);
        } else {
            SwrveLogger.i(LOG_TAG, "Next message in campaign " + getId() + " is random");
        }
    }
    @Override
    public boolean supportsOrientation(SwrveOrientation orientation) {
        Iterator<SwrveMessage> messageIt = messages.iterator();
        while (messageIt.hasNext()) {
            SwrveMessage message = messageIt.next();
            if (message.supportsOrientation(orientation))
                return true;
        }

        return false;
    }

    /**
     * Notify that a message was dismissed.
     */
    public void messageDismissed() {
        setMessageMinDelayThrottle();
    }

    @Override
    public boolean areAssetsReady(Set<String> assetsOnDisk) {
        Iterator<SwrveMessage> messageIt = messages.iterator();
        while (messageIt.hasNext()) {
            SwrveMessage message = messageIt.next();
            if (!message.areAssetsReady(assetsOnDisk))
                return false;
        }

        return true;
    }
}
