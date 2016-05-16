package com.swrve.sdk;

import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.model.Arg;
import com.swrve.sdk.messaging.model.Conditions;
import com.swrve.sdk.messaging.model.Trigger;
import com.swrve.sdk.qa.SwrveQAUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Logic for displaying campaigns based on conditions and limits.
 */
public class SwrveCampaignDisplayer {

    private static final String LOG_TAG = "SwrveSDK";
    protected final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss ZZZZ", Locale.US);

    public enum DisplayResult {
        CAMPAIGN_THROTTLE_RECENT,
        CAMPAIGN_THROTTLE_MAX_IMPRESSIONS,
        CAMPAIGN_THROTTLE_LAUNCH_TIME,
        ERROR_NO_VARIANT,
        CAMPAIGN_NOT_ACTIVE,
        ERROR_INVALID_TRIGGERS,
        NO_MATCH,
        MATCH,
        CAMPAIGN_NOT_DOWNLOADED,
        CAMPAIGN_WRONG_ORIENTATION,
        ELIGIBLE_BUT_OTHER_CHOSEN
    }

    private final SwrveQAUser qaUser;
    protected Date showMessagesAfterLaunch;
    protected Date showMessagesAfterDelay;
    protected int minDelayBetweenMessage;
    protected long messagesLeftToShow;

    public SwrveCampaignDisplayer(SwrveQAUser qaUser) {
        this.qaUser = qaUser;
    }

    protected void setShowMessagesAfterLaunch(Date showMessagesAfterLaunch) {
        this.showMessagesAfterLaunch = showMessagesAfterLaunch;
    }

    protected void setMinDelayBetweenMessage(int minDelayBetweenMessage) {
        this.minDelayBetweenMessage = minDelayBetweenMessage;
    }

    protected void setMessagesLeftToShow(long messagesLeftToShow) {
        this.messagesLeftToShow = messagesLeftToShow;
    }

    protected void decrementMessagesLeftToShow() {
        this.messagesLeftToShow = this.messagesLeftToShow - 1;
    }

