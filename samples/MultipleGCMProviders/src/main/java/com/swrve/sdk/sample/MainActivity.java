package com.swrve.sdk.sample;

import android.os.Bundle;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.swrve.sdk.SwrveSDK;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SwrveSDK.onCreate(this);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SwrveSDK.onResume(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        SwrveSDK.onLowMemory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SwrveSDK.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        SwrveSDK.onNewIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SwrveSDK.onDestroy(this);
    }
}
