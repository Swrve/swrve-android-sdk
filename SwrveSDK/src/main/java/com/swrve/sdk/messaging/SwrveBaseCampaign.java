package com.swrve.sdk.messaging;

import com.swrve.sdk.ISwrveCampaignManager;
import com.swrve.sdk.QaCampaignInfo.CAMPAIGN_TYPE;
import com.swrve.sdk.SwrveCampaignDisplayer;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.messaging.model.Trigger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Swrve campaign containing messages targeted for the current device and user id.
 */
public abstract class SwrveBaseCampaign {

    // Default campaign throttle limits
    protected static int DEFAULT_DELAY_FIRST_MESSAGE = 180;
    protected static int DEFAULT_MAX_IMPRESSIONS = 99999;
    protected static int DEFAULT_MIN_DELAY_BETWEEN_MSGS = 60;

    protected ISwrveCampaignManager campaignManager;
    protected SwrveCampaignDisplayer campaignDisplayer;
    protected int id;
    protected SwrveCampaignState saveableState; // The state of the campaign that will be kept saved by the SDK
    protected Date startDate;
    protected Date endDate;
    protected List<Trigger> triggers;
    protected boolean messageCenter;
    protected String subject; // MessageCenter subject of the campaign
    protected int maxImpressions;
    protected int minDelayBetweenMessage;
    protected Date showMessagesAfterLaunch; // Time we can show the first message after launch

    /*
     * Parse a campaign from JSON data.
     */
    public SwrveBaseCampaign(ISwrveCampaignManager campaignManager, SwrveCampaignDisplayer campaignDisplayer, JSONObject campaignData) throws JSONException {
        this.campaignManager = campaignManager;
        this.campaignDisplayer = campaignDisplayer;

        this.id = campaignData.getInt("id");
        SwrveLogger.i("Parsing campaign %s", id);

        this.messageCenter = campaignData.optBoolean("message_center", false);
        this.subject = campaignData.isNull("subject") ? "" : campaignData.getString("subject");
        this.saveableState = new SwrveCampaignState(); // Start with an empty state

        // Campaign rule defaults
        this.maxImpressions = DEFAULT_MAX_IMPRESSIONS;
        this.minDelayBetweenMessage = DEFAULT_MIN_DELAY_BETWEEN_MSGS;
        this.showMessagesAfterLaunch = SwrveHelper.addTimeInterval(campaignManager.getInitialisedTime(), DEFAULT_DELAY_FIRST_MESSAGE, Calendar.SECOND);


        // Parse campaign triggers
        if (campaignData.has("triggers")) {
            String triggersJson = campaignData.getString("triggers");
            triggers = Trigger.fromJson(triggersJson, id);
        } else {
            triggers = new ArrayList<>();
        }
        // Parse campaign rules
        if (campaignData.has("rules")) {

            JSONObject rules = campaignData.getJSONObject("rules");
            if (rules.has("dismiss_after_views")) {
                int totalImpressions = rules.getInt("dismiss_after_views");
                this.maxImpressions = totalImpressions;
            }
            if (rules.has("delay_first_message")) {
                int delayFirstMessage = rules.getInt("delay_first_message");
                this.showMessagesAfterLaunch = SwrveHelper.addTimeInterval(campaignManager.getInitialisedTime(), delayFirstMessage, Calendar.SECOND);
            }
            if (rules.has("min_delay_between_messages")) {
                this.minDelayBetweenMessage = rules.getInt("min_delay_between_messages");
            }
        }

        // Parse campaign dates
        if (campaignData.has("start_date")) {
            this.startDate = new Date(campaignData.getLong("start_date"));
        }

        if (campaignData.has("end_date")) {
            this.endDate = new Date(campaignData.getLong("end_date"));
        }
    }

    /**
     * @return the campaign id.
     */
    public int getId() {
        return id;
    }

    /**
     * Used internally to identify campaigns that have been marked as MessageCenter campaigns on the dashboard.
     *
     * @return true if the campaign is an MessageCenter campaign.
     */
    public boolean isMessageCenter() {
        return messageCenter;
    }

    /**
     * @return the name of the campaign.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @param date Current date
     * @return true if the campaign is active at the given time.
     */
    public boolean isActive(Date date) {
        return campaignDisplayer.isCampaignActive(this, date, null);
    }

    /**
     * @return the triggers for this campaign.
     */
    public List<Trigger> getTriggers() {
        return triggers;
    }

    /**
     * @return current impressions
     */
    public int getImpressions() {
        return saveableState.impressions;
    }

    public void setImpressions(int impressions) {
        this.saveableState.impressions = impressions;
    }

    /**
     * @return maximum impressions
     */
    public int getMaxImpressions() {
        return maxImpressions;
    }

    /**
     * @return the campaign start date.
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * @return the campaign end date.
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Increment impressions by one.
     */
    public void incrementImpressions() {
        this.saveableState.impressions++;
    }

    /**
     * Ensures a new message cannot be shown until now + minDelayBetweenMessage
     */
    protected void setMessageMinDelayThrottle() {
        this.saveableState.showMessagesAfterDelay = SwrveHelper.addTimeInterval(campaignManager.getNow(), this.minDelayBetweenMessage, Calendar.SECOND);
        campaignDisplayer.setMessageMinDelayThrottle(campaignManager.getNow());
    }

    /**
     * Used internally to set the status of the campaign.
     *
     * @param status new status of the campaign
     */
    public void setStatus(SwrveCampaignState.Status status) {
        this.saveableState.status = status;
    }

    /**
     * Get the status of the campaign.
     *
     * @return status of the campaign
     */
    public SwrveCampaignState.Status getStatus() {
        return saveableState.status;
    }

    /**
     * Used by sublcasses to inform that the campaign was displayed.
     */
    public void messageWasShownToUser() {
        setStatus(SwrveCampaignState.Status.Seen);
        incrementImpressions();
        setMessageMinDelayThrottle();
    }

    public abstract boolean supportsOrientation(SwrveOrientation orientation);

    /**
     * Used by QAUser to determine what kind of campaign we are reporting
     *
     * @return CAMPAIGN_TYPE enum
     */
    public abstract CAMPAIGN_TYPE getCampaignType();

    /**
     * Determine if the assets for this campaign have been downloaded.
     *
     * @param assetsOnDisk All assets that are already downloaded.
     * @return if the assets are ready
     */

    @Deprecated
    public abstract boolean areAssetsReady(Set<String> assetsOnDisk);

    /**
     * Determine if the assets for this campaign have been downloaded.
     *
     * @param assetsOnDisk All assets that are already downloaded.
     * @param properties   String map of personalized properties.
     * @return if the assets are ready
     */
    public abstract boolean areAssetsReady(Set<String> assetsOnDisk, Map<String, String> properties);

    /**
     * Obtain the serializable state of the campaign.
     *
     * @return the serializable state of the campaign.
     */
    public SwrveCampaignState getSaveableState() {
        return saveableState;
    }

    /**
     * Set the previous state of this campaign.
     *
     * @param saveableState The state to save
     */
    public void setSaveableState(SwrveCampaignState saveableState) {
        this.saveableState = saveableState;
    }

    public Date getShowMessagesAfterLaunch() {
        return showMessagesAfterLaunch;
    }
}
