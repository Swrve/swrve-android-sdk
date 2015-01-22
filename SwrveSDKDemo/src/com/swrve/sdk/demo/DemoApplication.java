package com.swrve.sdk.demo;

import android.app.Application;

import com.swrve.sdk.SwrveInstance;

public class DemoApplication extends Application {

    private static int YOUR_APP_ID = 123;
    private static String YOUR_API_KEY = "YOUR_API_KEY";

    @Override
    public void onCreate() {
        super.onCreate();
// TODO DOM change build rake script to look for appid/appkey in this class instead of MainActivity
        SwrveInstance.createInstance(this, YOUR_APP_ID, YOUR_API_KEY);
    }
}