    protected boolean checkAppCampaignRules(int campaignsCount, String campaignType, String event, Date now) {
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

    public boolean shouldShowCampaign(SwrveBaseCampaign swrveCampaign, String event, Map<String, String> payload, Date now, Map<Integer, Result> campaignDisplayResults, int elementCount) {
        if (!canTrigger(swrveCampaign, event, payload, campaignDisplayResults)) {
            return false;
        }

        if (elementCount == 0) {
            logAndAddReason(swrveCampaign, campaignDisplayResults, DisplayResult.ERROR_NO_VARIANT, "No campaign variants for campaign id:" + swrveCampaign.getId());
            return false;
        }

        if (!isCampaignActive(swrveCampaign, now, campaignDisplayResults)) {
            return false;
        }

        if (swrveCampaign.getSaveableState().getImpressions() >= swrveCampaign.getMaxImpressions()) {
            String resultText = "{Campaign throttle limit} Campaign " + swrveCampaign.getId() + " has been shown " + swrveCampaign.getMaxImpressions() + " times already";
            logAndAddReason(swrveCampaign, campaignDisplayResults, DisplayResult.CAMPAIGN_THROTTLE_MAX_IMPRESSIONS, resultText);
            return false;
        }

        // Ignore delay after launch throttle limit for auto show messages
        if (!event.equalsIgnoreCase(SwrveBase.SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER) && isTooSoonToShowMessageAfterLaunch(swrveCampaign, now)) {
            String formattedDate = timestampFormat.format(swrveCampaign.getShowMessagesAfterLaunch());
            String resultText = "{Campaign throttle limit} Too soon after launch. Wait until " + formattedDate;
            logAndAddReason(swrveCampaign, campaignDisplayResults, DisplayResult.CAMPAIGN_THROTTLE_LAUNCH_TIME, resultText);
            return false;
        }

        if (isTooSoonToShowMessageAfterDelay(swrveCampaign, now)) {
            String formattedDate = timestampFormat.format(swrveCampaign.getSaveableState().showMessagesAfterDelay);
            String resultText = "{Campaign throttle limit} Too soon after last campaign. Wait until " + formattedDate;
            logAndAddReason(swrveCampaign, campaignDisplayResults, DisplayResult.CAMPAIGN_THROTTLE_RECENT, resultText);
            return false;
        }

        return true;
    }

    protected boolean canTrigger(SwrveBaseCampaign swrveCampaign, String eventName, Map<String, String> payload, Map<Integer, Result> campaignDisplayResults) {
        if (swrveCampaign.getTriggers() == null || swrveCampaign.getTriggers().size() == 0) {
            String resultText = "Campaign [" + swrveCampaign.getId() + "], invalid triggers. Skipping this campaign.";
            logAndAddReason(swrveCampaign, campaignDisplayResults, DisplayResult.ERROR_INVALID_TRIGGERS, resultText);
            return false;
        }

        List<Trigger> triggers = swrveCampaign.getTriggers();
        for (Trigger trigger : triggers) {
            if (eventName != null && eventName.equalsIgnoreCase(trigger.getEventName())) {
                Conditions conditions = trigger.getConditions();
                if (conditions.getOp() == null && conditions.getArgs() == null) {
                    String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], matches eventName[" + eventName + "] & payload[" + payload + "].";
                    logAndAddReason(swrveCampaign, campaignDisplayResults, DisplayResult.MATCH, resultText);
                    return true; // no conditions equates to a match
                } else if (Conditions.Op.AND.equals(conditions.getOp())) {
                    boolean conditionsMatchPayload = false;
                    for (Arg arg : conditions.getArgs()) {
                        if (payload != null && payload.containsKey(arg.getKey()) && payload.get(arg.getKey()).equalsIgnoreCase(arg.getValue())) {
                            conditionsMatchPayload = true;
                        } else {
                            String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger.";
                            logAndAddReason(swrveCampaign, campaignDisplayResults, DisplayResult.NO_MATCH, resultText);
                            conditionsMatchPayload = false;
                            break;
                        }
                    }
                    if (conditionsMatchPayload) {
                        String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], matches eventName[" + eventName + "] & payload[" + payload + "].";
                        logAndAddReason(swrveCampaign, campaignDisplayResults, DisplayResult.MATCH, resultText);
                        return true;
                    }
                } else if (Conditions.Op.EQ.equals(conditions.getOp())) {
                    if (payload != null && payload.containsKey(conditions.getKey()) && payload.get(conditions.getKey()).equalsIgnoreCase(conditions.getValue())) {
                        String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], matches eventName[" + eventName + "] & payload[" + payload + "].";
                        logAndAddReason(swrveCampaign, campaignDisplayResults, DisplayResult.MATCH, resultText);
                        return true;
                    } else {
                        String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger.";
                        logAndAddReason(swrveCampaign, campaignDisplayResults, DisplayResult.NO_MATCH, resultText);
                        continue;
                    }
                }
            } else {
                String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger.";
                logAndAddReason(swrveCampaign, campaignDisplayResults, DisplayResult.NO_MATCH, resultText);
                continue;
            }
        }

        return false;
    }

    private boolean isTooSoonToShowMessageAfterLaunch(SwrveBaseCampaign swrveCampaign, Date now) {
        return now.before(swrveCampaign.getShowMessagesAfterLaunch());
    }

    private boolean isTooSoonToShowMessageAfterDelay(SwrveBaseCampaign swrveCampaign, Date now) {
        SwrveCampaignState swrveCampaignState = swrveCampaign.getSaveableState();
        return (swrveCampaignState.showMessagesAfterDelay != null && now.before(swrveCampaignState.showMessagesAfterDelay));
    }

    public boolean isCampaignActive(SwrveBaseCampaign swrveCampaign, Date now, Map<Integer, Result> campaignDisplayResult) {
        if (swrveCampaign.getStartDate().after(now)) {
            String resultText = "Campaign " + swrveCampaign.getId() + " has not started yet";
            logAndAddReason(swrveCampaign, campaignDisplayResult, DisplayResult.CAMPAIGN_NOT_ACTIVE, resultText);
            return false;
        }
        if (swrveCampaign.getEndDate().before(now)) {
            String resultText = "Campaign " + swrveCampaign.getId() + " has finished";
            logAndAddReason(swrveCampaign, campaignDisplayResult, DisplayResult.CAMPAIGN_NOT_ACTIVE, resultText);
            return false;
        }
        return true;
    }

    private boolean hasShowTooManyMessagesAlready() {
        return messagesLeftToShow <= 0;
    }

    private boolean isTooSoonToShowMessageAfterLaunch(Date now) {
        return now.before(showMessagesAfterLaunch);
    }

    private boolean isTooSoonToShowMessageAfterDelay(Date now) {
        if (showMessagesAfterDelay == null) {
            return false;
        }
        return now.before(showMessagesAfterDelay);
    }

    private void logAndAddReason(SwrveBaseCampaign swrveCampaign, Map<Integer, Result> campaignDisplayResults, DisplayResult resultCode, String resultText) {
        if (campaignDisplayResults != null) {
            campaignDisplayResults.put(swrveCampaign.getId(), buildResult(resultCode, resultText));
        }
        SwrveLogger.i(LOG_TAG, resultText);
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

    public Result buildResult(DisplayResult resultCode, String resultText) {
        Result result = new Result();
        result.resultCode = resultCode;
        result.resultText = resultText;
        return result;
    }

    public final class Result {
        public DisplayResult resultCode;
        public String resultText;
    }
}
