package com.swrve.sdk;

import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.SwrveConversationCampaign;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.model.Arg;
import com.swrve.sdk.messaging.model.Conditions;
import com.swrve.sdk.messaging.model.Trigger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.swrve.sdk.QaCampaignInfo.CAMPAIGN_TYPE.CONVERSATION;
import static com.swrve.sdk.QaCampaignInfo.CAMPAIGN_TYPE.IAM;

/**
 * Logic for displaying campaigns based on conditions and limits.
 */
public class SwrveCampaignDisplayer {

    protected final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss ZZZZ", Locale.US);

    protected Date showMessagesAfterLaunch;
    protected Date showMessagesAfterDelay;
    protected int minDelayBetweenMessage;
    protected long messagesLeftToShow;

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

    protected boolean checkAppCampaignRules(int campaignsCount, String campaignType, String event, Map<String, String> eventPayload, Date now) {
        if (campaignsCount == 0) {
            noMessagesWereShown(event, eventPayload, "No " + campaignType + "s available");
            return false;
        }

        if (!event.equalsIgnoreCase(SwrveBase.SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER) && isTooSoonToShowMessageAfterLaunch(now)) {
            noMessagesWereShown(event, eventPayload, "{App throttle limit} Too soon after launch. Wait until " + timestampFormat.format(showMessagesAfterLaunch));
            return false;
        }

        if (isTooSoonToShowMessageAfterDelay(now)) {
            noMessagesWereShown(event, eventPayload, "{App throttle limit} Too soon after last " + campaignType + ". Wait until " + timestampFormat.format(showMessagesAfterDelay));
            return false;
        }

        if (hasShowTooManyMessagesAlready()) {
            noMessagesWereShown(event, eventPayload, "{App Throttle limit} Too many " + campaignType + "s shown");
            return false;
        }

        return true;
    }

    public boolean shouldShowCampaign(SwrveBaseCampaign swrveCampaign, String event, Map<String, String> payload, Date now, Map<Integer, QaCampaignInfo> qaCampaignInfoMap, int elementCount) {
        if (!canTrigger(swrveCampaign, event, payload, qaCampaignInfoMap)) {
            return false;
        }

        if (elementCount == 0) {
            logAndAddReason(swrveCampaign, "No campaign variants for campaign id:" + swrveCampaign.getId(), false, qaCampaignInfoMap);
            return false;
        }

        if (!isCampaignActive(swrveCampaign, now, qaCampaignInfoMap)) {
            return false;
        }

        if (swrveCampaign.getSaveableState().getImpressions() >= swrveCampaign.getMaxImpressions()) {
            String resultText = "{Campaign throttle limit} Campaign " + swrveCampaign.getId() + " has been shown " + swrveCampaign.getMaxImpressions() + " times already";
            logAndAddReason(swrveCampaign, resultText, false, qaCampaignInfoMap);
            return false;
        }

        // Ignore delay after launch throttle limit for auto show messages
        if (!event.equalsIgnoreCase(SwrveBase.SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER) && isTooSoonToShowMessageAfterLaunch(swrveCampaign, now)) {
            String formattedDate = timestampFormat.format(swrveCampaign.getShowMessagesAfterLaunch());
            String resultText = "{Campaign throttle limit} Too soon after launch. Wait until " + formattedDate;
            logAndAddReason(swrveCampaign, resultText, false, qaCampaignInfoMap);
            return false;
        }

        if (isTooSoonToShowMessageAfterDelay(swrveCampaign, now)) {
            String formattedDate = timestampFormat.format(swrveCampaign.getSaveableState().showMessagesAfterDelay);
            String resultText = "{Campaign throttle limit} Too soon after last campaign. Wait until " + formattedDate;
            logAndAddReason(swrveCampaign, resultText, false, qaCampaignInfoMap);
            return false;
        }

        return true;
    }

