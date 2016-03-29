package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;

import java.io.IOException;
import java.io.InputStream;

public class HtmlSnippetView extends WebView implements IConversationContent {
    private Content model;
    private static String DEFAULT_CSS = null;

    public HtmlSnippetView(Context context, Content model) {
        super(context);

        if (DEFAULT_CSS == null) {
            // Load CSS reset and default values
            try {
                InputStream is = context.getAssets().open("swrve__css_defaults.css");
                if (is != null) {
                    DEFAULT_CSS = SwrveHelper.readStringFromInputStream(is);
                }
            } catch (IOException e) {
                SwrveLogger.e(Log.getStackTraceString(e));
            }
            if (SwrveHelper.isNullOrEmpty(DEFAULT_CSS)) {
                DEFAULT_CSS = "";
            }
        }
        init(model);
    }

    protected void init(Content model) {
        this.model = model;
        String safeHtml = "<html><head><style>" + DEFAULT_CSS + "</style></head><body><div style='max-width:100%; overflow: hidden; word-wrap: break-word;'>" + model.getValue() + "</div></body></html>";
        this.loadDataWithBaseURL(null, safeHtml, "text/html", "UTF-8", null);
    }

    @Override
    public ConversationAtom getModel() {
        return model;
    }
}
