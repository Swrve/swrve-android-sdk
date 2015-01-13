package com.swrve.sdk.messaging;

import android.content.Context;

import com.swrve.sdk.ISwrveEventListener;
import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.converser.ISwrveConversationListener;
import com.swrve.sdk.converser.SwrveConversation;

import java.lang.ref.WeakReference;

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
    public void onEvent(String eventName) {
        if (conversationListener != null && !SwrveHelper.isNullOrEmpty(eventName)) {
            SwrveBase<?, ?> talkRef = talk.get();
            if (talkRef != null && talkRef.getConfig().isTalkEnabled()) {
                SwrveOrientation deviceOrientation = SwrveOrientation.Both;
                Context ctx = talkRef.getContext();
                if (ctx != null) {
                    deviceOrientation = SwrveOrientation.parse(ctx.getResources().getConfiguration().orientation);
                }
                SwrveConversation conversation = talkRef.getConversationForEvent(eventName, deviceOrientation);
                if (conversation != null) {
                    conversationListener.onMessage(conversation, true);
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
                SwrveMessage message = talkRef.getMessageForEvent(eventName, deviceOrientation);
                if (message != null) {
                    messageListener.onMessage(message, true);
                }
            }
        }
    }
}
