package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

public class ConversationFullScreenVideoFrame extends FrameLayout {

    private WebChromeClient webCromeClient;

    public ConversationFullScreenVideoFrame(Context context) {
        super(context);
    }

    public ConversationFullScreenVideoFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void disableFullScreen() {
        if (webCromeClient != null) {
            webCromeClient.onHideCustomView();
            webCromeClient = null;
        }
        setVisibility(View.GONE);
    }

    public void setWebCromeClient(WebChromeClient webCromeClient) {
        this.webCromeClient = webCromeClient;
    }

    public void removeWebCromeClient(WebChromeClient webCromeClient) {
        if (this.webCromeClient == webCromeClient) {
            this.webCromeClient = null;
        }
    }
}
