package com.swrve.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;

public final class SwrveIntentHelper {

    public static void openDialer(Uri telUri, Activity activity) {
        Intent dialNum = new Intent(Intent.ACTION_VIEW, telUri);
        activity.startActivity(dialNum);
    }

    public static void openIntentWebView(Uri uri, Activity activity, String referrer) {
        Intent visitWebpage = new Intent(Intent.ACTION_VIEW, uri);
        Bundle bundle = new Bundle();
        bundle.putString("referrer", referrer);
        visitWebpage.putExtra(Browser.EXTRA_HEADERS, bundle);
        activity.startActivity(visitWebpage);
    }

    public static void openDeepLink(Context context, String uriString, Bundle extras) {
        try {
            if (SwrveCommon.getInstance() != null && SwrveCommon.getInstance().getSwrveDeeplinkListener() != null) {
                SwrveLogger.d("SwrveSDK: Passing to SwrveDeeplinkListener to open deeplink: %s", uriString);
                SwrveCommon.getInstance().getSwrveDeeplinkListener().handleDeeplink(context, uriString, extras);
                return;
            }

            SwrveLogger.d("SwrveSDK: Opening deeplink: %s", uriString);
            Uri uri = Uri.parse(uriString);
            Intent intent = new Intent(Intent.ACTION_VIEW).setData(uri);
            if (extras != null) {
                intent.putExtras(extras);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: could not open deeplink uri:%s", ex, uriString);
        }
    }
}
