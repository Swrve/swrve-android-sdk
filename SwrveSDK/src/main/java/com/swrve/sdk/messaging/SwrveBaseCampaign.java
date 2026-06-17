package com.swrve.sdk.messaging;

import androidx.annotation.Nullable;

import com.swrve.sdk.ISwrveCampaignManager;
import com.swrve.sdk.QaCampaignInfo.CAMPAIGN_TYPE;
import com.swrve.sdk.SwrveCampaignDisplayer;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveUtils;
import com.swrve.sdk.messaging.model.Trigger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Swrve campaign containing messages targeted for the current device and user id.
 */
public abstract class SwrveBaseCampaign {

    public enum SwrveTimezoneType {
        GLOBAL, LOCAL;

        public static SwrveTimezoneType parse(String timezoneType) {
            if (timezoneType.equalsIgnoreCase("global")) {
                return GLOBAL;
            } else if (timezoneType.equalsIgnoreCase("local")) {
                return LOCAL;
            }
            return GLOBAL;
        }
    }

    public class SwrveBlackoutDate {
        public final String from;
        public final String to;

        SwrveBlackoutDate(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }

    public class SwrveIntervalTime {
        public final String from;
        public final String to;

        SwrveIntervalTime(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }

    // Default campaign throttle limits
    protected static int DEFAULT_DELAY_FIRST_MESSAGE = 180;
    protected static int DEFAULT_MAX_IMPRESSIONS = 99999;
    protected static int DEFAULT_MIN_DELAY_BETWEEN_MSGS = 60;

    protected ISwrveCampaignManager campaignManager;
    protected SwrveCampaignDisplayer campaignDisplayer;
    protected int id;
    protected SwrveCampaignState saveableState; // The state of the campaign that will be kept saved by the SDK
    private String startDateIso;
    private String endDateIso;
    private SwrveTimezoneType timezoneType;
    private List<SwrveBlackoutDate> blackoutDates;
    private List<SwrveIntervalTime> intervalTimes;
    protected List<Trigger> triggers;
    protected boolean messageCenter;
    protected boolean freemarkerEnabled;
    protected String subject; // subject of the campaign
    protected int priority;
    protected int maxImpressions;
    protected int minDelayBetweenMessage;
    protected Date showMessagesAfterLaunch; // Time we can show the first message after launch
    protected String name;
    protected SwrveMessageCenterDetails messageCenterDetails;

    /*
     * Parse a campaign from JSON data.
     */
    public SwrveBaseCampaign(ISwrveCampaignManager campaignManager, SwrveCampaignDisplayer campaignDisplayer, JSONObject campaignData) throws JSONException {
        this.campaignManager = campaignManager;
        this.campaignDisplayer = campaignDisplayer;

        this.id = campaignData.getInt("id");
        SwrveLogger.i("Parsing campaign %s", id);

        this.messageCenter = campaignData.optBoolean("message_center", false);
        this.freemarkerEnabled = campaignData.optBoolean("freemarker_enabled", false);
        this.subject = campaignData.isNull("subject") ? "" : campaignData.getString("subject");
        this.saveableState = new SwrveCampaignState(null, campaignManager.getNow()); // Start with an empty state

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
                this.maxImpressions = rules.getInt("dismiss_after_views");
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
        if (campaignData.has("start_date_iso")) {
            this.startDateIso = campaignData.getString("start_date_iso");
        }

        if (campaignData.has("end_date_iso")) {
            this.endDateIso = campaignData.getString("end_date_iso");
        }

        if (campaignData.has("timezone_type")) {
            this.timezoneType = SwrveTimezoneType.parse(campaignData.getString("timezone_type"));
        }

        if (campaignData.has("blackout_dates")) {
            this.blackoutDates = new ArrayList<>();
            JSONArray jsonArray = campaignData.getJSONArray("blackout_dates");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject blackoutDate = jsonArray.getJSONObject(i);
                if (blackoutDate.has("from") && blackoutDate.has("to")) {
                    SwrveBlackoutDate swrveBlackoutDate = new SwrveBlackoutDate(blackoutDate.getString("from"), blackoutDate.getString("to"));
                    this.blackoutDates.add(swrveBlackoutDate);
                }
            }
        }

        if (campaignData.has("interval_times")) {
            this.intervalTimes = new ArrayList<>();
            JSONArray jsonArray = campaignData.getJSONArray("interval_times");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject intervalTime = jsonArray.getJSONObject(i);
                if (intervalTime.has("from") && intervalTime.has("to")) {
                    SwrveIntervalTime swrveIntervalTime = new SwrveIntervalTime(intervalTime.getString("from"), intervalTime.getString("to"));
                    this.intervalTimes.add(swrveIntervalTime);
                }
            }
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

    public boolean isFreemarkerEnabled() {
        return freemarkerEnabled;
    }

    /**
     * @param date Current date
     * @return true if the campaign is active at the given time.
     */
    public boolean isActive(Date date) {
        return campaignDisplayer.isCampaignActive(this, date, new HashMap<>());
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
        Date date;
        try {
            date = SwrveUtils.parseIso8601Date(startDateIso, timezoneType);
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK: Error parsing campaign start date", e);
            date = new Date(Long.MAX_VALUE); // Default to future date so it doesn't show
        }
        return date;
    }

    /**
     * @return the campaign end date.
     */
    public Date getEndDate() {
        Date date;
        try {
            date = SwrveUtils.parseIso8601Date(endDateIso, timezoneType);
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK: Error parsing campaign end date", e);
            date = new Date(0L); // Default to past date so it doesn't show
        }
        return date;
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
     * Get the download date of the campaign.
     *
     * @return the download date of the campaign
     */
    public Date getDownloadDate() {
        return saveableState.getDownloadDate();
    }

    /**
     * Used by sublcasses to inform that the campaign was displayed.
     */
    public void messageWasHandledOrShownToUser() {
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

    /**
     * @return the campaign name
     */
    public String getName() {
        return name;
    }

    public SwrveMessageCenterDetails getMessageCenterDetails() {
        return messageCenterDetails;
    }

    public void setMessageCenterDetails(SwrveMessageCenterDetails messageCenterDetails) {
        this.messageCenterDetails = messageCenterDetails;
    }

    /**
     * @return the priority of the campaign.
     */
    public int getPriority() {
        return priority;
    }

    @Nullable
    public SwrveTimezoneType getTimezoneType() {
        return timezoneType;
    }

    /**
     * Whether date built-ins in FreeMarker templates should be evaluated in the device's local
     * timezone (LOCAL) rather than UTC (GLOBAL). Single source of truth for the GLOBAL/LOCAL
     * mapping — mirrors how timezoneType drives campaign start/end/blackout date parsing. A null
     * timezoneType (legacy campaigns) defaults to UTC.
     */
    public boolean useLocalTimezone() {
        return timezoneType == SwrveTimezoneType.LOCAL;
    }

    @Nullable
    public List<SwrveBlackoutDate> getBlackoutDates() {
        return blackoutDates;
    }

    @Nullable
    public List<SwrveIntervalTime> getIntervalTimes() {
        return intervalTimes;
    }
}
