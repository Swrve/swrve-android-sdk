package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;

public class HtmlVideoView extends WebView implements ConversationContent {
    private static final String LOG_TAG = "SwrveSDK";
    public static final String PLAYER_VIDEO_VIMEO = "vimeo";
    public static final String PLAYER_VIDEO_YOUTUBE = "youtube";
    private static final String EXTERNAL_MARKER = "&swrveexternal";
    private String url;
    private int height;
    // This is a link to the video to be displayed in case the video is not visible to the customers
    private String errorHtml;
    // This is the final output. This will be the tags and information that is rendered in the application
    private String videoHtml;
    private Content model;
    private ConversationFullScreenVideoFrame fullScreenContainer;

    public HtmlVideoView(Context context, Content model, ConversationFullScreenVideoFrame fullScreenContainer) {
        super(context);
        this.model = model;
        this.fullScreenContainer = fullScreenContainer;
    }

    protected void init(Content model) {
        url = model.getValue();
        height = Integer.parseInt(model.getHeight());
        if (height <= 0) {
            height = 220;
        }

        String aStyle = "style='font-size: 0.6875em; color: #666; width:100%;'";
        String pStyle = "style='text-align:center; margin-top:8px'";

        errorHtml = ("<p " + pStyle + ">") + ("<a " + aStyle + " href='" + url.toString() + EXTERNAL_MARKER + "'>") + ("Can't see the video?</a>");

        if (SwrveHelper.isNullOrEmpty(url)) {
            Toast.makeText(this.getContext(), "Unknown Video Player Detected", Toast.LENGTH_SHORT).show();
            videoHtml = "<p>Sorry, a malformed URL was detected. This video cannot be played.</p> ";
        } else if (url.toLowerCase().contains(PLAYER_VIDEO_YOUTUBE) || url.toLowerCase().contains(PLAYER_VIDEO_VIMEO)) {
            videoHtml = "<iframe type='text/html' width='100%' height='" + height + "' src=" + url + " frameborder='0' webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>" + errorHtml;
        } else {
            Toast.makeText(this.getContext(), "Unknown Video Player Detected", Toast.LENGTH_SHORT).show();
            videoHtml = errorHtml;
        }

        String pageHtml = "<html><body style=\"margin: 0; padding: 0\">" + videoHtml + "</body></html>";
        // Setup the WebView for video playback
        // Do not remove the WebChrome or WebView clients as they will stop video working on Android 4.2.2.
        this.setWebChromeClient(new SwrveWebCromeClient());
        this.setWebViewClient(new SwrveVideoWebViewClient());
        this.getSettings().setJavaScriptEnabled(true);
        this.loadDataWithBaseURL(null, pageHtml, "text/html", "utf-8", null);
    }

    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);

        if (visibility != View.VISIBLE) {
            Log.i(LOG_TAG, "Stopping the Video!");
            this.stopLoading();
            // Load some blank data into the webview
            this.loadData("<p></p>", "text/html", "utf8");
        } else {
            // Reload video
            this.stopLoading();
            init(this.model);
        }
    }

    @Override
    public ConversationAtom getModel() {
        return model;
    }

    private class SwrveVideoWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Check if link has to be opened with an intent
            if(url.contains(EXTERNAL_MARKER)){
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url.replace(EXTERNAL_MARKER, "")));
                getContext().startActivity(i);
                return true;
            }
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.e("SwrveVideoWebViewClient", "Could not display url: " + failingUrl + "\n" +
                            "Error code: " + Integer.toString(errorCode) + "\n" +
                            "Message: " + description
            );
            showWebviewError();
        }
    }

    public void showWebviewError(){
        String placeHolderHtml = "<div style=\"width: 100%; height: " + height + "px\"></div>";
        String pageHtml = "<html><body style=\"margin: 0; padding: 0;\">" + placeHolderHtml + errorHtml + "</body></html>";
        HtmlVideoView.this.loadDataWithBaseURL(null, pageHtml, "text/html", "utf-8", null);
    }

    private class SwrveWebCromeClient extends WebChromeClient {
        private CustomViewCallback mCustomViewCallback;
        private View mView;

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (consoleMessage.messageLevel().equals(ConsoleMessage.MessageLevel.ERROR)){
                showWebviewError();
            }
            return true;
        }

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
