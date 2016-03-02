package com.swrve.sdk.conversations;

import com.swrve.sdk.conversations.engine.model.UserInputResult;

import java.io.File;
import java.util.ArrayList;

/**
 * Used internally to interface the SwrveSDK (native and Unity) with conversations
 */
public interface ISwrveConversationsSDK {
    File getCacheDir();

    void conversationEventsCommitedByUser(SwrveCommonConversation swrveConversation, ArrayList<UserInputResult> userInputEvents);

    void conversationPageWasViewedByUser(SwrveCommonConversation swrveConversation, String pageTag);

    void conversationWasStartedByUser(SwrveCommonConversation swrveConversation, String startPageTag);

    void conversationWasFinishedByUser(SwrveCommonConversation swrveConversation, String endPageTag, String endControlTag);

    void conversationWasCancelledByUser(SwrveCommonConversation swrveConversation, String currentPageTag);

    void conversationEncounteredError(SwrveCommonConversation swrveConversation, String currentPageTag, Exception e);

    void conversationTransitionedToOtherPage(SwrveCommonConversation swrveConversation, String currentPageTag, String targetPageTag, String controlTag);

    void conversationLinkVisitActionCalledByUser(SwrveCommonConversation swrveConversation, String currentPageTag, String tag);

    void conversationDeeplinkActionCalledByUser(SwrveCommonConversation swrveConversation, String currentPageTag, String tag);

    void conversationCallActionCalledByUser(SwrveCommonConversation swrveConversation, String currentPageTag, String tag);
}
