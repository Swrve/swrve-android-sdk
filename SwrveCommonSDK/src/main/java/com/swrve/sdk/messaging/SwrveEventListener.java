package com.swrve.sdk.messaging;

import android.content.Context;

import com.swrve.sdk.ISwrveEventListener;
import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;

import java.lang.ref.WeakReference;

/**
 * Default event listener. Will display an in-app message if available.
 */
public class SwrveEventListener implements ISwrveEventListener {

    private WeakReference<SwrveBase<?, ?>> talk;
    private ISwrveMessageListener messageListener;

    public SwrveEventListener(SwrveBase<?, ?> talk, ISwrveMessageListener messageListener) {
        this.talk = new WeakReference<SwrveBase<?, ?>>(talk);
        this.messageListener = messageListener;
    }

    @Override
    public void onEvent(String eventName) {
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
