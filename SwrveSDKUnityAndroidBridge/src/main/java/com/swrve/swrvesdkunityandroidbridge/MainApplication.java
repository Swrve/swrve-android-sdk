package com.swrve.swrvesdkunityandroidbridge;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.swrve.sdk.SwrveSDK;

public class MainApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        try
        {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;

            SwrveSDK.createInstance(
               this,
               bundle.getInt("swrve_app_id"),
               bundle.getString("swrve_api_key")
            );
        }
        catch(Exception exp)
        {
            Log.e("SwrveAndroidBridge", "Could not initialize the Swrve SDK", exp);
        }
    }
}
