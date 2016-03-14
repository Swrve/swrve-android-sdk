package com.swrve.sdk;

import java.util.Map;

/**
 * Used internally to interface the SwrveSDK (native and Unity) with conversations
 */
interface ISwrveConversationSDK {
    int CONVERSATION_VERSION = 3;
    void queueConversationEvent(String viewEvent, String eventName, String page, String conversationId, Map<String, String> payload);
}
