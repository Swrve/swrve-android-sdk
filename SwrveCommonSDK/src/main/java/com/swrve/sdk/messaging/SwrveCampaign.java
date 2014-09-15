package com.swrve.sdk.messaging;

import android.util.Log;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/*
 * Swrve campaign containing messages targeted for the current device and user id.
 */
public class SwrveCampaign {
    protected static final String LOG_TAG = "SwrveMessagingSDK";
    // Default campaign throttle limits
    protected static int DEFAULT_DELAY_FIRST_MESSAGE = 180;
    protected static int DEFAULT_MAX_IMPRESSIONS = 99999;
    protected static int DEFAULT_MIN_DELAY_BETWEEN_MSGS = 60;
    // Random number generator
    protected static Random rnd = new Random();
    protected final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss ZZZZ", Locale.US);
    // Identifies the campaign
    protected int id;
    // SDK controller for this campaign
    protected SwrveBase<?, ?> talkController;
    // Start date of the campaign
    protected Date startDate;
    // End date of the campaign
    protected Date endDate;
    // List of messages contained in the campaign
    protected List<SwrveMessage> messages;
    // List of triggers for the campaign
    protected Set<String> triggers;
    // Indicates if the campaign serves messages randomly or using round robin
    protected boolean randomOrder;
    // Next message to be shown if round robin campaign
    protected int next;
    // Number of impressions of this campaign. Used to disable the campaign if
    // it reaches total impressions
    protected int impressions;
    // Number of maximum impressions of the campaign
    protected int maxImpressions;
    // Minimum delay we want between messages
    protected int minDelayBetweenMessage;
    // Time we can show the first message after launch
    protected Date showMessagesAfterLaunch;
    // Time we can show the next message
    // Will be based on time previous message was shown + minDelayBetweenMessage
    protected Date showMessagesAfterDelay;
    // Amount of seconds to wait for the first message
    protected int delayFirstMessage;

    public SwrveCampaign() {
        this.messages = new ArrayList<SwrveMessage>();
        this.triggers = new HashSet<String>();
    }

