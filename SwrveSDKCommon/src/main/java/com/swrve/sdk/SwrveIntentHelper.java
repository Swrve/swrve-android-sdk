package com.swrve.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;

import java.util.List;

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
            Intent intent = getDeepLinkIntent(uriString, extras);
            context.startActivity(intent);
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: could not open deeplink uri:%s", ex, uriString);
        }
    }


    public static Intent getDeepLinkIntent(String uriString, Bundle extras) {
        Intent intent = null;
        try {
            Uri uri = Uri.parse(uriString);
            intent = new Intent(Intent.ACTION_VIEW).setData(uri);
            if (extras != null) {
                intent.putExtras(extras);
            }
            intent.addFlags(getDefaultIntentFlags());
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: could not get deeplink intent uri:%s", ex, uriString);
        }
        return intent;
    }

    public static int getDefaultIntentFlags() {
        return Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION;
    }

    @SuppressLint("QueryPermissionsNeeded")
    public static boolean canOpenIntentInternally(Context context, Intent intent) {
        try {
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> activityResolveInfoList = packageManager.queryIntentActivities(intent, 0);
            String packageName = context.getPackageName();
            for (ResolveInfo resolveInfo : activityResolveInfoList) {
                if (resolveInfo.activityInfo.packageName.equals(packageName)) {
                    return true;
                }
            }
            List<ResolveInfo> receiverResolveInfoList = packageManager.queryBroadcastReceivers(intent, 0);
            for (ResolveInfo resolveInfo : receiverResolveInfoList) {
                if (resolveInfo.activityInfo.packageName.equals(packageName)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: could not check if intent can be opened", ex);
        }
        return false;
    }
}
