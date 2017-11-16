package com.swrve.sdk.conversations.ui.video;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.ui.ConversationFullScreenVideoFrame;

import java.util.Locale;

public abstract class WebVideoViewBase extends WebView {
    protected static final String PLAYER_VIDEO_VIMEO = "vimeo";
    protected static final String PLAYER_VIDEO_YOUTUBE = "youtube";
    protected String url;
    protected int height;
    // This is a link to the video to be displayed in case the video is not visible to the customers
    protected String errorHtml;
    // This is the final output. This will be the tags and information that is rendered in the application
    protected String videoHtml;
    protected Content model;
    protected ConversationFullScreenVideoFrame fullScreenContainer;

    public WebVideoViewBase(Context context, Content model, ConversationFullScreenVideoFrame fullScreenContainer) {
        super(context);
        this.model = model;
        this.fullScreenContainer = fullScreenContainer;
        url = model.getValue();
        height = Integer.parseInt(model.getHeight());
        if (height <= 0) {
            height = 220;
        }

        String aStyle = "style='font-size: 0.6875em; color: #666; width:100%;'";
        String pStyle = "style='text-align:center; margin-top:8px'";

        errorHtml = ("<p " + pStyle + ">") + ("<a " + aStyle + " href='" + url.toString() + "'>") + ("Can't see the video?</a>");

        if (SwrveHelper.isNullOrEmpty(url)) {
            Toast.makeText(this.getContext(), "Unknown Video Player Detected", Toast.LENGTH_SHORT).show();
            videoHtml = "<p>Sorry, a malformed URL was detected. This video cannot be played.</p> ";
        } else if (url.toLowerCase(Locale.ENGLISH).contains(PLAYER_VIDEO_YOUTUBE) || url.toLowerCase(Locale.ENGLISH).contains(PLAYER_VIDEO_VIMEO)) {
            videoHtml = "<iframe type='text/html' width='100%' height='" + height + "' src=" + url + " frameborder='0' webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>" + errorHtml;
        } else {
            Toast.makeText(this.getContext(), "Unknown Video Player Detected", Toast.LENGTH_SHORT).show();
            videoHtml = errorHtml;
        }
    }

    protected class SwrveVideoWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Open links using an intent
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            getContext().startActivity(i);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            SwrveLogger.e("Could not display url: %s\n" +
                    "Error code: %s\n" +
                    "Message: %s",
                    failingUrl, Integer.toString(errorCode), description);
            String placeHolderHtml = "<div style=\"width: 100%; height: " + height + "px\"></div>";
            String pageHtml = "<html><body style=\"margin: 0; padding: 0;\">" + placeHolderHtml + errorHtml + "</body></html>";
            WebVideoViewBase.this.loadDataWithBaseURL(null, pageHtml, "text/html", "utf-8", null);
        }
    }

    protected class SwrveWebCromeClient extends WebChromeClient {
        private CustomViewCallback mCustomViewCallback;
        private View mView;

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
            mCustomViewCallback = callback;
            mView = view;

            fullScreenContainer.setWebCromeClient(this);
            fullScreenContainer.setVisibility(View.VISIBLE);
            fullScreenContainer.addView(view, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        @Override
        public void onHideCustomView() {
            if (mView != null) {
                mView = null;
                fullScreenContainer.removeWebCromeClient(this);
                fullScreenContainer.removeView(mView);
                mCustomViewCallback.onCustomViewHidden();
                mCustomViewCallback = null;
                fullScreenContainer.setVisibility(View.GONE);
            }
            super.onHideCustomView();
        }
    }
}
