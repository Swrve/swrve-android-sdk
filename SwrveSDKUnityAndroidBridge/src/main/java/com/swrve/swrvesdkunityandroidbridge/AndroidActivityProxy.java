package com.swrve.swrvesdkunityandroidbridge;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrvePlot;
import com.swrve.sdk.SwrveSDK;
import com.unity3d.player.UnityPlayerActivity;
import com.unity3d.player.UnityPlayerNativeActivity;
import com.unity3d.player.UnityPlayerProxyActivity;

public class AndroidActivityProxy extends UnityPlayerActivity
{
    final private int REQUEST_CODE_ASK_LOC_PERMISSIONS = 124;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        SwrveSDK.onCreate(this);

        // checkLocPermission();
    }

    private void checkLocPermission()
    {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            afterLocPermission();
        }
        else
        {
            final String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_LOC_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_LOC_PERMISSIONS:
            {
                checkLocPermission();
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void afterLocPermission()
    {
        SwrvePlot.onCreate(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        SwrveSDK.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        SwrveSDK.onResume(this);
    }

    @Override
    protected void onDestroy()
    {
        SwrveSDK.onDestroy(this);
        super.onDestroy();
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        SwrveSDK.onLowMemory();
    }

//    public static void onCreate(Bundle savedInstanceState)
//    {
//        SwrveSDK.onCreate( SwrveAndroidHelper.GetUnityActivity() );
//    }
//
//    public static void onPause()
//    {
//        SwrveSDK.onPause();
//    }
//
//    public static void onResume()
//    {
//        SwrveSDK.onResume( SwrveAndroidHelper.GetUnityActivity() );
//    }
//
//    public static void onDestroy()
//    {
//        SwrveSDK.onDestroy( SwrveAndroidHelper.GetUnityActivity() );
//    }
//
//    public static void onLowMemory()
//    {
//        SwrveSDK.onLowMemory();
//    }
}
