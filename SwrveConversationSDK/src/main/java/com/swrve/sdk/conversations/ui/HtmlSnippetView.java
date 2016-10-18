package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.webkit.WebView;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

import java.io.File;
import java.io.InputStream;
import java.text.MessageFormat;

public class HtmlSnippetView extends WebView {

    private static final String FONT_FACE_TEMPLATE = "@font-face '{' font-family: ''{0}''; src: url(''{1}'');'}'";

    private File cacheDir;
    private String css = "";
    private String fontFace = "";

    public HtmlSnippetView(Context context, Content model, File cacheDir) {
        super(context);
        this.cacheDir = cacheDir;
        initCss();
        initFont(model);
        initHtml(model);
    }

    private void initCss() {
        try {
            InputStream is = getContext().getAssets().open("swrve__css_defaults.css");
            if (is != null) {
                css = SwrveHelper.readStringFromInputStream(is);
            }
        } catch (Exception e) {
            SwrveLogger.e("Error init'ing default css", e);
        }
        if (SwrveHelper.isNullOrEmpty(css)) {
            css = "";
        }
    }

    private void initFont(Content model) {
        if (model.getStyle() == null) {
            return;
        }
        ConversationStyle style = model.getStyle();
        if (SwrveHelper.isNotNullOrEmpty(style.getFontFile())) {
            File fontFile = new File(cacheDir, style.getFontFile());
            if (fontFile.exists()) {
                fontFace = MessageFormat.format(FONT_FACE_TEMPLATE, model.getStyle().getFontFamily(), "file://" + fontFile.getAbsolutePath());
            }
        }
    }

    protected void initHtml(Content model) {
        String safeHtml = "<html><head><style>" + fontFace + css + "</style></head><body><div style='max-width:100%; overflow: hidden; word-wrap: break-word;'>" + model.getValue() + "</div></body></html>";
        this.loadDataWithBaseURL(null, safeHtml, "text/html", "UTF-8", null);
    }

}
