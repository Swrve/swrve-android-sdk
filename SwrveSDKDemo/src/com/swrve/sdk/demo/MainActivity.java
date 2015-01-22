package com.swrve.sdk.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.swrve.sdk.ISwrve;
import com.swrve.sdk.SwrveIAPRewards;
import com.swrve.sdk.SwrveInstance;
import com.swrve.sdk.UIThreadSwrveUserResourcesDiffListener;
import com.swrve.sdk.UIThreadSwrveUserResourcesListener;
import com.swrve.sdk.runnable.UIThreadSwrveResourcesDiffRunnable;
import com.swrve.sdk.runnable.UIThreadSwrveResourcesRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends Activity {
    private static final String LOG_TAG = "SwrveDemo";
    private boolean appAvailable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwrveInstance.getInstance().initOrBind(this);
    }

    public void btnSendEvent(View v) {
        // Queue custom event
        HashMap<String, String> payloads = new HashMap<String, String>();
        payloads.put("level", "" + (new Random().nextInt(2000)));
        SwrveInstance.getInstance().event("TUTORIAL.END", payloads);
    }

    public void btnPurchase(View v) {
        // Queue purchase event
        SwrveInstance.getInstance().purchase("BANANA_PACK", "gold", 120, 99);
    }

    public void btnCurrencyGiven(View v) {
        // Queue currency given event
        SwrveInstance.getInstance().currencyGiven("gold", 999999);
    }

    public void btnIap(View v) {
        // Queue IAP event
        SwrveIAPRewards rewards = new SwrveIAPRewards("gold", 200);
        SwrveInstance.getInstance().iap(1, "CURRENCY_PACK", 9.99, "USD", rewards);
    }

    public void btnUserUpdate(View v) {
        // Queue user update event
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("level", "12");
        attributes.put("coins", "999");
        SwrveInstance.getInstance().userUpdate(attributes);
    }

    public void btnGetResources(View v) {
        // Get resources
        SwrveInstance.getInstance().getUserResources(new UIThreadSwrveUserResourcesListener(MainActivity.this, new UIThreadSwrveResourcesRunnable() {
            @Override
            public void onUserResourcesSuccess(Map<String, Map<String, String>> resources, String resourcesAsJSON) {
                if (appAvailable) {
                    // Runs on UI thread
                    String resourcesTxt = resources.keySet().toString();
                    Toast.makeText(MainActivity.this, "Resources: " + resourcesTxt, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onUserResourcesError(Exception exception) {
                String exceptionTxt = exception.getMessage();
                Toast.makeText(MainActivity.this, "EXCEPTION!: " + exceptionTxt, Toast.LENGTH_LONG).show();
            }
        }));
    }

    public void btnGetResourcesDiffs(View v) {
        // Get AB test resources diffs
        SwrveInstance.getInstance().getUserResourcesDiff(new UIThreadSwrveUserResourcesDiffListener(MainActivity.this, new UIThreadSwrveResourcesDiffRunnable() {
            @Override
            public void onUserResourcesDiffSuccess(Map<String, Map<String, String>> oldResourcesValues, Map<String, Map<String, String>> newResourcesValues, String resourcesAsJSON) {
                if (appAvailable) {
                    // Runs on UI thread
                    String resourcesTxt = newResourcesValues.keySet().toString();
                    Toast.makeText(MainActivity.this, "New resources: " + resourcesTxt, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onUserResourcesDiffError(Exception exception) {
                String exceptionTxt = exception.getMessage();
                Toast.makeText(MainActivity.this, "EXCEPTION!: " + exceptionTxt, Toast.LENGTH_LONG).show();
            }
        }));
    }

    public void btnShowTalkMessage(View v) {
        // Swrve Talk - Trigger message for Swrve.Demo.OfferMessage
        SwrveInstance.getInstance().event("Swrve.Demo.OfferMessage");
    }

    public void btnSendQueuedEvents(View v) {
        // Send data to Swrve
        SwrveInstance.getInstance().sendQueuedEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Notify the SDK of activity resume.
        appAvailable = true;
        SwrveInstance.getInstance().onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Notify the SDK of activity pause.
        appAvailable = false;
        SwrveInstance.getInstance().onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Notify the SDK of activity destroy.
        appAvailable = false;
        SwrveInstance.getInstance().onDestroy();
    }
}
