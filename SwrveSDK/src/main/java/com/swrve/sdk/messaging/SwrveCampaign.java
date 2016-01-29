package com.swrve.sdk.messaging;

import com.swrve.sdk.SwrveLogger;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;

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

/**
 * Swrve campaign containing an in-app message targeted to the current device and user id.
 */
public class SwrveCampaign extends SwrveBaseCampaign {
    // List of messages contained in the campaign
    protected List<SwrveMessage> messages;

    /**
     * Load a campaign from JSON data.
     *
     * @param controller   SwrveTalk object that will manage the data from the campaign.
     * @param campaignData JSON data containing the campaign details.
     * @param assetsQueue  Set where to save the resources to be loaded
     * @throws JSONException
     */
    public SwrveCampaign(SwrveBase<?, ?> controller, JSONObject campaignData, Set<String> assetsQueue) throws JSONException {
        super(controller, campaignData);
        this.messages = new ArrayList<SwrveMessage>();

        if (campaignData.has("messages")) {
            JSONArray jsonMessages = campaignData.getJSONArray("messages");
            for (int k = 0, t = jsonMessages.length(); k < t; k++) {
                JSONObject messageData = jsonMessages.getJSONObject(k);
                SwrveMessage message = createMessage(controller, this, messageData);

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
     * Search for a message with the given trigger event and that satisfies
     * the specific rules for the campaign.
     *
     * @param event trigger event
     * @param now   device time
     * @return SwrveMessage message setup to the given trigger or null
     * otherwise.
     */
    public SwrveMessage getMessageForEvent(String event, Date now) {
        return getMessageForEvent(event, now, null);
    }

    /**
     * Search for a message related to the given trigger event at the given
     * time. This function will return null if too many messages were dismissed,
     * the campaign start is in the future, the campaign end is in the past or
     * the given event is not contained in the trigger set.
     *
     * @param event           trigger event
     * @param now             device time
     * @param campaignReasons will contain the reason the campaign returned no message
     * @return SwrveMessage message setup to the given trigger or null
     * otherwise.
     */
    public SwrveMessage getMessageForEvent(String event, Date now, Map<Integer, String> campaignReasons) {
        if (checkCampaignLimits(event, now, campaignReasons, messages.size(), "message")) {
            SwrveLogger.i(LOG_TAG, event + " matches a trigger in " + id);
            return getNextMessage(campaignReasons);
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

    protected SwrveMessage getNextMessage(Map<Integer, String> campaignReasons) {
        if (randomOrder) {
            List<SwrveMessage> randomMessages = new ArrayList<SwrveMessage>(messages);
            Collections.shuffle(randomMessages);
            Iterator<SwrveMessage> itRandom = randomMessages.iterator();
            while (itRandom.hasNext()) {
                SwrveMessage msg = itRandom.next();
                if (msg.assetsReady()) {
                    return msg;
                }
            }
        } else if (next < messages.size()) {
            SwrveMessage msg = messages.get(next);
            if (msg.assetsReady()) {
                return messages.get(next);
            }
        }

        logAndAddReason(campaignReasons, "Campaign " + this.getId() + " hasn't finished downloading.");
        return null;
    }

    protected SwrveMessage createMessage(SwrveBase<?, ?> controller, SwrveCampaign swrveCampaign, JSONObject messageData) throws JSONException {
        return new SwrveMessage(controller, swrveCampaign, messageData);
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
            setNext(nextMessage);
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

    public boolean assetsReady() {
        Iterator<SwrveMessage> messageIt = messages.iterator();
        while (messageIt.hasNext()) {
            SwrveMessage message = messageIt.next();
            if (!message.assetsReady())
                return false;
        }

        return true;
    }
}
