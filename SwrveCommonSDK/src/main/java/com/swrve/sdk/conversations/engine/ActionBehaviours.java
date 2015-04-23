package com.swrve.sdk.conversations.engine;

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

public class ActionBehaviours {
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