    /**
     * Load a campaign from JSON data.
     *
     * @param controller   SwrveTalk object that will manage the data from the campaign.
     * @param campaignData JSON data containing the campaign details.
     * @param assetsQueue  Set where to save the resources to be loaded
     * @return SwrveCampaign Loaded SwrveCampaign.
     * @throws JSONException
     */
    public SwrveCampaign(SwrveBase<?, ?> controller, JSONObject campaignData, Set<String> assetsQueue) throws JSONException {
        this();
        setId(campaignData.getInt("id"));
        setTalkController(controller);
        Log.i(LOG_TAG, "Loading campaign " + getId());

        // Campaign rule defaults
        this.maxImpressions = DEFAULT_MAX_IMPRESSIONS;
        this.minDelayBetweenMessage = DEFAULT_MIN_DELAY_BETWEEN_MSGS;
        this.showMessagesAfterLaunch = SwrveHelper.addTimeInterval(this.talkController.getInitialisedTime(), DEFAULT_DELAY_FIRST_MESSAGE, Calendar.SECOND);

        assignCampaignTriggers(this, campaignData);
        assignCampaignRules(this, campaignData);
        assignCampaignDates(this, campaignData);

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

    /**
     * @return the campaign id.
     */
    public int getId() {
        return id;
    }

    protected void setId(int id) {
        this.id = id;
    }

    protected void setTalkController(SwrveBase<?, ?> controller) {
        this.talkController = controller;
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
     * @return the next message to show.
     */
    public int getNext() {
        return next;
    }

    public void setNext(int next) {
        this.next = next;
    }

    /**
     * @return the set of triggers for this campaign.
     */
    public Set<String> getTriggers() {
        return triggers;
    }

    protected void setTriggers(Set<String> triggers) {
        this.triggers = triggers;
    }

    /**
     * @return if the campaign serves messages in random order and not round
     * robin.
     */
    public boolean isRandomOrder() {
        return randomOrder;
    }

    protected void setRandomOrder(boolean randomOrder) {
        this.randomOrder = randomOrder;
    }

    /**
     * @return current impressions
     */
    public int getImpressions() {
        return impressions;
    }

    public void setImpressions(int impressions) {
        this.impressions = impressions;
    }

    /**
     * @return maximum impressions
     */
    public int getMaxImpressions() {
        return maxImpressions;
    }

    public void setMaxImpressions(int maxImpressions) {
        this.maxImpressions = maxImpressions;
    }

    /**
     * @return the campaign start date.
     */
    public Date getStartDate() {
        return startDate;
    }

    protected void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * @return the campaign end date.
     */
    public Date getEndDate() {
        return endDate;
    }

    protected void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Check if the campaign contains messages for the given event.
     *
     * @param eventName
     * @return true if the campaign has this event as a trigger
     */
    public boolean hasMessageForEvent(String eventName) {
        String lowerCaseEvent = eventName.toLowerCase(Locale.US);
        return triggers != null && triggers.contains(lowerCaseEvent);
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
        int messagesCount = messages.size();

        if (!hasMessageForEvent(event)) {
            Log.i(LOG_TAG, "There is no trigger in " + id + " that matches " + event);
            return null;
        }

        if (messagesCount == 0) {
            logAndAddReason(campaignReasons, "No messages in campaign " + id);
            return null;
        }

        if (startDate.after(now)) {
            logAndAddReason(campaignReasons, "Campaign " + id + " has not started yet");
            return null;
        }

        if (endDate.before(now)) {
            logAndAddReason(campaignReasons, "Campaign " + id + " has finished");
            return null;
        }

        if (impressions >= maxImpressions) {
            logAndAddReason(campaignReasons, "{Campaign throttle limit} Campaign " + id + " has been shown " + maxImpressions + " times already");
            return null;
        }

        // Ignore delay after launch throttle limit for auto show messages
        if (!event.equalsIgnoreCase(talkController.getAutoShowEventTrigger()) && isTooSoonToShowMessageAfterLaunch(now)) {
            logAndAddReason(campaignReasons, "{Campaign throttle limit} Too soon after launch. Wait until " + timestampFormat.format(showMessagesAfterLaunch));
            return null;
        }

        if (isTooSoonToShowMessageAfterDelay(now)) {
            logAndAddReason(campaignReasons, "{Campaign throttle limit} Too soon after last message. Wait until " + timestampFormat.format(showMessagesAfterDelay));
            return null;
        }

        Log.i(LOG_TAG, event + " matches a trigger in " + id);

        return getNextMessage(messagesCount, campaignReasons);
    }

    protected boolean isTooSoonToShowMessageAfterLaunch(Date now) {
        return now.before(showMessagesAfterLaunch);
    }

    protected boolean isTooSoonToShowMessageAfterDelay(Date now) {
        if (showMessagesAfterDelay == null) {
            return false;
        }
        return now.before(showMessagesAfterDelay);
    }

    protected void logAndAddReason(Map<Integer, String> campaignReasons, String reason) {
        if (campaignReasons != null) {
            campaignReasons.put(id, reason);
        }
        Log.i(LOG_TAG, reason);
    }

    /**
     * Amount of seconds to wait for first message.
     *
     * @return time in seconds
     */
    public int getDelayFirstMessage() {
        return delayFirstMessage;
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
            Log.i(LOG_TAG, "No messages in campaign " + id);
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

    protected SwrveMessage getNextMessage(int messagesCount, Map<Integer, String> campaignReasons) {
        if (randomOrder) {
            List<SwrveMessage> randomMessages = new ArrayList<SwrveMessage>(messages);
            Collections.shuffle(randomMessages);
            Iterator<SwrveMessage> itRandom = randomMessages.iterator();
            while (itRandom.hasNext()) {
                SwrveMessage msg = itRandom.next();
                if (msg.isDownloaded()) {
                    return msg;
                }
            }
        } else if (next < messagesCount) {
            SwrveMessage msg = messages.get(next);
            if (msg.isDownloaded()) {
                return messages.get(next);
            }
        }

        logAndAddReason(campaignReasons, "Campaign " + this.getId() + " hasn't finished downloading.");
        return null;
    }

    protected SwrveMessage createMessage(SwrveBase<?, ?> controller, SwrveCampaign swrveCampaign, JSONObject messageData) throws JSONException {
        return new SwrveMessage(controller, swrveCampaign, messageData);
    }

    protected void assignCampaignTriggers(SwrveCampaign campaign, JSONObject campaignData) throws JSONException {
        JSONArray jsonTriggers = campaignData.getJSONArray("triggers");
        for (int i = 0, j = jsonTriggers.length(); i < j; i++) {
            String trigger = jsonTriggers.getString(i);
            campaign.getTriggers().add(trigger.toLowerCase(Locale.US));
        }
    }

    protected void assignCampaignRules(SwrveCampaign campaign, JSONObject campaignData) throws JSONException {
        JSONObject rules = campaignData.getJSONObject("rules");
        campaign.setRandomOrder(rules.getString("display_order").equals("random"));

        if (rules.has("dismiss_after_views")) {
            int totalImpressions = rules.getInt("dismiss_after_views");
            setMaxImpressions(totalImpressions);
        }

        if (rules.has("delay_first_message")) {
            int delay = rules.getInt("delay_first_message");
            this.delayFirstMessage = delay;
            this.showMessagesAfterLaunch = SwrveHelper.addTimeInterval(this.talkController.getInitialisedTime(), this.delayFirstMessage, Calendar.SECOND);
        }

        if (rules.has("min_delay_between_messages")) {
            this.minDelayBetweenMessage = rules.getInt("min_delay_between_messages");
        }
    }

    protected void assignCampaignDates(SwrveCampaign campaign, JSONObject campaignData) throws JSONException {
        campaign.setStartDate(new Date(campaignData.getLong("start_date")));
        campaign.setEndDate(new Date(campaignData.getLong("end_date")));
    }

    /**
     * Increment impressions by one.
     */
    public void incrementImpressions() {
        this.impressions++;
    }

    /**
     * Serialize the campaign state.
     *
     * @return JSONObject Settings for the campaign.
     * @throws JSONException
     */
    public JSONObject createSettings() throws JSONException {
        JSONObject settings = new JSONObject();
        settings.put("next", next);
        settings.put("impressions", impressions);

        return settings;
    }

    /**
     * Load campaign settings from JSON data.
     *
     * @param settings JSONObject containing the campaign settings.
     * @throws JSONException
     */
    public void loadSettings(JSONObject settings) throws JSONException {
        try {
            if (settings.has("next")) {
                this.next = settings.getInt("next");
            }

            if (settings.has("impressions")) {
                this.impressions = settings.getInt("impressions");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while trying to load campaign settings", e);
        }
    }

    /**
     * Ensures a new message cannot be shown until now + minDelayBetweenMessage
     */
    private void setMessageMinDelayThrottle()
    {
        Date now = this.talkController.getNow();
        this.showMessagesAfterDelay = SwrveHelper.addTimeInterval(now, this.minDelayBetweenMessage, Calendar.SECOND);
        this.talkController.setMessageMinDelayThrottle();
    }

    /**
     * Notify that a message was shown to the user.
     *
     * @param messageFormat SwrveMessageFormat shown to the user.
     */
    public void messageWasShownToUser(SwrveMessageFormat messageFormat) {
        incrementImpressions();

        setMessageMinDelayThrottle();

        // Set next message to be shown
        if (!isRandomOrder()) {
            int nextMessage = (getNext() + 1) % getMessages().size();
            setNext(nextMessage);
            Log.i(LOG_TAG, "Round Robin: Next message in campaign " + getId() + " is " + nextMessage);
        } else {
            Log.i(LOG_TAG, "Next message in campaign " + getId() + " is random");
        }
    }

    /**
     * Notify that a message was dismissed.
     */
    public void messageDismissed() {
        setMessageMinDelayThrottle();
    }
}
