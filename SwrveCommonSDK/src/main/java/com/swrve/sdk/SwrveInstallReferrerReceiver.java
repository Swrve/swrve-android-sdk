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

        String referrer = intent.getStringExtra("referrer");
        Log.i(SwrveImp.LOG_TAG, "Received referrer:" + referrer);

        if (referrer != null) {
            String decodedReferrer = "";
            try {
                decodedReferrer = URLDecoder.decode(referrer, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(SwrveImp.LOG_TAG, "Error decoding the referrer:" + referrer, e);
            }

            SharedPreferences.Editor prefs = context.getSharedPreferences(SwrveImp.SDK_PREFS_NAME, 0).edit();
            prefs.putString(SwrveImp.SWRVE_REFERRER_ID, decodedReferrer).commit();
        } else {
            Log.e(SwrveImp.LOG_TAG, "Referrer is null.");
        }
    }
}
