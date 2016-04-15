package com.swrve.swrvesdkunityandroidbridge;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.swrve.sdk.ISwrve;
import com.swrve.sdk.SwrvePlot;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.SwrveSDKBase;
import com.swrve.sdk.config.SwrveConfig;
import com.unity3d.player.UnityPlayer;

import java.net.URL;

/**
 * Main helper class for the Swrve Unity Android SDK bridge
 */
public class SwrveAndroidHelper
{
    private static final String LOG_TAG = "SwrveUnityAndroid";
    private static LocationManager _locationManager;

    public static Activity GetUnityActivity() {
        return UnityPlayer.currentActivity;
    }

    public static String GetVersionName() {
        return GetVersionName(GetUnityActivity());
    }

    public static String GetVersionName(Activity activity) {
        if (null != getPackageInfo(activity)) {
            return getPackageInfo(activity).versionName;
        }
        return null;
    }

    public static String GetVersionCode() {
        return GetVersionCode(GetUnityActivity());
    }

    public static String GetVersionCode(Activity activity) {
        if (null != getPackageInfo(activity)) {
            return "" + getPackageInfo(activity).versionCode;
        }
        return null;
    }

    private static PackageInfo getPackageInfo(Activity activity) {
        try {
            return activity
                    .getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void CreateSwrveInstance(int appId, String apiKey) {
        CreateSwrveInstance(GetUnityActivity(), appId, apiKey);
    }

    public static void CreateSwrveInstance(Context context, int appId, String apiKey) {
        Log.d(LOG_TAG, "[" + SwrveAndroidHelper.class + ":CreateSwrveInstance] context: " + context + ", appId: " + appId + ", apiKey: " + apiKey);
        try {
            ISwrve swrve = SwrveSDK.createInstance(context, appId, apiKey);
            Log.d(LOG_TAG, "[" + SwrveAndroidHelper.class + ":CreateSwrveInstance] swrve: " + swrve.getClass());
        } catch (Exception exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }

    public static void SwapToSwrveView() {
        SwapToSwrveView(GetUnityActivity());
    }

    public static void SwapToSwrveView(Context context) {
        context.startActivity(new Intent(context, MainActivity.class));
    }

    public static void DoTheNativeStuff(int appId, String apiKey) {
        DoTheNativeStuff(GetUnityActivity(), appId, apiKey);
    }

    public static void DoTheNativeStuff(Activity activity, int appId, String apiKey) {
        Log.d("SeSwrve", "DoTheNativeStuff, SwrveSDKBase.getInstance " + SwrveSDKBase.getInstance());

        if (null == SwrveSDKBase.getInstance()) {
            try {
                SwrveConfig config = new SwrveConfig();
                config.setEventsUrl(new URL("http://featurestack17-api.swrve.com"));
                config.setContentUrl(new URL("http://featurestack17-content.swrve.com"));
                config.setHideToolbar(false);

                Log.d(
                        "SeSwrve",
                        "DoTheNativeStuff, " +
                        "SwrveConfig: " + config + ", " +
                        "appId: " + appId + ", " +
                        "apiKey: " + apiKey
                );

                SwrveSDK.createInstance(
                        activity,
                        appId,
                        apiKey,
                        config
                );
                SwrveSDK.onCreate(activity);
            } catch (Exception exp) {
                Log.e("SwrveAndroidBridge", "Could not initialize the Swrve SDK", exp);
            }

            Log.d("SeSwrve", "DoTheNativeStuff, SwrveSDKBase.getInstance " + SwrveSDKBase.getInstance());
        }
        Log.d("SeSwrve", "DoTheNativeStuff, _DonePlot" + _DonePlot);
        if (!_DonePlot) {
            CheckLocPermission(activity);
        }

        if (!_DoneLoc)
        {
            SetupLoc(activity);
        }
    }

    private static boolean _DonePlot;

    private static void CheckLocPermission(Activity activity) {
        if (HasLocPermission(activity)) {
            AfterLocPermission(activity);
        } else {
            final String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(activity, permissions, 124);
        }
    }

    private static void AfterLocPermission(Activity activity) {
        android.util.Log.d("SeSwrve", "AfterLocPermission");
        SwrvePlot.onCreate(activity);
        _DonePlot = true;
    }

    private static boolean HasLocPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean _DoneLoc;
    private static void SetupLoc(final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            _locationManager = (LocationManager)
                    activity.getSystemService(Context.LOCATION_SERVICE);

            LocationListener locationListener = new LocationListener() {
                final String LOG_TAG = "SwrveUnityAndroidGPS";

                @Override
                public void onLocationChanged(Location location)
                {
                    Log.d(LOG_TAG, "onLocationChanged " + location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras)
                {
                    Log.d(LOG_TAG, "onStatusChanged " + provider + " " + status + "");
                }

                @Override
                public void onProviderEnabled(String provider) {
                    Log.d(LOG_TAG, "onProviderEnabled " + provider);
                }

                @Override
                public void onProviderDisabled(String provider) {
                    Log.d(LOG_TAG, "onProviderDisabled " + provider);
                }
            };

            if ((ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                return;
            }
            _locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
            _DoneLoc = true;
            }
        });
    }
}
