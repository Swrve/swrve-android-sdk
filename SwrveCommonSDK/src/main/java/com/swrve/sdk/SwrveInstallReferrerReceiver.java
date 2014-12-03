package com.swrve.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;


public class SwrveInstallReferrerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String referrer = intent.getStringExtra(SwrveImp.REFERRER);
        Log.i(SwrveImp.LOG_TAG, "Received INSTALL_REFERRER broadcast with referrer:" + referrer);

        if (!SwrveHelper.isNullOrEmpty(referrer)) {
            try {
                String decodedReferrer = URLDecoder.decode(referrer, "UTF-8");
                SharedPreferences.Editor prefs = context.getSharedPreferences(SwrveImp.SDK_PREFS_NAME, 0).edit();
                prefs.putString(SwrveImp.SWRVE_REFERRER_ID, decodedReferrer).commit();
            } catch (UnsupportedEncodingException e) {
                Log.e(SwrveImp.LOG_TAG, "Error decoding the referrer:" + referrer, e);
            }
        }
    }
}
