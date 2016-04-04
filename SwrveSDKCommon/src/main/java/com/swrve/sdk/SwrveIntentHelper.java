package com.swrve.sdk;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;

public final class SwrveIntentHelper {

    private static final String LOG_TAG = "SwrveSDK";

    public static void openDialer(Uri telUri, Activity activity) {
        Intent dialNum = new Intent(Intent.ACTION_DIAL, telUri);
        activity.startActivity(dialNum);
    }

    public static void openIntentWebView(Uri uri, Activity activity, String referrer) {
        Intent visitWebpage = new Intent(Intent.ACTION_VIEW, uri);
        Bundle bundle = new Bundle();
        bundle.putString("referrer", referrer);
        visitWebpage.putExtra(Browser.EXTRA_HEADERS, bundle);
        activity.startActivity(visitWebpage);
    }

    public static void openDeepLink(Context context, String uriString){
        openDeepLink(context, uriString, null);
    }

    public static void openDeepLink(Context context, String uriString, Bundle extras){
        Uri uri = Uri.parse(uriString);
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(uri);
        if(extras!=null) {
            intent.putExtras(extras);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            SwrveLogger.e(LOG_TAG, "Could not launch activity for uri: " + uriString + ". Possibly badly formatted deep link", ex);
        }
    }
}
