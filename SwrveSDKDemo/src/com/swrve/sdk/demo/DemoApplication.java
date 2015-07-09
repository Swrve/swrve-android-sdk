package com.swrve.sdk.demo;

import android.app.Application;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.gcm.ISwrvePushNotificationListener;
import com.swrve.sdk.messaging.ISwrveCustomButtonListener;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DemoApplication extends Application {

    private static final String LOG_TAG = "SwrveDemo";
    private static int YOUR_APP_ID = 415;
    private static String YOUR_API_KEY = "VhZ4d59QKL0tRPjmBhnP";

    @Override
    public void onCreate() {
        super.onCreate();

        // --------------------------------------------------------------------
        // todo.gg - Add sandbox and production api_keys.  Show an if block
        // that chooses the sandbox or a production api_key depending on the
        // build config
        // --------------------------------------------------------------------

        SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY); // TODO DOM rake build script looks for appid/appkey in MainActivity. Change this.

        final DemoApplication application = this;
        SwrveSDK.setCustomButtonListener(new ISwrveCustomButtonListener() {
            @Override
            public void onAction(String customAction) {
                application.processDeeplink(customAction);
            }
        });
        SwrveSDK.setPushNotificationListener(new ISwrvePushNotificationListener() {
            @Override
            public void onPushNotification(Bundle bundle) {
                application.processDeeplink(bundle.getString("deeplink", null));
            }
        });
    }

    /*
        Parses the deeplink as a uri and executes Swrve specific actions embedded
        in it's query parameters.

        We recommend you implement these actions in your app so that your marketing
        team can take full advantage of the Swrve toolset:

            * event action - Fires one Swrve event for each event in the comma delimited
              list. Since events are also trigger points these events can trigger other in-app
              messages.
            * property action - Sets user properties for each property pair in the comma
              delimited list.
            * refresh_campaigns action - Sends all queued events to Swrve and refreshes the
              campaigns on the device.  Typically used with actions above to deliver campaigns to
              users who just entered an segment based on an event or propety.
     */
    protected void processDeeplink(String deeplink) {

        final int EVENT_DELAY_MILLISECONDS = 250;
        final int REFRESH_CAMPAIGNS_DELAY_MILLISSECONDS = 5000;

        if(SwrveHelper.isNullOrEmpty(deeplink)) {
            return;
        }

        Uri uri = Uri.parse(deeplink);
        if(uri.getScheme().equals("swrve")) {
            for( String key: uri.getQueryParameterNames() ) {

                final String value = uri.getQueryParameter(key);

                if(key.equals("event") ) {
                    ScheduledExecutorService timedService = Executors.newSingleThreadScheduledExecutor();
                    timedService.schedule(new Runnable() {
                        @Override
                        public void run() {
                            String[] events = value.split(",");
                            for(String event: events) {
                                SwrveSDK.event(value);
                            }
                            SwrveSDK.sendQueuedEvents();
                        }
                    }, EVENT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS);
                }
                else if(key.equals("property")) {
                    ScheduledExecutorService timedService = Executors.newSingleThreadScheduledExecutor();
                    timedService.schedule(new Runnable() {
                        @Override
                        public void run() {
                            String[] keyValuePairs = value.split(",");
                            if(keyValuePairs.length % 2 == 0 ) {
                                for( int i = 0; i < keyValuePairs.length; i += 2 ) {
                                    HashMap<String, String> attribute = new HashMap<String, String>();
                                    attribute.put(keyValuePairs[i], keyValuePairs[i+1]);
                                    SwrveSDK.userUpdate(attribute);
                                }
                            }
                            SwrveSDK.sendQueuedEvents();
                        }
                    }, EVENT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS);
                }
                // Trigger an in-app message that is not on the device ye
                else if(key.equals("refresh_campaigns")) {
                    ScheduledExecutorService timedService = Executors.newSingleThreadScheduledExecutor();
                    timedService.schedule(new Runnable() {
                        @Override
                        public void run() {
                            SwrveSDK.enableAutoShowMessage();
                            SwrveSDK.refreshCampaignsAndResources();
                        }
                    }, REFRESH_CAMPAIGNS_DELAY_MILLISSECONDS, TimeUnit.MILLISECONDS);
                }
                // Make an in-app purchase
                else if(key.equals("iap")) {

                }
                // Make a virtual purchase
                else if(key.equals("purchase")) {

                }
                // Goto another activity
                else if(key.equals("goto")) {

                }
            }
        }

    }

    // --------------------------------------------------------------------
    // todo.gg - Add processDeepLink method that handles all of our
    // recommended deeplinks including:
    //      uri based
    //          event
    //          user property
    //          trigger (same as event but with delay + flush events for push to in-app)
    //          permissions
    //          purchase SKU
    //          goto (activity by name in Android, seque in iOS, nothing in Unity)
    //      non uri based
    //          gift SKU (non uri based)
    //          gift currency (non uri based)
    // --------------------------------------------------------------------
}