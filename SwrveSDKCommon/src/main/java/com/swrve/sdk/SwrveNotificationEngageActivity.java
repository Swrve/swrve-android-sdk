package com.swrve.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class SwrveNotificationEngageActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            overridePendingTransition(0, 0); // disable transition animation
            Intent intent = getIntent();
            getSwrveNotificationEngage(getApplicationContext()).processIntent(intent);
            finish();
        } catch (Exception e) {
            SwrveLogger.e("SwrveNotificationEngageActivity engage.processIntent", e);
        }
    }

    protected SwrveNotificationEngage getSwrveNotificationEngage(Context context) {
        return new SwrveNotificationEngage(context);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0); // disable transition animation
    }
}