    protected boolean canTrigger(SwrveBaseCampaign swrveCampaign, String eventName, Map<String, String> payload, Map<Integer, QaCampaignInfo> qaCampaignInfoMap) {
        if (swrveCampaign.getTriggers() == null || swrveCampaign.getTriggers().size() == 0) {
            String resultText = "Campaign [" + swrveCampaign.getId() + "], no triggers (could be message centre). Skipping this campaign.";
            logAndAddReason(swrveCampaign, resultText, false, qaCampaignInfoMap);
            return false;
        }

        List<Trigger> triggers = swrveCampaign.getTriggers();
        for (Trigger trigger : triggers) {
            if (eventName != null && eventName.equalsIgnoreCase(trigger.getEventName())) {
                Conditions conditions = trigger.getConditions();
                if (conditions.getOp() == null && conditions.getArgs() == null) {
                    String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], matches eventName[" + eventName + "] & payload[" + payload + "].";
                    logAndAddReason(swrveCampaign, resultText, true, qaCampaignInfoMap);
                    return true; // no conditions equates to a match
                } else if (Conditions.Op.AND.equals(conditions.getOp())) {
                    boolean conditionsMatchPayload = false;
                    for (Arg arg : conditions.getArgs()) {
                        if (payload != null && payload.containsKey(arg.getKey()) && payload.get(arg.getKey()).equalsIgnoreCase(arg.getValue())) {
                            conditionsMatchPayload = true;
                        } else {
                            String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger.";
                            logAndAddReason(swrveCampaign, resultText, false, qaCampaignInfoMap);
                            conditionsMatchPayload = false;
                            break;
                        }
                    }
                    if (conditionsMatchPayload) {
                        String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], matches eventName[" + eventName + "] & payload[" + payload + "].";
                        logAndAddReason(swrveCampaign, resultText, true, qaCampaignInfoMap);
                        return true;
                    }
                } else if (Conditions.Op.OR.equals(conditions.getOp())) {
                    boolean conditionsMatchPayload = false;
                    for (Arg arg : conditions.getArgs()) {
                        if (payload != null && payload.containsKey(arg.getKey()) && payload.get(arg.getKey()).equalsIgnoreCase(arg.getValue())) {
                            String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], matches eventName[" + eventName + "] & payload[" + payload + "].";
                            logAndAddReason(swrveCampaign, resultText, true, qaCampaignInfoMap);
                            conditionsMatchPayload = true;
                            break;
                        }
                    }
                    if (conditionsMatchPayload) {
                        return true;
                    } else {
                        String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger.";
                        logAndAddReason(swrveCampaign, resultText, false, qaCampaignInfoMap);
                    }
                } else if (Conditions.Op.EQ.equals(conditions.getOp())) {
                    if (payload != null && payload.containsKey(conditions.getKey()) && payload.get(conditions.getKey()).equalsIgnoreCase(conditions.getValue())) {
                        String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], matches eventName[" + eventName + "] & payload[" + payload + "].";
                        logAndAddReason(swrveCampaign, resultText, true, qaCampaignInfoMap);
                        return true;
                    } else {
                        String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger.";
                        logAndAddReason(swrveCampaign, resultText, false, qaCampaignInfoMap);
                        continue;
                    }
                }
            } else {
                String resultText = "Campaign [" + swrveCampaign.getId() + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger.";
                logAndAddReason(swrveCampaign, resultText, false, qaCampaignInfoMap);
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

    public boolean isCampaignActive(SwrveBaseCampaign swrveCampaign, Date now, Map<Integer, QaCampaignInfo> qaCampaignInfoMap) {
        if (swrveCampaign.getStartDate().after(now)) {
            String resultText = "Campaign " + swrveCampaign.getId() + " has not started yet";
            logAndAddReason(swrveCampaign, resultText, false, qaCampaignInfoMap);
            return false;
        }
        if (swrveCampaign.getEndDate().before(now)) {
            String resultText = "Campaign " + swrveCampaign.getId() + " has finished";
            logAndAddReason(swrveCampaign, resultText, false, qaCampaignInfoMap);
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

    private void logAndAddReason(SwrveBaseCampaign swrveCampaign, String resultText, boolean displayed, Map<Integer, QaCampaignInfo> qaCampaignInfoMap) {
        if (QaUser.isLoggingEnabled() && swrveCampaign != null && qaCampaignInfoMap != null) {
            if (swrveCampaign instanceof SwrveConversationCampaign) {
                int variantId = ((SwrveConversationCampaign) swrveCampaign).getConversation().getId();
                qaCampaignInfoMap.put(swrveCampaign.getId(), new QaCampaignInfo(swrveCampaign.getId(), variantId, CONVERSATION, displayed, resultText));
            } else if (swrveCampaign instanceof SwrveInAppCampaign) {
                int variantId = ((SwrveInAppCampaign) swrveCampaign).getVariantId();
                qaCampaignInfoMap.put(swrveCampaign.getId(), new QaCampaignInfo(swrveCampaign.getId(), variantId, IAM, displayed, resultText));
            }
        }
        SwrveLogger.i(resultText);
    }

    private void noMessagesWereShown(String event, Map<String, String> eventPayload, String reason) {
        SwrveLogger.i("Not showing message for %s: %s", event, reason);
        QaUser.campaignsAppRuleTriggered(event, eventPayload, reason);
    }

    // Ensures a new message cannot be shown until now + minDelayBetweenMessage
    public void setMessageMinDelayThrottle(Date now) {
        this.showMessagesAfterDelay = SwrveHelper.addTimeInterval(now, this.minDelayBetweenMessage, Calendar.SECOND);
    }
}
