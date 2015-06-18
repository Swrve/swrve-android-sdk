package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

public class ConversationFullScreenVideoFrame extends FrameLayout {

    private WebChromeClient.CustomViewCallback callback;

    public ConversationFullScreenVideoFrame(Context context) {
        super(context);
    }

    public ConversationFullScreenVideoFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCustomViewCallback(WebChromeClient.CustomViewCallback callback) {
        this.callback = callback;
    }

    public void disableFullScreen() {
        if (callback != null) {
            callback.onCustomViewHidden();
            callback = null;
        }
        setVisibility(View.GONE);
    }
}
