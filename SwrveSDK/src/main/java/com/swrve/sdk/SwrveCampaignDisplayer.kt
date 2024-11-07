package com.swrve.sdk

import com.swrve.sdk.messaging.SwrveBaseCampaign
import com.swrve.sdk.messaging.SwrveBaseCampaign.SwrveTimezoneType
import com.swrve.sdk.messaging.SwrveInAppCampaign
import com.swrve.sdk.messaging.model.Arg
import com.swrve.sdk.messaging.model.Conditions
import com.swrve.sdk.messaging.model.Trigger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Logic for displaying campaigns based on conditions and limits.
 */
class SwrveCampaignDisplayer {
    private val timestampFormat = SimpleDateFormat("HH:mm:ss ZZZZ", Locale.US)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)

    var showMessagesAfterLaunch: Date? = null
    private var showMessagesAfterDelay: Date? = null
    var minDelayBetweenMessage: Int = 0
    var messagesLeftToShow: Long = 0

    fun decrementMessagesLeftToShow() {
        this.messagesLeftToShow = this.messagesLeftToShow - 1
    }

    fun checkAppCampaignRules(campaignsCount: Int, campaignType: String, event: String, eventPayload: Map<String, String>?, now: Date): Boolean {
        if (campaignsCount == 0) {
            noMessagesWereShown(event, eventPayload, "No " + campaignType + "s available")
            return false
        }

        if (!event.equals(SwrveBase.SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER, ignoreCase = true) && isTooSoonToShowMessageAfterLaunch(now)) {
            noMessagesWereShown(event, eventPayload, "{App throttle limit} Too soon after launch. Wait until " + timestampFormat.format(showMessagesAfterLaunch))
            return false
        }

        if (isTooSoonToShowMessageAfterDelay(now)) {
            noMessagesWereShown(event, eventPayload, "{App throttle limit} Too soon after last " + campaignType + ". Wait until " + timestampFormat.format(showMessagesAfterDelay))
            return false
        }

        if (hasShowTooManyMessagesAlready()) {
            noMessagesWereShown(event, eventPayload, "{App Throttle limit} Too many " + campaignType + "s shown")
            return false
        }

        return true
    }

    fun shouldShowCampaign(campaign: SwrveBaseCampaign, event: String, payload: Map<String?, String>?, now: Date, qaCampaignInfoMap: MutableMap<Int?, QaCampaignInfo?>, elementCount: Int): Boolean {
        if (!canTrigger(campaign, event, payload, qaCampaignInfoMap)) {
            return false
        }

        if (elementCount == 0) {
            logAndAddReason(campaign, "No campaign variants for campaign id:" + campaign.id, false, qaCampaignInfoMap)
            return false
        }

        if (!isCampaignActive(campaign, now, qaCampaignInfoMap)) {
            return false
        }

        if (campaign.saveableState.impressions >= campaign.maxImpressions) {
            val text = "{Campaign throttle limit} Campaign " + campaign.id + " has been shown " + campaign.maxImpressions + " times already"
            logAndAddReason(campaign, text, false, qaCampaignInfoMap)
            return false
        }

        // Ignore delay after launch throttle limit for auto show messages
        if (!event.equals(SwrveBase.SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER, ignoreCase = true) && isTooSoonToShowMessageAfterLaunch(campaign, now)) {
            val formattedDate = timestampFormat.format(campaign.showMessagesAfterLaunch)
            val text = "{Campaign throttle limit} Too soon after launch. Wait until $formattedDate"
            logAndAddReason(campaign, text, false, qaCampaignInfoMap)
            return false
        }

        if (isTooSoonToShowMessageAfterDelay(campaign, now)) {
            val formattedDate = timestampFormat.format(campaign.saveableState.showMessagesAfterDelay)
            val text = "{Campaign throttle limit} Too soon after last campaign. Wait until $formattedDate"
            logAndAddReason(campaign, text, false, qaCampaignInfoMap)
            return false
        }

        return true
    }

    fun canTrigger(swrveCampaign: SwrveBaseCampaign, eventName: String?, payload: Map<String?, String>?, qaCampaignInfoMap: MutableMap<Int?, QaCampaignInfo?>): Boolean {
        if (swrveCampaign.triggers == null || swrveCampaign.triggers.size == 0) {
            val text = "Campaign [" + swrveCampaign.id + "], no triggers (could be message centre). Skipping this campaign."
            logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
            return false
        }

        val triggers = swrveCampaign.triggers
        for (trigger in triggers) {
            if (eventName != null && eventName.equals(trigger.eventName, ignoreCase = true)) {
                val conditions = trigger.conditions
                if (conditions.op == null && conditions.args == null) {
                    val text = "Campaign [" + swrveCampaign.id + "], Trigger [" + trigger + "], matches eventName[" + eventName + "] & payload[" + payload + "]."
                    logAndAddReason(swrveCampaign, text, true, qaCampaignInfoMap)
                    return true // no conditions equates to a match
                } else if (Conditions.Op.AND == conditions.op) {
                    var conditionsMatchPayload = false
                    for (arg in conditions.args) {
                        if (arg.op == Arg.Op.EQ) {
                            if (payload != null && payload.containsKey(arg.key) && payload[arg.key].equals(arg.value as String, ignoreCase = true)) {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                            } else {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, false, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            }
                        } else if (arg.op == Arg.Op.CONTAINS) {
                            if (payload != null && payload.containsKey(arg.key) && payload[arg.key]!!.lowercase().contains(arg.value.toString().lowercase())) {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                            } else {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, false, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            }
                        } else if (arg.op == Arg.Op.NUMBER_GT) {
                            val value = arg.value as Double
                            val valueInteger = value.toInt()
                            if (payload != null && payload.containsKey(arg.key) && payload[arg.key]!!.toInt() > valueInteger) {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            } else {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, false, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            }
                        } else if (arg.op == Arg.Op.NUMBER_LT) {
                            val value = arg.value as Double
                            val valueInteger = value.toInt()
                            if (payload != null && payload.containsKey(arg.key) && payload[arg.key]!!.toInt() < valueInteger) {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            } else {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, false, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            }
                        } else if (arg.op == Arg.Op.NUMBER_EQ) {
                            val value = arg.value as Double
                            val valueInteger = value.toInt()
                            if (payload != null && payload.containsKey(arg.key) && payload[arg.key]!!.toInt() == valueInteger) {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            } else {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, false, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            }
                        } else if (arg.op == Arg.Op.NUMBER_NOT_BETWEEN) {
                            if (payload != null && payload.containsKey(arg.key) && arg.value is Map<*, *>) {
                                val values = arg.value as Map<String, Double>
                                val lower = values["lower"]!!.toInt()
                                val upper = values["upper"]!!.toInt()
                                val payloadValue = payload[arg.key]!!.toInt()

                                if (payloadValue < lower || payloadValue > upper) {
                                    conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                                    break
                                } else {
                                    conditionsMatchPayload = logAndAddReason(swrveCampaign, false, qaCampaignInfoMap, eventName, trigger, payload)
                                    break
                                }
                            } else {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, false, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            }
                        } else if (arg.op == Arg.Op.NUMBER_BETWEEN) {
                            if (payload != null && payload.containsKey(arg.key) && arg.value is Map<*, *>) {
                                val values = arg.value as Map<String, Double>
                                val lower = values["lower"]!!.toInt()
                                val upper = values["upper"]!!.toInt()
                                val payloadValue = payload[arg.key]!!.toInt()

                                if (payloadValue > lower && payloadValue < upper) {
                                    conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                                    break
                                } else {
                                    conditionsMatchPayload = logAndAddReason(swrveCampaign, false, qaCampaignInfoMap, eventName, trigger, payload)
                                    break
                                }
                            } else {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, false, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            }
                        }
                    }
                    if (conditionsMatchPayload) {
                        return logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                    }
                } else if (Conditions.Op.OR == conditions.op) {
                    var conditionsMatchPayload = false
                    for (arg in conditions.args) {
                        if (arg.op == Arg.Op.EQ) {
                            if (payload != null && payload.containsKey(arg.key) && payload[arg.key].equals(arg.value as String, ignoreCase = true)) {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            }
                        } else if (arg.op == Arg.Op.CONTAINS) {
                            if (payload != null && payload.containsKey(arg.key) && payload[arg.key]!!.lowercase().contains(arg.value.toString().lowercase())) {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            }
                        } else if (arg.op == Arg.Op.NUMBER_GT) {
                            val value = arg.value as Double
                            val valueInteger = value.toInt()
                            if (payload != null && payload.containsKey(arg.key) && payload[arg.key]!!.toInt() > valueInteger) {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            }
                        } else if (arg.op == Arg.Op.NUMBER_LT) {
                            val value = arg.value as Double
                            val valueInteger = value.toInt()
                            if (payload != null && payload.containsKey(arg.key) && payload[arg.key]!!.toInt() < valueInteger) {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            }
                        } else if (arg.op == Arg.Op.NUMBER_EQ) {
                            val value = arg.value as Double
                            val valueInteger = value.toInt()
                            if (payload != null && payload.containsKey(arg.key) && payload[arg.key]!!.toInt() == valueInteger) {
                                conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                                break
                            }
                        } else if (arg.op == Arg.Op.NUMBER_NOT_BETWEEN) {
                            if (payload != null && payload.containsKey(arg.key) && arg.value is Map<*, *>) {
                                val values = arg.value as Map<String, Double>
                                val lower = values["lower"]!!.toInt()
                                val upper = values["upper"]!!.toInt()
                                val payloadValue = payload[arg.key]!!.toInt()

                                if (payloadValue < lower || payloadValue > upper) {
                                    conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                                    break
                                }
                            }
                        } else if (arg.op == Arg.Op.NUMBER_BETWEEN) {
                            if (payload != null && payload.containsKey(arg.key) && arg.value is Map<*, *>) {
                                val values = arg.value as Map<String, Double>
                                val lower = values["lower"]!!.toInt()
                                val upper = values["upper"]!!.toInt()
                                val payloadValue = payload[arg.key]!!.toInt()

                                if (payloadValue > lower && payloadValue < upper) {
                                    conditionsMatchPayload = logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                                    break
                                }
                            }
                        }
                    }
                    if (conditionsMatchPayload) {
                        return true
                    } else {
                        val text = "Campaign [" + swrveCampaign.id + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger."
                        logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
                    }
                } else if (Conditions.Op.CONTAINS == conditions.op) {
                    if (payload != null && payload.containsKey(conditions.key) && payload[conditions.key]!!.lowercase().contains((conditions.value.toString().lowercase()))) {
                        return logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                    } else {
                        val text = "Campaign [" + swrveCampaign.id + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger."
                        logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
                        continue
                    }
                } else if (Conditions.Op.EQ == conditions.op) {
                    if (payload != null && payload.containsKey(conditions.key) && payload[conditions.key].equals(conditions.value as String, ignoreCase = true)) {
                        return logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                    } else {
                        val text = "Campaign [" + swrveCampaign.id + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger."
                        logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
                        continue
                    }
                } else if (Conditions.Op.NUMBER_GT == conditions.op) {
                    if (payload != null && payload.containsKey(conditions.key)) {
                        val value = conditions.value as Double
                        val valueInteger = value.toInt()
                        if (payload[conditions.key]!!.toInt() > valueInteger) {
                            return logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                        } else {
                            continue
                        }
                    } else {
                        val text = "Campaign [" + swrveCampaign.id + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger."
                        logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
                        continue
                    }
                } else if (Conditions.Op.NUMBER_LT == conditions.op) {
                    if (payload != null && payload.containsKey(conditions.key)) {
                        val value = conditions.value as Double
                        val valueInteger = value.toInt()
                        if (payload[conditions.key]!!.toInt() < valueInteger) {
                            return logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                        } else {
                            continue
                        }
                    } else {
                        val text = "Campaign [" + swrveCampaign.id + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger."
                        logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
                        continue
                    }
                } else if (Conditions.Op.NUMBER_EQ == conditions.op) {
                    if (payload != null && payload.containsKey(conditions.key)) {
                        val value = conditions.value as Double
                        val valueInteger = value.toInt()
                        if (payload[conditions.key]!!.toInt() == valueInteger) {
                            return logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                        } else {
                            continue
                        }
                    } else {
                        val text = "Campaign [" + swrveCampaign.id + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger."
                        logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
                        continue
                    }
                } else if (Conditions.Op.NUMBER_NOT_BETWEEN == conditions.op) {
                    if (payload != null && payload.containsKey(conditions.key)) {
                        val value = conditions.value
                        if (value is Map<*, *>) {
                            val values = value as Map<String, Double>
                            val lower = values["lower"]!!.toInt()
                            val upper = values["upper"]!!.toInt()
                            val payloadValue = payload[conditions.key]!!.toInt()

                            if (payloadValue < lower || payloadValue > upper) {
                                return logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                            } else {
                                continue
                            }
                        } else {
                            continue
                        }
                    } else {
                        val text = "Campaign [" + swrveCampaign.id + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger."
                        logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
                        continue
                    }
                } else if (Conditions.Op.NUMBER_BETWEEN == conditions.op) {
                    if (payload != null && payload.containsKey(conditions.key)) {
                        val value = conditions.value
                        if (value is Map<*, *>) {
                            val values = value as Map<String, Double>
                            val lower = values["lower"]!!.toInt()
                            val upper = values["upper"]!!.toInt()
                            val payloadValue = payload[conditions.key]!!.toInt()

                            if (payloadValue > lower && payloadValue < upper) {
                                return logAndAddReason(swrveCampaign, true, qaCampaignInfoMap, eventName, trigger, payload)
                            } else {
                                continue
                            }
                        } else {
                            continue
                        }
                    } else {
                        val text = "Campaign [" + swrveCampaign.id + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger."
                        logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
                        continue
                    }
                }
            } else {
                val text = "Campaign [" + swrveCampaign.id + "], Trigger [" + trigger + "], does not match eventName[" + eventName + "] & payload[" + payload + "]. Skipping this trigger."
                logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
                continue
            }
        }

        return false
    }

    private fun isTooSoonToShowMessageAfterLaunch(swrveCampaign: SwrveBaseCampaign, now: Date): Boolean {
        return now.before(swrveCampaign.showMessagesAfterLaunch)
    }

    private fun isTooSoonToShowMessageAfterDelay(swrveCampaign: SwrveBaseCampaign, now: Date): Boolean {
        val swrveCampaignState = swrveCampaign.saveableState
        return (swrveCampaignState.showMessagesAfterDelay != null && now.before(swrveCampaignState.showMessagesAfterDelay))
    }

    fun isCampaignActive(swrveCampaign: SwrveBaseCampaign, now: Date, qaCampaignInfoMap: MutableMap<Int?, QaCampaignInfo?>): Boolean {
        val startDate = swrveCampaign.startDate // evaluate start date
        if (startDate.after(now)) {
            val startDateLog = getLogDate(startDate, swrveCampaign.timezoneType)
            val nowLog = getLogDate(now, swrveCampaign.timezoneType)
            val text = "Campaign ${swrveCampaign.id} has not started yet. Start:$startDateLog TimezoneType:${swrveCampaign.timezoneType} Now:$nowLog"
            logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
            return false
        }
        val endDate = swrveCampaign.endDate // evaluate end date
        if (endDate.before(now)) {
            val endDateLog = getLogDate(endDate, swrveCampaign.timezoneType)
            val nowLog = getLogDate(now, swrveCampaign.timezoneType)
            val text = "Campaign ${swrveCampaign.id} has finished. End:$endDateLog TimezoneType:${swrveCampaign.timezoneType} Now:$nowLog"
            logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
            return false
        }

        swrveCampaign.blackoutDates?.forEach { blackoutDate ->
            val from = SwrveUtils.parseIso8601Date(blackoutDate.from, swrveCampaign.timezoneType)
            val to = SwrveUtils.parseIso8601Date(blackoutDate.to, swrveCampaign.timezoneType)
            if (now.after(from) && now.before(to)) {
                val fromLog = getLogDate(from, swrveCampaign.timezoneType)
                val toLog = getLogDate(to, swrveCampaign.timezoneType)
                val nowLog = getLogDate(now, swrveCampaign.timezoneType)
                val text = "Campaign ${swrveCampaign.id} is in blackout period. Blackout from:$fromLog to:$toLog TimezoneType:${swrveCampaign.timezoneType} Now:$nowLog"
                logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
                return false // exit early by returning false as soon as a blackout period is found
            }
        }

        if (!hasActiveTimeInterval(swrveCampaign, now)) {
            val nowLog = getLogDate(now, swrveCampaign.timezoneType)
            val text = "Campaign ${swrveCampaign.id} is outside active interval time. TimezoneType:${swrveCampaign.timezoneType} Now:$nowLog"
            logAndAddReason(swrveCampaign, text, false, qaCampaignInfoMap)
            return false
        }

        return true
    }

    private fun hasActiveTimeInterval(swrveCampaign: SwrveBaseCampaign, now: Date): Boolean {
        if (swrveCampaign.intervalTimes == null || swrveCampaign.intervalTimes.isEmpty()) {
            return true // no interval times set so always return true
        }
        swrveCampaign.intervalTimes.forEach { intervalTime ->
            val fromSeconds = getSecondsSinceMidnight(intervalTime.from)
            val toSeconds = getSecondsSinceMidnight(intervalTime.to)
            val nowSeconds = getSecondsSinceMidnight(now, swrveCampaign.timezoneType)
            if (nowSeconds in fromSeconds..toSeconds) {
                return true
            }
        }
        return false // no interval times matched so return false
    }

    private fun getSecondsSinceMidnight(time: String): Int {
        val timeParts = time.split(":") // Time must be in the format HH:mm:ss
        val hours = timeParts[0].toInt()
        val minutes = timeParts[1].toInt()
        val seconds = timeParts[2].toInt()
        return hours * 3600 + minutes * 60 + seconds
    }

    private fun getSecondsSinceMidnight(now: Date, timezoneType: SwrveTimezoneType): Int {
        val timezone = when (timezoneType) {
            SwrveTimezoneType.GLOBAL -> TimeZone.getTimeZone("UTC")
            SwrveTimezoneType.LOCAL -> TimeZone.getDefault()
        }
        val calendar = Calendar.getInstance(timezone).apply {
            time = now
        }
        return calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND)
    }

    private fun hasShowTooManyMessagesAlready(): Boolean {
        return messagesLeftToShow <= 0
    }

    private fun isTooSoonToShowMessageAfterLaunch(now: Date): Boolean {
        return now.before(showMessagesAfterLaunch)
    }

    private fun isTooSoonToShowMessageAfterDelay(now: Date): Boolean {
        if (showMessagesAfterDelay == null) {
            return false
        }
        return now.before(showMessagesAfterDelay)
    }

    private fun logAndAddReason(swrveCampaign: SwrveBaseCampaign?, text: String, displayed: Boolean, qaCampaignInfoMap: MutableMap<Int?, QaCampaignInfo?>?) {
        if (QaUser.isLoggingEnabled() && swrveCampaign != null && qaCampaignInfoMap != null) {
            if (swrveCampaign is SwrveInAppCampaign) {
                val variantId = swrveCampaign.variantId
                qaCampaignInfoMap[swrveCampaign.getId()] = QaCampaignInfo(swrveCampaign.getId().toLong(), variantId.toLong(), QaCampaignInfo.CAMPAIGN_TYPE.IAM, displayed, text)
            }
        }

        SwrveLogger.i(text)
        if (!QaUser.isLoggingEnabled()) {
            return
        }
    }

    private fun logAndAddReason(campaign: SwrveBaseCampaign, displayed: Boolean, qaInfo: MutableMap<Int?, QaCampaignInfo?>, event: String, trigger: Trigger, payload: Map<String?, String>?): Boolean {
        val text = if (!displayed) {
            "Campaign [" + campaign.id + "], Trigger [" + trigger + "], does not match eventName[" + event + "] & payload[" + payload + "]. Skipping this trigger."
        } else {
            "Campaign [" + campaign.id + "], Trigger [" + trigger + "], matches eventName[" + event + "] & payload[" + payload + "]."
        }
        logAndAddReason(campaign, text, displayed, qaInfo)
        return displayed
    }

    private fun getLogDate(date: Date, timezoneType: SwrveTimezoneType): String {
        val timezone = when (timezoneType) {
            SwrveTimezoneType.GLOBAL -> TimeZone.getTimeZone("UTC")
            SwrveTimezoneType.LOCAL -> TimeZone.getDefault()
        }
        dateFormat.timeZone = timezone
        val calendar = Calendar.getInstance(timezone).apply {
            time = date
        }
        return dateFormat.format(calendar.time)
    }

    private fun noMessagesWereShown(event: String, eventPayload: Map<String, String>?, reason: String) {
        SwrveLogger.i("Not showing message for %s: %s", event, reason)
        QaUser.campaignsAppRuleTriggered(event, eventPayload, reason)
    }

    // Ensures a new message cannot be shown until now + minDelayBetweenMessage
    fun setMessageMinDelayThrottle(now: Date?) {
        this.showMessagesAfterDelay = SwrveHelper.addTimeInterval(now, this.minDelayBetweenMessage, Calendar.SECOND)
    }
}