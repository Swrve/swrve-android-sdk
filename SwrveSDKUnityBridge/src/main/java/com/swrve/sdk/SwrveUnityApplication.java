package com.swrve.sdk;

import android.app.Application;

public class SwrveUnityApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        SwrveCommon.setRunnable(new Runnable() {
            @Override
            public void run() {
                new SwrveUnityCommon().init(SwrveUnityApplication.this, null);
            }
        });
    }
}
