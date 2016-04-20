package com.swrve.sdk.messaging;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.qa.SwrveQAUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Logic for campaign conditions/limits.
 *
 */
public class SwrveCampaignRulesManager {

    protected static final String LOG_TAG = "SwrveSDK";
    protected final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss ZZZZ", Locale.US);

    private final SwrveQAUser qaUser;
    protected Date showMessagesAfterLaunch;
    protected Date showMessagesAfterDelay;
    protected int minDelayBetweenMessage;
    protected long messagesLeftToShow;

    public SwrveCampaignRulesManager(SwrveQAUser qaUser) {
        this.qaUser = qaUser;
    }

    public void setShowMessagesAfterLaunch(Date showMessagesAfterLaunch) {
        this.showMessagesAfterLaunch = showMessagesAfterLaunch;
    }

    public void setMinDelayBetweenMessage(int minDelayBetweenMessage) {
        this.minDelayBetweenMessage = minDelayBetweenMessage;
    }

    public void setMessagesLeftToShow(long messagesLeftToShow) {
        this.messagesLeftToShow = messagesLeftToShow;
    }

    public void decrementMessagesLeftToShow() {
        this.messagesLeftToShow = this.messagesLeftToShow - 1;
    }

    public boolean checkCampaignRules(int campaignsCount, String campaignType, String event, Date now) {
        if (campaignsCount == 0) {
            noMessagesWereShown(event, "No " + campaignType + "s available");
            return false;
        }

        if (!event.equalsIgnoreCase(SwrveBase.SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER) && isTooSoonToShowMessageAfterLaunch(now)) {
            noMessagesWereShown(event, "{App throttle limit} Too soon after launch. Wait until " + timestampFormat.format(showMessagesAfterLaunch));
            return false;
        }

        if (isTooSoonToShowMessageAfterDelay(now)) {
            noMessagesWereShown(event, "{App throttle limit} Too soon after last " + campaignType + ". Wait until " + timestampFormat.format(showMessagesAfterDelay));
            return false;
        }

        if (hasShowTooManyMessagesAlready()) {
            noMessagesWereShown(event, "{App Throttle limit} Too many " + campaignType + "s shown");
            return false;
        }

        return true;
    }

    protected boolean shouldShowCampaign(SwrveBaseCampaign swrveCampaign, String event, Date now, Map<Integer, String> campaignReasons, int elementCount) {
        if (!canTrigger(swrveCampaign, event)) {
            SwrveLogger.i(LOG_TAG, "There is no trigger in " + swrveCampaign.getId() + " that matches " + event);
            return false;
        }

        if (elementCount == 0) {
            logAndAddReason(swrveCampaign, campaignReasons, "No campaign variants for campaign id:" + swrveCampaign.getId());
            return false;
        }

        if (!isCampaignActive(swrveCampaign, now, campaignReasons)) {
            return false;
        }

        if (swrveCampaign.getSaveableState().impressions >= swrveCampaign.getMaxImpressions()) {
            logAndAddReason(swrveCampaign, campaignReasons, "{Campaign throttle limit} Campaign " + swrveCampaign.getId() + " has been shown " + swrveCampaign.getMaxImpressions() + " times already");
            return false;
        }

        // Ignore delay after launch throttle limit for auto show messages
        if (!event.equalsIgnoreCase(SwrveBase.SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER) && isTooSoonToShowMessageAfterLaunch(swrveCampaign, now)) {
            logAndAddReason(swrveCampaign, campaignReasons, "{Campaign throttle limit} Too soon after launch. Wait until " + timestampFormat.format(swrveCampaign.showMessagesAfterLaunch));
            return false;
        }

        if (isTooSoonToShowMessageAfterDelay(swrveCampaign, now)) {
            String formattedDate = timestampFormat.format(swrveCampaign.getSaveableState().showMessagesAfterDelay);
            logAndAddReason(swrveCampaign, campaignReasons, "{Campaign throttle limit} Too soon after last campaign. Wait until " + formattedDate);
            return false;
        }

        return true;
    }

    /**
     * Check if the campaign contains messages for the given event.
     *
     * @param eventName the event name to trigger the campaign
     * @return true if the campaign has this event as a trigger
     */
    public boolean canTrigger(SwrveBaseCampaign swrveCampaign, String eventName) {
        String lowerCaseEvent = eventName.toLowerCase(Locale.US);
        return swrveCampaign.getTriggers() != null && swrveCampaign.getTriggers().contains(lowerCaseEvent);
    }

    private boolean isTooSoonToShowMessageAfterLaunch(SwrveBaseCampaign swrveCampaign, Date now) {
        return now.before(swrveCampaign.showMessagesAfterLaunch);
    }

    private boolean isTooSoonToShowMessageAfterDelay(SwrveBaseCampaign swrveCampaign, Date now) {
        SwrveCampaignState swrveCampaignState = swrveCampaign.getSaveableState();
        return (swrveCampaignState.showMessagesAfterDelay != null && now.before(swrveCampaignState.showMessagesAfterDelay));
    }

    protected boolean isCampaignActive(SwrveBaseCampaign swrveCampaign, Date now, Map<Integer, String> campaignReasons) {
        if (swrveCampaign.getStartDate().after(now)) {
            logAndAddReason(swrveCampaign, campaignReasons, "Campaign " + swrveCampaign.getId() + " has not started yet");
            return false;
        }
        if (swrveCampaign.getEndDate().before(now)) {
            logAndAddReason(swrveCampaign, campaignReasons, "Campaign " + swrveCampaign.getId() + " has finished");
            return false;
        }
        return true;
    }

    protected boolean hasShowTooManyMessagesAlready() {
        return messagesLeftToShow <= 0;
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


    private void logAndAddReason(SwrveBaseCampaign swrveCampaign, Map<Integer, String> campaignReasons, String reason) {
        if (campaignReasons != null) {
            campaignReasons.put(swrveCampaign.getId(), reason);
        }
        SwrveLogger.i(LOG_TAG, reason);
    }

    private void noMessagesWereShown(String event, String reason) {
        SwrveLogger.i(LOG_TAG, "Not showing message for " + event + ": " + reason);
        if (qaUser != null) {
            qaUser.triggerFailure(event, reason);
        }
    }

    /**
     * Ensures a new message cannot be shown until now + minDelayBetweenMessage
     */
    public void setMessageMinDelayThrottle(Date now) {
        this.showMessagesAfterDelay = SwrveHelper.addTimeInterval(now, this.minDelayBetweenMessage, Calendar.SECOND);
    }
}
