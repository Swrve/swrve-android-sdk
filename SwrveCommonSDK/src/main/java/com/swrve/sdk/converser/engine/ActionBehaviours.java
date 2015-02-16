package com.swrve.sdk.converser.engine;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;

public abstract class ActionBehaviours {
    private static final String LOG_TAG = "ActionBehaviours";
    private Activity activity;
    private Context context;

    public ActionBehaviours(Activity a, Context c) {
        this.activity = a;
        this.context = c;
    }

    public void openDialer(Uri telUri, Activity activity) {
        Intent dialNum = new Intent(Intent.ACTION_DIAL, telUri);
        activity.startActivity(dialNum);
    }

    public void openPopupWebView(Uri uri, Activity activity, String referrer, String backMessage) {
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        final HashMap<String, String> extraHeaders = new HashMap<String, String>();
        extraHeaders.put("Referrer", referrer);
        WebView wv = new WebView(activity);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url, extraHeaders);
                return true;
            }
        });
        wv.loadUrl(uri.toString(), extraHeaders);
        alert.setView(wv);
        alert.setNegativeButton(backMessage, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        alert.show();
        Log.i("ConversationFragment Action", "Action is visit URL!");
    }

    public void openIntentWebView(Uri uri, Activity activity, String referrer) {
        Intent visitWebpage = new Intent(Intent.ACTION_VIEW, uri);
        Bundle bundle = new Bundle();
        bundle.putString("referrer", referrer);
        visitWebpage.putExtra(Browser.EXTRA_HEADERS, bundle);
        activity.startActivity(visitWebpage);
    }

    public void openDeepLink(Uri uri, Activity activity){
        String uriString = uri.toString();
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            activity.startActivity(intent);
        }catch(ActivityNotFoundException anfe){
            Log.e(LOG_TAG, "Could not launch activity for uri: " + uriString + ". Possibly badly formatted deep link", anfe);
            activity.finish();
        }
    }
}
