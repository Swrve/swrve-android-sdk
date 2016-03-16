package com.swrve.sdk.conversations.ui.video;

import android.content.Context;
import android.content.res.AssetManager;
import com.swrve.sdk.SwrveLogger;

import android.util.Log;
import android.view.View;

import com.swrve.sdk.SwrveHelper;

import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.ui.ConversationFullScreenVideoFrame;

import java.io.IOException;
import java.io.InputStream;

public class YoutubeVideoView extends WebVideoViewBase {
    public static final String VIDEO_ID_PLACEHOLDER = "VIDEO_ID_PLACEHOLDER";
    public static final String VIDEO_HEIGHT_PLACEHOLDER = "VIDEO_HEIGHT_PLACEHOLDER";

    public YoutubeVideoView(Context context, Content model, ConversationFullScreenVideoFrame fullScreenContainer) {
        super(context, model, fullScreenContainer);
    }

    protected void init(Content model) {
        String videoID = model.getYoutubeVideoId();
        String videoHeight = model.getHeight();

        String pageHtml = "";
        InputStream ims;

        AssetManager assetManager = getContext().getAssets();
        try {
            ims = assetManager.open("youtubeapi.html");
            pageHtml = SwrveHelper.readStringFromInputStream(ims);
        } catch (IOException e) {
            SwrveLogger.e(LOG_TAG, Log.getStackTraceString(e));
        }

        pageHtml = pageHtml.replaceAll(VIDEO_ID_PLACEHOLDER, videoID);
        pageHtml = pageHtml.replaceAll(VIDEO_HEIGHT_PLACEHOLDER, videoHeight);
        pageHtml = pageHtml + errorHtml;
        
        YoutubeVideoView.this.loadDataWithBaseURL(null, pageHtml, "text/html", "utf-8", null);
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
            SwrveLogger.i(LOG_TAG, "Stopping the Video!");
            this.stopLoading();
            // Load some blank data into the webview
            this.loadData("<p></p>", "text/html", "utf8");
        } else {
            // Reload video
            this.stopLoading();
            init(this.model);
        }
    }
}
