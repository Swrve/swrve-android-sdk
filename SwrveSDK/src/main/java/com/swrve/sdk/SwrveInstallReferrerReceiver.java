package com.swrve.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;


public class SwrveInstallReferrerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String referrer = intent.getStringExtra(SwrveImp.REFERRER);
        SwrveLogger.i("Received INSTALL_REFERRER broadcast with referrer:%s", referrer);

        if (!SwrveHelper.isNullOrEmpty(referrer)) {
            try {
                String decodedReferrer = URLDecoder.decode(referrer, "UTF-8");
                SharedPreferences.Editor prefs = context.getSharedPreferences(ISwrveCommon.SDK_PREFS_NAME, 0).edit();
                prefs.putString(ISwrveCommon.SDK_PREFS_REFERRER_ID, decodedReferrer).apply();
            } catch (UnsupportedEncodingException e) {
                SwrveLogger.e("Error decoding the referrer:" + referrer, e);
            }
        }
    }
}
