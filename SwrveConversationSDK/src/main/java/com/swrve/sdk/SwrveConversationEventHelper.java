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

    protected static String getEventForConversation(SwrveBaseConversation conversation) {
        return "Swrve.Conversations.Conversation-" + conversation.getId();
    }

    protected void queueEvent(SwrveBaseConversation conversation, String eventName, String page, Map<String, String> payload) {
        if (conversation != null && swrveConversationSDK != null) {
            swrveConversationSDK.queueConversationEvent(getEventForConversation(conversation), eventName, page, Integer.toString(conversation.getId()), payload);
        }
    }

    protected void queueEvent(SwrveBaseConversation conversation, String eventName, String page) {
        queueEvent(conversation, eventName, page, null);
    }

    protected void queueEventPageAction(SwrveBaseConversation conversation, String pageKey, String fromPageTag, String actionKey, String toActionTag) {
        try {
            Map<String, String> payload = null;
            if ((actionKey != null) && (toActionTag != null)) {
                payload = new HashMap<String, String>();
                payload.put(actionKey, toActionTag);
            }

            queueEvent(conversation, pageKey, fromPageTag, payload);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    protected void queueEventPageAction(SwrveBaseConversation conversation, String pageKey, String fromPageTag) {
        queueEventPageAction(conversation, pageKey, fromPageTag, null, null);
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
                    }

                    swrveConversationSDK.queueConversationEvent(
                            getEventForConversation(conversation),
                            userInteraction.getType(), userInteraction.getPageTag(), userInteraction.getConversationId(),
                            payload);
                }
            }
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    public void conversationTransitionedToOtherPage(SwrveBaseConversation conversation, String fromPageTag, String toPageTag, String controlTag) {
        try {
            Map<String, String> payload = new HashMap<String, String>();

            payload.put("control", controlTag);
            payload.put("to", toPageTag); // The page the user ended on

            queueEvent(conversation, "navigation", fromPageTag, payload); // The page the user came on
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    public void conversationEncounteredError(SwrveBaseConversation conversation, String currentPageTag, Exception e) {
        try {
            String viewEvent = getEventForConversation(conversation) + ".error";

            if (e != null) {
                SwrveLogger.e(LOG_TAG, "Sending error conversation event: " + viewEvent, e);
            } else {
                SwrveLogger.e(LOG_TAG, "Sending error conversations event: (No Exception) " + viewEvent);
            }

            queueEvent(conversation, "error", currentPageTag);
        } catch (Exception e2) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e2);
        }
    }
}
