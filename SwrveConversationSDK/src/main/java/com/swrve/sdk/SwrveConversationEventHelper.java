package com.swrve.sdk;

import com.swrve.sdk.conversations.engine.model.ChoiceInputResponse;
import com.swrve.sdk.conversations.engine.model.UserInputResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SwrveConversationEventHelper {
    private static final String LOG_TAG = "SwrveConversationSDK";

    private ISwrveConversationSDK swrveConversationSDK;

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
                payload = new HashMap<String, String>();
                payload.put(actionKey, toActionTag);
            }

            queueEvent(conversation, key, fromPageTag, payload);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in SwrveConversationSDK", e);
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
        queueEventPageAction(conversation, "visit", fromPageTag, "deeplink", toActionTag);
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
                    Map<String, String> payload = new HashMap<String, String>();
                    payload.put("fragment", userInteraction.getFragmentTag());

                    if (userInteraction.isSingleChoice()) {
                        ChoiceInputResponse response = (ChoiceInputResponse) userInteraction.getResult();
                        payload.put("result", response.getAnswerID());
                    } else if (userInteraction.isStarRating()) {
                        payload.put("result", String.valueOf(userInteraction.getResult()));
                    }

                    String key = userInteraction.getType();
                    String eventParamName = getEventForConversation(conversation, key);
                    swrveConversationSDK.queueConversationEvent(eventParamName, key, userInteraction.getPageTag(), userInteraction.getConversationId(), payload);
                }
            } else {
                SwrveLogger.e(LOG_TAG, "The SwrveConversationSDK is null, so cannot send events.");
            }
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in SwrveConversationSDK.", e);
        }
    }

    public void conversationTransitionedToOtherPage(SwrveBaseConversation conversation, String fromPageTag, String toPageTag, String controlTag) {
        try {
            Map<String, String> payload = new HashMap<String, String>();

            payload.put("control", controlTag);
            payload.put("to", toPageTag); // The page the user ended on

            queueEvent(conversation, "navigation", fromPageTag, payload); // The page the user came on
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in SwrveConversationSDK", e);
        }
    }

    public void conversationEncounteredError(SwrveBaseConversation conversation, String currentPageTag, Exception e) {
        try {
            String eventName = getEventForConversation(conversation, "error");
            if (e != null) {
                SwrveLogger.e(LOG_TAG, "Sending error conversation event: " + eventName, e);
            } else {
                SwrveLogger.e(LOG_TAG, "Sending error conversations event: (No Exception) " + eventName);
            }

            queueEvent(conversation, "error", currentPageTag);
        } catch (Exception e2) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in SwrveConversationSDK", e2);
        }
    }
}
