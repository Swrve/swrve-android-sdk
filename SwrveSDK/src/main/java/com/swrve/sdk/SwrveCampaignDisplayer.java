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

    protected static final String LOG_TAG = "SwrveSDK";
    protected final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss ZZZZ", Locale.US);

    public static final int RULE_RESULT_CAMPAIGN_THROTTLE_RECENT = 1;
    public static final int RULE_RESULT_CAMPAIGN_THROTTLE_MAX_IMPRESSIONS = 2;
    public static final int RULE_RESULT_CAMPAIGN_THROTTLE_LAUNCH_TIME = 3;
    public static final int RULE_RESULT_ERROR_NO_VARIANT = 4;
    public static final int RULE_RESULT_CAMPAIGN_NOT_ACTIVE = 5;
    public static final int RULE_RESULT_ERROR_INVALID_TRIGGERS = 6;
    public static final int RULE_RESULT_ERROR_UNSUPPORTED = 7;
    public static final int RULE_RESULT_NO_MATCH = 8;
    public static final int RULE_RESULT_CAMPAIGN_NOT_DOWNLOADED = 9;
    public static final int RULE_RESULT_CAMPAIGN_WRONG_ORIENTATION = 10;
    public static final int RULE_RESULT_ELIGIBLE_BUT_OTHER_CHOSEN = 11;

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
            logAndAddReason(swrveCampaign, campaignDisplayResults, RULE_RESULT_ERROR_NO_VARIANT, "No campaign variants for campaign id:" + swrveCampaign.getId());
            return false;
        }

        if (!isCampaignActive(swrveCampaign, now, campaignDisplayResults)) {
            return false;
        }

        if (swrveCampaign.getSaveableState().impressions >= swrveCampaign.getMaxImpressions()) {
            String resultText = "{Campaign throttle limit} Campaign " + swrveCampaign.getId() + " has been shown " + swrveCampaign.getMaxImpressions() + " times already";
            logAndAddReason(swrveCampaign, campaignDisplayResults, RULE_RESULT_CAMPAIGN_THROTTLE_MAX_IMPRESSIONS, resultText);
            return false;
        }

        // Ignore delay after launch throttle limit for auto show messages
        if (!event.equalsIgnoreCase(SwrveBase.SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER) && isTooSoonToShowMessageAfterLaunch(swrveCampaign, now)) {
            String formattedDate = timestampFormat.format(swrveCampaign.getShowMessagesAfterLaunch());
            String resultText = "{Campaign throttle limit} Too soon after launch. Wait until " + formattedDate;
            logAndAddReason(swrveCampaign, campaignDisplayResults, RULE_RESULT_CAMPAIGN_THROTTLE_LAUNCH_TIME, resultText);
            return false;
        }

        if (isTooSoonToShowMessageAfterDelay(swrveCampaign, now)) {
            String formattedDate = timestampFormat.format(swrveCampaign.getSaveableState().showMessagesAfterDelay);
            String resultText = "{Campaign throttle limit} Too soon after last campaign. Wait until " + formattedDate;
            logAndAddReason(swrveCampaign, campaignDisplayResults, RULE_RESULT_CAMPAIGN_THROTTLE_RECENT, resultText);
            return false;
        }

        return true;
    }

    protected boolean canTrigger(SwrveBaseCampaign swrveCampaign, String eventName, Map<String, String> payload, Map<Integer, Result> campaignDisplayResults) {
        if (swrveCampaign.getTriggers() == null || swrveCampaign.getTriggers().size() == 0) {
            String resultText = "Campaign [" + swrveCampaign.getId() + "], invalid triggers. Skipping this campaign.";
            logAndAddReason(swrveCampaign, campaignDisplayResults, RULE_RESULT_ERROR_INVALID_TRIGGERS, resultText);
            return false;
        }

        List<Trigger> triggers = swrveCampaign.getTriggers();
        for (Trigger trigger : triggers) {
            if(eventName.equalsIgnoreCase(trigger.getEventName())) {
                Conditions conditions = trigger.getConditions();
                if (conditions == null) {
                    String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], contains invalid conditions. Skipping this trigger.";
                    logAndAddReason(swrveCampaign, campaignDisplayResults, RULE_RESULT_ERROR_UNSUPPORTED, resultText);
                    continue;
                }
                else if (conditions.getOp() == null && conditions.getArgs() == null) {
                    return true; // no conditions equates to a match
                } else if (conditions.getOp() != null && "and".equals(conditions.getOp())) {
                    if (conditions.getArgs() != null && conditions.getArgs().size() > 0) {
                        boolean conditionsMatchPayload = false;
                        for (Arg arg : conditions.getArgs()) {
                            if (arg.getKey() == null || arg.getOp() == null || !"eq".equals(arg.getOp()) || arg.getValue() == null) {
                                String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], contains invalid conditions. Skipping this trigger.";
                                logAndAddReason(swrveCampaign, campaignDisplayResults, RULE_RESULT_ERROR_UNSUPPORTED, resultText);
                                conditionsMatchPayload = false;
                                continue;
                            } else if (payload.containsKey(arg.getKey()) && payload.get(arg.getKey()).equalsIgnoreCase(arg.getValue())) {
                                conditionsMatchPayload = true;
                            } else {
                                String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], does not match conditions [" + arg + "]. Skipping this trigger.";
                                logAndAddReason(swrveCampaign, campaignDisplayResults, RULE_RESULT_NO_MATCH, resultText);
                                conditionsMatchPayload = false;
                                break;
                            }
                        }
                        if (conditionsMatchPayload) {
                            return true;
                        }
                    } else {
                        String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], contains invalid conditions. Skipping this trigger.";
                        logAndAddReason(swrveCampaign, campaignDisplayResults, RULE_RESULT_ERROR_UNSUPPORTED, resultText);
                        continue;
                    }
                } else if (conditions != null && conditions.getOp() != null && "eq".equals(conditions.getOp())) {
                    if (conditions.getKey() == null  || conditions.getValue() == null) {
                        String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], contains invalid conditions. Skipping this trigger.";
                        logAndAddReason(swrveCampaign, campaignDisplayResults, RULE_RESULT_ERROR_UNSUPPORTED, resultText);
                        continue;
                    } else if (payload.containsKey(conditions.getKey()) && payload.get(conditions.getKey()).equalsIgnoreCase(conditions.getValue())) {
                        return true; // matched condition
                    } else {
                        String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], does not match conditions. Skipping this trigger.";
                        logAndAddReason(swrveCampaign, campaignDisplayResults, RULE_RESULT_NO_MATCH, resultText);
                        continue;
                    }
                }
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
            logAndAddReason(swrveCampaign, campaignDisplayResult, RULE_RESULT_CAMPAIGN_NOT_ACTIVE, resultText);
            return false;
        }
        if (swrveCampaign.getEndDate().before(now)) {
            String resultText = "Campaign " + swrveCampaign.getId() + " has finished";
            logAndAddReason(swrveCampaign, campaignDisplayResult, RULE_RESULT_CAMPAIGN_NOT_ACTIVE, resultText);
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

    private void logAndAddReason(SwrveBaseCampaign swrveCampaign, Map<Integer, Result> campaignDisplayResults, int resultCode, String resultText) {
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

    public Result buildResult(int resultCode, String resultText){
        Result result = new Result();
        result.resultCode = resultCode;
        result.resultText = resultText;
        return result;
    }

    public final class Result {
        public int resultCode;
        public String resultText;
    }
}
