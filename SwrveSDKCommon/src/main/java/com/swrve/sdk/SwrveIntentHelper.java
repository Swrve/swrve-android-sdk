package com.swrve.sdk;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.List;

public final class SwrveIntentHelper {

    private static final String LOG_TAG = "SwrveSDK";

    public static Intent convertDeeplinkToIntent(final PackageManager packageManager, String deeplink) {
        Intent intentDeeplink = new Intent(Intent.ACTION_VIEW);
        intentDeeplink.setData(Uri.parse(deeplink));
        if(SwrveIntentHelper.canOpenIntent(packageManager, intentDeeplink)) {
            return intentDeeplink;
        } else {
            return null;
        }
    }

    private static boolean canOpenIntent(final PackageManager packageManager, Intent intent) {
        return isIntentAvailable(packageManager, intent) || isActivityAvailable(packageManager, intent);
    }

    private static boolean isIntentAvailable(final PackageManager packageManager, Intent intent) {
        List resolveInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo.size() > 0) {
            return true;
        }
        SwrveLogger.e(LOG_TAG, "SwrveHelper.isIntentAvailable is false. Intent:" + intent);
        return false;
    }

    private static boolean isActivityAvailable(final PackageManager packageManager, Intent intent) {
        ComponentName componentName = intent.resolveActivity(packageManager);
        if(componentName == null) {
            SwrveLogger.e(LOG_TAG, "SwrveHelper.isActivityAvailable is false. Intent:" + intent);
            return false;
        }
        return true;
    }
}
