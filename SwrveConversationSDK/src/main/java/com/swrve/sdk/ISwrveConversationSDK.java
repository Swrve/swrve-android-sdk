package com.swrve.sdk;

import java.util.Map;

/**
 * Used internally to interface the SwrveSDK (native and Unity) with conversations
 */
interface ISwrveConversationSDK {
    int CONVERSATION_VERSION = 3;
    void queueConversationEvent(String eventParamName, String eventPayloadName, String page, int conversationId, Map<String, String> payload);
}
