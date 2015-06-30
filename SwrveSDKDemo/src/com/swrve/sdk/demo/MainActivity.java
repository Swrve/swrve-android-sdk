package com.swrve.sdk.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.swrve.sdk.SwrveIAPRewards;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.UIThreadSwrveUserResourcesDiffListener;
import com.swrve.sdk.UIThreadSwrveUserResourcesListener;
import com.swrve.sdk.runnable.UIThreadSwrveResourcesDiffRunnable;
import com.swrve.sdk.runnable.UIThreadSwrveResourcesRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// --------------------------------------------------------------------
// todo.gg - Refactor the example to show a "BaseActivity"
// implementation.  Most customers, if not all, have a base activity
// which all of their other activites derive.  We should follow that
// pattern here.
// --------------------------------------------------------------------


public class MainActivity extends Activity {
    private static final String LOG_TAG = "SwrveDemo";
    private boolean appAvailable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwrveSDK.onCreate(this);

        // --------------------------------------------------------------------
        // todo.gg - Add auto event instrumentation here. The default should
        // be the name of the activity (probably with the package name taken
        // out.)  Next, add support for class annotations where the developer
        // can override the name on a per-class basis
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // todo.gg - Add an example of where an engineer should send user
        // properties.  We recommend once at the beginning and the end of each
        // session.  That might be hard in Android though.
        // --------------------------------------------------------------------
    }

    public void btnSendEvent(View v) {
        // Queue custom event
        HashMap<String, String> payloads = new HashMap<String, String>();
        payloads.put("level", "" + (new Random().nextInt(2000)));
        SwrveSDK.event("TUTORIAL.END", payloads);
    }

    public void btnPurchase(View v) {
        // Queue purchase event
        SwrveSDK.purchase("BANANA_PACK", "gold", 120, 99);
    }

    public void btnCurrencyGiven(View v) {
        // Queue currency given event
        SwrveSDK.currencyGiven("gold", 999999);
    }

    // --------------------------------------------------------------------
    // todo.gg - Add a practical IAP example here as we have documented
    // on our site.  It think we can add this to the base activity but we
    // need to confirm
    // --------------------------------------------------------------------

    public void btnIap(View v) {
        // Queue IAP event
        SwrveIAPRewards rewards = new SwrveIAPRewards("gold", 200);
        SwrveSDK.iap(1, "CURRENCY_PACK", 9.99, "USD", rewards);
    }

    public void btnUserUpdate(View v) {
        // Queue user update event
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("level", "12");
        attributes.put("coins", "999");
        SwrveSDK.userUpdate(attributes);
    }

    // --------------------------------------------------------------------
    // todo.gg - Remove these callbacks.  Instead we should use the
    // SwrveResourceManager class only because that's what we recommend
    // --------------------------------------------------------------------

    public void btnGetResources(View v) {
        // Get resources
        SwrveSDK.getUserResources(new UIThreadSwrveUserResourcesListener(MainActivity.this, new UIThreadSwrveResourcesRunnable() {
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
        SwrveSDK.getUserResourcesDiff(new UIThreadSwrveUserResourcesDiffListener(MainActivity.this, new UIThreadSwrveResourcesDiffRunnable() {
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
        //SwrveSDK.event("Swrve.Demo.OfferMessage");
        SwrveSDK.event("Swrve.Messages.showAtSessionStart");
        // TODO.Converser: Using this trigger to emulate show at session start
        // Look at session start not working for some reason (debuggin?)
    }

    public void btnSendQueuedEvents(View v) {
        // Send data to Swrve
        SwrveSDK.sendQueuedEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Notify the SDK of activity resume.
        appAvailable = true;

        SwrveSDK.onResume(this);
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // Notify the SDK of activity low memory.
        SwrveSDK.onLowMemory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Notify the SDK of activity pause.
        appAvailable = false;
        SwrveSDK.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Notify the SDK of activity destroy.
        appAvailable = false;
        SwrveSDK.onDestroy(this);
    }
}
