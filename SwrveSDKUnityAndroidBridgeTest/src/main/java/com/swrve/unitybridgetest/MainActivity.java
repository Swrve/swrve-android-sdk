package com.swrve.unitybridgetest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.swrve.swrvesdkunityandroidbridge.SwrveAndroidHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SwrveAndroidHelper.DoTheNativeStuff(this, 1330, "OFLRPjJWrrQ6yTr2HNpv");
    }
}
