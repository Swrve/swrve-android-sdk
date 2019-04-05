package com.swrve.sdk;

import com.swrve.sdk.conversations.engine.model.ChoiceInputResponse;
import com.swrve.sdk.conversations.engine.model.UserInputResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SwrveConversationEventHelper {

    protected ISwrveConversationSDK swrveConversationSDK;

    protected static Map<String,String> customPayload;

    public static Map<String, String> getCustomPayload() {
        return customPayload;
    }

    private static boolean validatePayloadKeys(Map payload) {
        if (payload == null) return true;

        HashSet unionKeys = new HashSet<>(payload.keySet());
        HashSet restrictedSwrveKeys = new HashSet();
        restrictedSwrveKeys.add("event");
        restrictedSwrveKeys.add("to");
        restrictedSwrveKeys.add("page");
        restrictedSwrveKeys.add("conversation");
        restrictedSwrveKeys.add("control");
        restrictedSwrveKeys.add("fragment");
        restrictedSwrveKeys.add("result");
        restrictedSwrveKeys.add("name");
        restrictedSwrveKeys.add("id");

        unionKeys.addAll(restrictedSwrveKeys);
        return (unionKeys.size() == payload.size() + restrictedSwrveKeys.size());
    }

    public static void setCustomPayload(Map<String, String> customPayload) {

        if (customPayload == null) {
            SwrveLogger.d("Swrve removing custom payload");
            SwrveConversationEventHelper.customPayload = customPayload;
            return;
        }

        if (customPayload.size() > 5) {
            SwrveLogger.e("Swrve custom payload rejected, attempted to add more than 5 key pair values");
            return;
        }

        if (!(validatePayloadKeys(customPayload))) {
            SwrveLogger.e("Swrve custom payload rejected, attempted to add a key which is already reserved for Swrve internal events");
            return;
        }

        SwrveConversationEventHelper.customPayload = customPayload;
    }

    public SwrveConversationEventHelper() {
        this.swrveConversationSDK = (ISwrveConversationSDK) SwrveCommon.getInstance();
    }

    protected String getEventForConversation(SwrveBaseConversation conversation, String suffix) {
        return "Swrve.Conversations.Conversation-" + conversation.getId() + "." + suffix;
    }

    protected void queueEvent(SwrveBaseConversation conversation, String key, String page, Map<String, String> payload) {
        if (conversation != null && swrveConversationSDK != null) {
            String eventParamName = getEventForConversation(conversation, key);
            swrveConversationSDK.queueConversationEvent(eventParamName, key, page, conversation.getId(), payload);
        }
    }

    protected void queueEvent(SwrveBaseConversation conversation, String eventName, String page) {
        queueEvent(conversation, eventName, page, null);
    }

    protected void queueEventPageAction(SwrveBaseConversation conversation, String key, String fromPageTag, String actionKey, String toActionTag) {
        try {
            Map<String, String> payload = null;
            if ((actionKey != null) && (toActionTag != null)) {
                payload = new HashMap<>();
                payload.put(actionKey, toActionTag);
            }

            queueEvent(conversation, key, fromPageTag, payload);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in SwrveConversationSDK", e);
        }
    }

    protected void queueEventPageAction(SwrveBaseConversation conversation, String key, String fromPageTag) {
        queueEventPageAction(conversation, key, fromPageTag, null, null);
    }

    public void conversationPageWasViewedByUser(SwrveBaseConversation conversation, String pageTag) {
        queueEventPageAction(conversation, "impression", pageTag);
    }

    public void conversationWasStartedByUser(SwrveBaseConversation conversation, String pageTag) {
        queueEventPageAction(conversation, "start", pageTag);
    }

    public void conversationCallActionCalledByUser(SwrveBaseConversation conversation, String fromPageTag, String toActionTag) {
        queueEventPageAction(conversation, "call", fromPageTag, "control", toActionTag);
    }

    public void conversationLinkVisitActionCalledByUser(SwrveBaseConversation conversation, String fromPageTag, String toActionTag) {
        queueEventPageAction(conversation, "visit", fromPageTag, "control", toActionTag);
    }

    public void conversationDeeplinkActionCalledByUser(SwrveBaseConversation conversation, String fromPageTag, String toActionTag) {
        queueEventPageAction(conversation, "deeplink", fromPageTag, "control", toActionTag);
    }

    public void conversationWasFinishedByUser(SwrveBaseConversation conversation, String endPageTag, String endControlTag) {
        queueEventPageAction(conversation, "done", endPageTag, "control", endControlTag);
    }

    public void conversationWasCancelledByUser(SwrveBaseConversation conversation, String finalPageTag) {
        queueEventPageAction(conversation, "cancel", finalPageTag); //The current page the user is on when they cancelled
    }

    public void conversationEventsCommitedByUser(SwrveBaseConversation conversation, ArrayList<UserInputResult> userInteractions) {
        try {
            if (swrveConversationSDK != null) {
                for (UserInputResult userInteraction : userInteractions) {
                    Map<String, String> payload = new HashMap<>();
                    payload.put("fragment", userInteraction.getFragmentTag());

                    if (userInteraction.isSingleChoice()) {
                        ChoiceInputResponse response = (ChoiceInputResponse) userInteraction.getResult();
                        payload.put("result", response.getAnswerID());
                    } else if (userInteraction.isStarRating()) {
                        payload.put("result", String.valueOf(userInteraction.getResult()));
                    }

                    String key = userInteraction.getType();
                    String eventParamName = getEventForConversation(conversation, key);

                    if (UserInputResult.TYPE_STAR_RATING.equals(key) || UserInputResult.TYPE_SINGLE_CHOICE.equals(key) || UserInputResult.TYPE_VIDEO_PLAY.equals(key)) {
                        Map customPayload = getCustomPayload();
                        if (customPayload != null) {
                            payload.putAll(customPayload);
                        }
                    }

                    swrveConversationSDK.queueConversationEvent(eventParamName, key, userInteraction.getPageTag(), userInteraction.getConversationId(), payload);
                }
            } else {
                SwrveLogger.e("The SwrveConversationSDK is null, so cannot send events.");
            }
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in SwrveConversationSDK.", e);
        }
    }

    public void conversationTransitionedToOtherPage(SwrveBaseConversation conversation, String fromPageTag, String toPageTag, String controlTag) {
        try {
            Map<String, String> payload = new HashMap<>();

            payload.put("control", controlTag);
            payload.put("to", toPageTag); // The page the user ended on

            queueEvent(conversation, "navigation", fromPageTag, payload); // The page the user came on
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in SwrveConversationSDK", e);
        }
    }

    public void conversationEncounteredError(SwrveBaseConversation conversation, String currentPageTag, Exception e) {
        try {
            String eventName = getEventForConversation(conversation, "error");
            if (e != null) {
                SwrveLogger.e("Sending error conversation event: %s", e, eventName);
            } else {
                SwrveLogger.e("Sending error conversations event: (No Exception) %s");
            }

            queueEvent(conversation, "error", currentPageTag);
        } catch (Exception e2) {
            SwrveLogger.e("Exception thrown in SwrveConversationSDK", e2);
        }
    }

    public void sendQueuedEvents() {
        if (swrveConversationSDK != null) {
            swrveConversationSDK.sendQueuedEvents();
        }
    }
}
