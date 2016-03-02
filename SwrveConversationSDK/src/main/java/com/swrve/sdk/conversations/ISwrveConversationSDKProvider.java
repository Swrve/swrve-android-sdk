package com.swrve.sdk.conversations;

/**
 * Used internally to interface obtain the latest instance of the ISwrveConversationsSDK
 */
public interface ISwrveConversationSDKProvider {
    ISwrveConversationsSDK getInstance();
}
