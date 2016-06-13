package com.swrve.sdk;

import android.content.Context;

import com.swrve.sdk.conversations.ISwrveConversationListener;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.messaging.ISwrveMessageListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveOrientation;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Default event listener. Will display an in-app message if available.
 */
public class SwrveEventListener implements ISwrveEventListener {

    private WeakReference<SwrveBase<?, ?>> talk;
    private ISwrveMessageListener messageListener;
    private ISwrveConversationListener conversationListener;

    public SwrveEventListener(SwrveBase<?, ?> talk, ISwrveMessageListener messageListener, ISwrveConversationListener conversationListener) {
        this.talk = new WeakReference<SwrveBase<?, ?>>(talk);
        this.messageListener = messageListener;
        this.conversationListener = conversationListener;
    }

    @Override
    public void onEvent(String eventName, Map<String, String> payload) {
        if (conversationListener != null && !SwrveHelper.isNullOrEmpty(eventName)) {
            SwrveBase<?, ?> talkRef = talk.get();
            if (talkRef != null && talkRef.getConfig().isTalkEnabled()) {
                SwrveConversation conversation = talkRef.getConversationForEvent(eventName, payload);
                if (conversation != null) {
                    conversationListener.onMessage(conversation);
                    return;
                }
            }
        }

        if (messageListener != null && !SwrveHelper.isNullOrEmpty(eventName)) {
            SwrveBase<?, ?> talkRef = talk.get();
            if (talkRef != null && talkRef.getConfig().isTalkEnabled()) {
                SwrveOrientation deviceOrientation = SwrveOrientation.Both;
                Context ctx = talkRef.getContext();
                if (ctx != null) {
                    deviceOrientation = SwrveOrientation.parse(ctx.getResources().getConfiguration().orientation);
                }
                SwrveMessage message = talkRef.getMessageForEvent(eventName, payload, deviceOrientation);
                if (message != null) {
                    messageListener.onMessage(message, true);
                }
            }
        }
    }
}
