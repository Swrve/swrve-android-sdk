Swrve
=
Swrve is a single integrated platform delivering everything you need to drive mobile engagement and create valuable consumer relationships on mobile.

This Android SDK will enable your app to use all of these features.

- [Getting started](#getting-started)
  - [Requirements](#requirements)
    - [Android 2.3.3+](#android-233)
    - [Gradle (distributed with the SDK)](#gradle-distributed-with-the-sdk)
- [Installation Instructions](#installation-instructions)
  - [In-app Messaging](#in-app-messaging)
    - [In-app Messaging Deeplinks](#in-app-messaging-deeplinks)
  - [Push Notifications](#push-notifications)
    - [If you already use Push Notifications](#if-you-already-use-push-notifications)
    - [Creating and Inputting the Android Server Key](#creating-and-inputting-the-android-server-key)
    - [Advanced Configuration](#advanced-configuration)
  - [Sending Events](#sending-events)
    - [Sending Named Events](#sending-named-events)
    - [Event Payloads](#event-payloads)
    - [Send User Properties](#send-user-properties)
    - [Sending Virtual Economy Events](#sending-virtual-economy-events)
    - [Sending IAP Events and IAP Validation](#sending-iap-events-and-iap-validation)
    - [IAP Functions](#iap-functions)
    - [Enabling IAP Receipt Validation](#enabling-iap-receipt-validation)
    - [Verifying IAP Receipt Validation](#verifying-iap-receipt-validation)
  - [Integrating Resource A/B Testing](#integrating-resource-ab-testing)
  - [Testing your integration](#testing-your-integration)
- [Upgrade Instructions](#upgrade-instructions)
- [How to run the demo](#how-to-run-the-demo)
- [How to build the SDK](#how-to-build-the-sdk)
- [Contributing](#contributing)
- [License](#license)

Getting started
=
Have a look at the quick integration guide at http://docs.swrve.com/developer-documentation/37926233/

Requirements
-

### Android 2.3.3+
The SDK supports Android 2.3.3+ but will handle older versions with a dummy SDK.

### Gradle (distributed with the SDK)
Used to build the SDK and its dependencies.

Installation Instructions
=

In-app Messaging
-
Integrate the in-app messaging functionality so you can use Swrve to send personalized messages to your app users while they’re using your app. If you’d like to find out more about in-app messaging, see [Intro to In-App Messages](http://docs.swrve.com/user-documentation/in-app-messaging/intro-to-in-app-messages/).

Before you can test the in-app message feature in your game, you need to create an In App Message campaign in the Swrve Dashboard.

### In-app Messaging Deeplinks

When creating in-app messages in Swrve, you can configure message buttons to direct users to perform a custom action when clicked. For example, you might configure a button to direct the app user straight to your app store. To enable this feature, you must configure deeplinks by performing the actions outlined below. For more information about creating in-app messages in Swrve, see [Creating In-App Messages](http://docs.swrve.com/user-documentation/in-app-messaging/creating-in-app-messages/).

Swrve's default deeplink behaviour is to treat custom actions as URLs and therefore use your existing custom URL scheme. Before handling deeplinks in Swrve, you’ll need to register a custom URL scheme in your `AndroidManifest.xml`.

Once the custom URL scheme is set, your app can receive and direct users from outside of the app.

It is also possible to override this behavior and integrate custom actions to direct users to a sale, website or other target when they click on an in-app message. For example, if you would like to handle additional swrve query parameters from this URL, you must implement this within the app.


```
SwrveSDK.setCustomButtonListener(new ISwrveCustomButtonListener() {
   @Override
   public void onAction(String customAction) {
      // Custom code here
   }
});
```

For example, if you'd like to send Swrve events using custom actions, you could add a customButtonCallback like this:

```
SwrveSDK.setCustomButtonListener(new ISwrveCustomButtonListener() {
    @Override
    public void onAction(String customAction) {
        final int EVENT_DELAY_MILLISECONDS = 250;
        ScheduledExecutorService timedService = Executors.newSingleThreadScheduledExecutor();
        timedService.schedule(new Runnable() {
            @Override
            public void run() {
                Uri uri = Uri.parse(customAction);
                if (uri.getScheme().equals("swrve")) {
                    for( String key: uri.getQueryParameterNames() ) {
                        final String value = uri.getQueryParameter(key);

                        if(key.equals("event") ) {
                            SwrveSDK.event(value);
                        }
                        SwrveSDK.sendQueuedEvents();
                    }
                }
            }, EVENT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS);
        }
    }
});
```


Push Notifications
-

In addition, you must be using the Google SDK, replacing X.X.X with the SDK version number (for example, 4.0.2). If no patch version is present, assume 0; for example with 4.0, use 4.0.0:

```
dependencies {
	compile 'com.swrve.sdk.android:swrve-google:X.X.X’
}
```

1. Make the following changes to your `AndroidManifest.xml` to include Google’s GCM receiver, Swrve’s intent service, permissions and configuration metadata:

  ```
  <?xml version="1.0" encoding="utf-8"?>
  <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="YOUR_PACKAGE_NAME" android:versionCode="1" android:versionName="1.0" >
   <uses-sdk android:minSdkVersion="10" android:targetSdkVersion="10" />
    ...
    <!-- Add this to your AndroidManifest.xml -->
    <uses-permission android:name="android.permission.GET_ACCOUNTS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
    <permission android:name="YOUR_PACKAGE_NAME.permission.C2D_MESSAGE" android:protectionLevel="signature" />
    <uses-permission android:name="YOUR_PACKAGE_NAME.permission.C2D_MESSAGE" />
    <!-- End of changes -->

    <application android:allowBackup="true" android:icon="@drawable/ic_launcher" android:label="@string/app_name" android:theme="@style/AppTheme" >

      <!-- Add this to your AndroidManifest.xml -->
      <!-- Specifies the Google Play Services version that the app was compiled with -->
      <meta-data android:name="com.google.android.gms.version" android:value="@integer/google_play_services_version" />
      
      <!-- Configure the aspect of the UI -->
      <meta-data android:name="SWRVE_PUSH_ICON" android:resource="@drawable/ic_launcher"/>
      <meta-data android:name="SWRVE_PUSH_ICON_MATERIAL" android:resource="@drawable/ic_launcher_material"/>
      <meta-data android:name="SWRVE_PUSH_ICON_LARGE" android:resource="@drawable/ic_launcher_large" />
      <meta-data android:name="SWRVE_PUSH_ACCENT_COLOR" android:resource="@android:color/black" />
      <meta-data android:name="SWRVE_PUSH_ACTIVITY" android:value=".MainActivity"/>
      <meta-data android:name="SWRVE_PUSH_TITLE" android:value="Your app title"/>

      <receiver android:name="com.google.android.gms.gcm.GcmReceiver" android:exported="true" android:permission="com.google.android.c2dm.permission.SEND" >
        <intent-filter>
          <action android:name="com.google.android.c2dm.intent.RECEIVE" />
          <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
          <category android:name="YOUR_PACKAGE_NAME" />
        </intent-filter>
      </receiver>
      <service android:name="com.swrve.sdk.gcm.SwrveGcmIntentService" >
        <intent-filter>
          <action android:name="com.google.android.c2dm.intent.RECEIVE" />
        </intent-filter>
      </service>
      <service android:name="com.swrve.sdk.gcm.SwrveGcmInstanceIDListenerService" android:exported="false" >
        <intent-filter>
          <action android:name="com.google.android.gms.iid.InstanceID" />
        </intent-filter>
      </service>
      <!-- End of changes -->
      ...
   </application>
  </manifest>
  ```

  Note that you must replace `YOUR_PACKAGE_NAME` with the main package of your app. The listener services for push notifications need to live in one of your app’s packages.

2. Provide the sender ID (the project number obtained from the Google Developer Console) to the SDK on initialization. If you haven’t specified any special configuration, do the following:

  ```
  SwrveSDK.onCreate(this, appId, apiKey, SwrveConfig.withPush(SENDER_ID));
  ```

  If you initialized the SDK with advanced configuration, add the following line to your initialization:

  ```
  config.setSenderId(SENDER_ID);
  ```

  For more information about the sender ID, see [How Do I Manage the Server Key for Push Notifications?](http://docs.swrve.com/faqs/push-notifications/manage-android-server-key-for-push-notifications/)

### If you already use Push Notifications

1. To make sure other providers continue to work move Swrve’s intent service to the bottom of your application definition. This will prevent the broadcast listener from calling it as the first option.

2. Create a custom broadcast listener and use it to replace the base `GcmReceiver`. This broadcast listener will make sure to call Swrve when the pushes are directed to it:

  ```
  import android.content.ComponentName;
  import android.content.Context;
  import android.content.Intent;
  import android.os.Bundle;

  import com.google.android.gms.gcm.GcmReceiver;
  import com.swrve.sdk.SwrveHelper;

  public class SwrveIdentifyPushesBroadcastListener extends GcmReceiver {
      @Override
      public void zzd(Context context, Intent intent) {
          // Call the Swrve intent service if the push contains the Swrve payload _p
          if("com.google.android.c2dm.intent.RECEIVE".equals(intent.getAction())) {
              Bundle extras = intent.getExtras();
              if (extras != null) {
                  Object rawId = extras.get("_p");
                  String msgId = (rawId != null) ? rawId.toString() : null;
                  if (!SwrveHelper.isNullOrEmpty(msgId)) {
                      // It is a Swrve push!
                      ComponentName comp = new ComponentName(context.getPackageName(), "com.swrve.sdk.gcm.SwrveGcmIntentService");
                      intent = intent.setComponent(comp);
                  }
              }
         }
         super.zzd(context, intent);
      }
  }
  ```

3. Update your `AndroidManifest.xml` with these changes, it should look like:

  ```
  <?xml version="1.0" encoding="utf-8"?>
  <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="YOUR_PACKAGE_NAME" android:versionCode="1" android:versionName="1.0" >
   <uses-sdk android:minSdkVersion="10" android:targetSdkVersion="10" />
    ...
    <!-- You should already have these -->
    <uses-permission android:name="android.permission.GET_ACCOUNTS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
    <permission android:name="YOUR_PACKAGE_NAME.permission.C2D_MESSAGE" android:protectionLevel="signature" />
    <uses-permission android:name="YOUR_PACKAGE_NAME.permission.C2D_MESSAGE" />
   
    <application android:allowBackup="true" android:icon="@drawable/ic_launcher" android:label="@string/app_name" android:theme="@style/AppTheme" >
   
      <!-- You should already have this -->
      <!-- Specifies the Google Play Services version that the app was compiled with -->
      <meta-data android:name="com.google.android.gms.version" android:value="@integer/google_play_services_version" />
   
      <!-- Add this to your AndroidManifest.xml -->
      <!-- Configure the aspect of the UI --> 
      <meta-data android:name="SWRVE_PUSH_ICON" android:resource="@drawable/ic_launcher"/> 
      <meta-data android:name="SWRVE_PUSH_ACTIVITY" android:value=".MainActivity"/> 
      <meta-data android:name="SWRVE_PUSH_TITLE" android:value="Your app title"/>
      <meta-data android:name="SWRVE_PUSH_ICON_MATERIAL" android:resource="@drawable/ic_launcher_material"/>
      <meta-data android:name="SWRVE_PUSH_ICON_LARGE" android:resource="@drawable/ic_launcher_large" />
      <meta-data android:name="SWRVE_PUSH_ACCENT_COLOR" android:resource="@android:color/black" />
      <receiver
        android:name="YOUR_PACKAGE_NAME.SwrveIdentifyPushesBroadcastListener"
        android:exported="true"
        android:permission="com.google.android.c2dm.permission.SEND">
        <intent-filter>
          <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
          <action android:name="com.google.android.c2dm.intent.RECEIVE" />
          <category android:name="YOUR_PACKAGE_NAME" />
        </intent-filter>
     </receiver>
     <!-- End of changes -->

     ... Your other push intent services here ...

     <!-- Add this to your AndroidManifest.xml -->
     <service android:name="com.swrve.sdk.gcm.SwrveGcmIntentService">
       <intent-filter>
         <action android:name="com.google.android.c2dm.intent.RECEIVE" />
       </intent-filter>
     </service>
     <!-- End of changes -->
    </application>
  </manifest>
  ```

  Note that you must replace `YOUR_PACKAGE_NAME` with the main package of your app.

4. Provide the sender ID (the project number obtained from the Google Developer Console) when creating the SDK instance. If you haven’t specified any special configuration, do the following:

  ```
  SwrveSDK.createInstance(this, appId, apiKey, SwrveConfig.withSenderId(SENDER_ID));
  ```

  For more information about the sender ID, see [How Do I Manage the Server Key for Push Notifications?](http://docs.swrve.com/faqs/push-notifications/manage-android-server-key-for-push-notifications/)

  If you initialized the SDK with advanced configuration, add the following line to your initialization:

  ```
  config.setSenderId(SENDER_ID);
  ```

### Creating and Inputting the Android Server Key

To enable your app to send push notifications to Google Play devices, you require a server key. For more information, see [How Do I Manage the Server Key for Push Notifications?](http://docs.swrve.com/faqs/push-notifications/manage-android-server-key-for-push-notifications/)

### Advanced Configuration

This section describes how to configure advanced options for Android SDK push notification integration.

* Processing Custom Payloads

  To process notifications when they are opened from a push notification, add a listener for push notifications to the SDK before initialization:

  ```
  SwrveSDK.setPushNotificationListener(new ISwrvePushNotificationListener() {
     @Override
     public void onPushNotification(Bundle bundle) {
        // CUSTOM CODE HERE
     }
  });

  SwrveSDK.onCreate(this, appId, apiKey, SwrveConfig.withPush(SENDER_ID));
  ...
  ```

* Using Custom Sounds

  You can send push notifications with custom sounds. To do so, place your custom sound under `res/raw` and set your sounds in the Swrve service. For more information about adding custom sounds in Swrve, see [Intro to Push Notifications](http://docs.swrve.com/user-documentation/push-notifications/intro-to-push-notifications/).

Sending Events
-

### Sending Named Events ###

```
SwrveSDK.event("custom.event_name");
```

Rules for sending events:

* Do not send the same named event in differing case.

* Use '.'s in your event name to organize their layout in the Swrve dashboard. Each '.' creates a new tree in the UI which groups your events so they are easy to locate.
* Do not send more than 1000 unique named events.
 * Do not add unique identifiers to event names. For example, Tutorial.Start.ServerID-ABDCEFG
 * Do not add timestamps to event names. For example, Tutorial.Start.1454458885
* When creating custom events, do not use the `swrve.*` or `Swrve.*` namespace for your own events. This is reserved for Swrve use only. Custom event names beginning with `Swrve.` are restricted and cannot be sent.

### Event Payloads ###

An event payload can be added and sent with every event. This allows for more detailed reporting around events and funnels. The associated payload should be a dictionary of key/value pairs; it is restricted to string and integer keys and values. There is a maximum cardinality of 500 key-value pairs for this payload per event. This parameter is optional.

```
Map<String,String> payload = new HashMap<String, String>();
payload.put("key1", "value1");
payload.put("key2", "value2");
SwrveSDK.event("custom.event_name", payload);
```

For example, if you want to track when a user starts the tutorial experience it might make sense to send an event `tutorial.start` and add a payload `time` which captures how long the user spent starting the tutorial.

```
Map<String,String> payload = new HashMap<String, String>();
payload.put("time", "100");
SwrveSDK.event("tutorial.start", payload);
```

### Send User Properties ###

Assign user properties to send the status of the user. For example create a custom user property called `premium`, and then target non-premium users and premium users in the dashboard.

When configuring custom properties for Android, the Swrve SDK only supports string values.

```
Map<String, String> attributes = new HashMap<String, String>();
attributes.put("premium", "true");
attributes.put("level", "12");
attributes.put("balance", "999");
SwrveSDK.userUpdate(attributes);
```

### Sending Virtual Economy Events ###

To ensure virtual currency events are not ignored by the server, make sure the currency name configured in your app matches exactly the Currency Name you enter in the App Currencies section on the App Settings screen (including case-sensitive). If there is any difference, or if you haven’t added the currency in Swrve, the event will be ignored and return an error event called Swrve.error.invalid_currency. Additionally, the ignored events will not be included in your KPI reports. For more information, see [Add Your App](http://docs.swrve.com/getting-started/add-your-app/).

If your app has a virtual economy, send the purchase event when users purchase in-app items with virtual currency.

```
String item = "some.item";
String currency = "gold";
int cost = 99;
int quantity = 1;
SwrveSDK.purchase(item, currency, cost, quantity);
```

Send the currency given event when you give users virtual currency. Examples include initial currency balances, retention bonuses and level-complete rewards.

```
String givenCurrency = "gold";
double givenAmount = 99;
SwrveSDK.currencyGiven(givenCurrency, givenAmount);
```

### Sending IAP Events and IAP Validation ###

This section details the IAP functions for unverified IAP events and for IAP events where the receipt can be verified. It also details how to enable IAP receipt validation for Google Play.

### IAP Functions

In the case of Android platforms (both native and Unity), Swrve does not automatically receive IAP receipts. For Swrve to verify IAPs, you must send an event for each purchase using the IAP API with the purchase receipt. For native Android, use Google Play services for this purpose. For Unity Android, you must use a Unity plugin to receive IAP receipts.

IAP functions for unverified IAP events (that is, for any app store other than Google Play):

```
public void iap(int quantity,
                String productId,
                double productPrice,
                String currency);

public void iap(int quantity,
                String productId,
                double productPrice,
                String currency,
                SwrveIAPRewards rewards);
```

IAP functions for IAP events where the receipt can be verified (for now, only Google Play is supported):

```
public void iapPlay(String productId,
                    double productPrice,
                    String currency,
                    String receipt,
                    String receiptSignature);

public void iapPlay(String productId,
                    double productPrice,
                    String currency,
                    SwrveIAPRewards rewards,
                    String receipt,
                    String receiptSignature);
```

Example:

The following code is the initialization of the in-app purchase:

```
// start an in-app purchase for the product with purchaseProductId
try {
   Bundle buyIntentBundle = mService.getBuyIntent(3,
                                                  getPackageName(),
                                                  purchaseProductId,
                                                  "inapp",
                                                  "extra");
   PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
   startIntentSenderForResult(pendingIntent.getIntentSender(),
                              purchaseRequestCode,
                              new Intent(), 
                              Integer.valueOf(0),
                              Integer.valueOf(0),
                              Integer.valueOf(0));
} catch (RemoteException e) {
   e.printStackTrace();
} catch (SendIntentException e) {
   e.printStackTrace();
}
```

The following code is the callback function (executed when the purchase has been made):

```
@Override
// callback on purchase finished
protected void onActivityResult(int requestCode, int resultCode, Intent data) { 

   if (requestCode == purchaseRequestCode) {
      int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
      // get the receipt JSON string and the receipt signature
      String receipt = data.getStringExtra("INAPP_PURCHASE_DATA");
      String receiptSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
 
      // make sure the purchase succeeded
      if (resultCode == RESULT_OK && responseCode == 0) {
         // fill in the product details and the receipt data
         SwrveSDK.iapPlay(purchaseProductId,
         	              purchasePrice,
         	              purchaseCurrency,
         	              receipt,
         	              receiptSignature);
      }
   }
}
```

It is also possible to create a `SwrveIAPRewards` object containing all the in-app currency and bundle items purchased (if the purchase contained other in-app items):

```
SwrveIAPRewards purchaseRewards = new SwrveIAPRewards();
purchaseRewards.addCurrency(virtualCurrency, 200);
purchaseRewards.addItem(inAppItem, 1);
```

In this instance, iapPlay can be supplied with this object:

```
SwrveSDK.iapPlay(purchaseProductId,
                 purchasePrice,
                 purchaseCurrency,
                 purchaseRewards,
                 receipt,
                 receiptSignature);
```

### Enabling IAP Receipt Validation

To enable validation of Google Play IAP receipts, on the Integration Settings screen, in the IAP Validation section, in the Google Play Licensing Public Key field, you must enter and save the public key for licensing and in-app billing. This public key is used to verify digital signatures. Google signs data (the app itself for verifying that the app has been payed for or the in-app purchase receipt) and Swrve then uses the public key to verify every purchase before calculating your revenue KPIs. This ensures that your revenue figures are as accurate as possible (ignoring pirates and cheaters).

You usually enter the public key into Swrve when configuring the Integration Settings screen as part of the Swrve onboarding process. You can edit the settings on this screen at any time, as required. On the Setup menu, click Integration Settings.

To access your public key, access the Google Play Developer Console and navigate to All applications > <Your Application> > Services and APIs. The public key is then displayed in the Your License Key For This Application section as illustrated below. For more information, see the [Google Play In-App Billing documentation](http://developer.android.com/google/play/billing/index.html).

![Google Play License Key](/docs/images/google-play-key.jpg)

### Verifying IAP Receipt Validation

Use the following events to monitor the success of IAP receipt validation:

* `swrve.valid_iap` – fired if receipt verification has been successful and the receipt is valid.
* `swrve.invalid_iap` – fired if receipt verification has been successful and the receipt is invalid.


Integrating Resource A/B Testing
-

To get the latest version of a resource from Swrve using the Resource Manager, use the following:

```
// Get the SwrveResourceManager which holds all up-to-date attribute values
SwrveResourceManager resourceManager = SwrveSDK.getResourceManager();

// Then, whenever you need to use a resource, pull it from the resourceManager.
// For example, use the following to set some welcome text in your app to the 
// current value of the attribute "welcome_text" of the resource "new_app_config"
// defaulting to "Welcome!" when attribute is unavailable
welcomeScreen.setMessageText(resourceManager.getAttributeAsString("new_app_config", "welcome_text", "Welcome!"));
```

If you want to be notified whenever resources change, you can add a callback function as follows:
iOSAndroidUnityPhoneGap

```
SwrveSDK.setResourcesListener(new ISwrveResourcesListener() {
  public void onResourcesUpdated() {
    // Callback functionality
  }
});
```

Testing your integration
-
When you have completed the steps above, the next step is to test the integration. See [Testing Your Integration](http://docs.swrve.com/developer-documentation/advanced-integration/testing-your-integration/) for more details.

Upgrade Instructions
=
If you’re moving from an earlier version of the Android SDK to the current version, see the [Android SDK Upgrade Guide](/docs/upgrade_guide.md) for upgrade instructions.

How to run the demo
=
Import the SwrveSDKDemo project into your IDE and replace YOUR_APP_ID and YOUR_API_KEY with the values provided by Swrve. Run as a normal Android application on your device or emulator.

How to build the SDK
=
To build the Swrve AAR library, run the following command from the SwrveSDK folder:
`../gradlew clean build assemble`
This will generate the AAR library in the `build/outputs/aar` folder. Use the `google` AAR for push related campaigns.

Contributing
=
We would love to see your contributions! Follow these steps:

1. Fork this repository.
2. Create a branch (`git checkout -b my_awesome_feature`)
3. Commit your changes (`git commit -m "Awesome feature"`)
4. Push to the branch (`git push origin my_awesome_feature`)
5. Open a Pull Request.

License
=
© Copyright Swrve Mobile Inc or its licensors. Distributed under the [Apache 2.0 License](LICENSE).  
Google Play Services Library Copyright © 2012 The Android Open Source Project. Licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).  
Gradle Copyright © 2007-2011 the original author or authors. Licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).
