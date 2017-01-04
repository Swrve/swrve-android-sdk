Android SDK Upgrade Guide
=

This guide provides information about how you can upgrade to the latest Swrve Android SDK. For information about the changes that have been made in each Android SDK release, see [Android SDK Release Notes](/docs/release_notes.md).

- [Upgrading to Android SDK v4.1](#upgrading-to-android-sdk-v41)
  - [Custom Events Starting with "Swrve"](#custom-events-starting-with-swrve)
  - [Google Push New Material Metadata](#google-push-new-material-metadata)
- [Upgrading to Android SDK v4.0.5](#upgrading-to-android-sdk-v405)
- [Upgrading to Android SDK v4.0.4](#upgrading-to-android-sdk-v404)
- [Upgrading to Android SDK v4.0.3](#upgrading-to-android-sdk-v403)
- [Upgrading to Android SDK v4.0.2](#upgrading-to-android-sdk-v402)
- [Upgrading to Android SDK v4.0.1](#upgrading-to-android-sdk-v401)
- [Upgrading to Android SDK v4.0](#upgrading-to-android-sdk-v40)
  - [New SwrveSDK class](#new-swrvesdk-class)
  - [Swrve Initialization](#swrve-initialization)
  - [AAR Distribution](#aar-distribution)
- [Upgrading to Android SDK v3.4](#upgrading-to-android-sdk-v34)
  - [New onCreate method](#new-oncreate-method)
  - [Lifecycle Changes](#lifecycle-changes)
  - [In-app Message Background Color default](#in-app-message-background-color-default)
- [Upgrading to Android SDK v3.3.1](#upgrading-to-android-sdk-v331)
- [Upgrading to Android SDK v3.3](#upgrading-to-android-sdk-v33)
- [Upgrading to Android SDK v3.2](#upgrading-to-android-sdk-v32)
  - [Link token and link server deprecated](#link-token-and-link-server-deprecated)
- [Upgrading to Android SDK v3.1](#upgrading-to-android-sdk-v31)
  - [Google Cloud Messaging Push Integration](#google-cloud-messaging-push-integration)
  - [In-app Messages at Session Start](#in-app-messages-at-session-start)
- [Upgrading to Android SDK v3.0](#upgrading-to-android-sdk-v30)
  - [Real-time targeting](#real-time-targeting)
  - [Event Intervals](#event-intervals)
  - [Multiple Activities](#multiple-activities)
  - [Dialog Listener](#dialog-listener)
  - [In-app Message Sample Images](#in-app-message-sample-images)
  - [Cache folder](#cache-folder)
- [Upgrading to Android SDK v2.10](#upgrading-to-android-sdk-v210)
  - [Creating an instance of the SDK](#creating-an-instance-of-the-sdk)
  - [Custom Button Processing](#custom-button-processing)
  - [In-app Messages](#in-app-messages)
- [Upgrading to Android SDK v2.9](#upgrading-to-android-sdk-v29)
- [Upgrading to Android SDK v2.8](#upgrading-to-android-sdk-v28)
  - [Moving from game to app](#moving-from-game-to-app)
  - [In-app Messaging enabled by default](#in-app-messaging-enabled-by-default)
- [Upgrading to Android SDK v2.7](#upgrading-to-android-sdk-v27)
  - [Automatic User ID generation](#automatic-user-id-generation)
  - [SDK Singleton Access](#sdk-singleton-access)
  - [Analytics and In-app Messaging Integration](#analytics-and-in-app-messaging-integration)
- [Upgrading to Android SDK v2.6](#upgrading-to-android-sdk-v26)
  - [Deprecation of the `buy_in` function](#deprecation-of-the-buy_in-function)
- [Upgrading to Android SDK v2.4](#upgrading-to-android-sdk-v24)
  - [Migrating from Android SDK 2.3 TO Android SDK 2.4](#migrating-from-android-sdk-23-to-android-sdk-24)

Upgrading to Android SDK v4.1
-
This section provides information to enable you to upgrade to Swrve Android SDK v4.1.

### Custom Events Starting with "Swrve"

Custom events that start with `Swrve.*` or `swrve.*` are now restricted. You need to rename any custom `Swrve.` events or they won’t be sent.

### Google Push New Material Metadata

The Android SDK now includes new metadata configurations for remote notifications so they can be configured properly for Android L+. The new configurations are:

* Material icon – To configure a Material icon for Android L+, add the metadata key `SWRVE_PUSH_ICON_MATERIAL` to your application’s `AndroidManifest.xml`:

  ```
  <meta-data android:name=”SWRVE_PUSH_ICON_MATERIAL” android:resource=”@drawable/ic_launcher_material” />
  ```

* Large icon – To configure a large icon to be displayed for your notification, add the metadata key `SWRVE_PUSH_ICON_LARGE` to your application’s `AndroidManifest.xml`:
  ```
  <meta-data android:name=”SWRVE_PUSH_ICON_LARGE” android:resource=”@drawable/ic_launcher_large” />
  ```

* Accent color – To configure a Material accent color for Android L+, add the metadata key `SWRVE_PUSH_ACCENT_COLOR` to your application’s `AndroidManifest.xml`:

  ```
  <meta-data android:name=”SWRVE_PUSH_ACCENT_COLOR” android:resource=”@android:color/black” />
  ```

If you’re integrating the Android SDK libraries using Maven from the Swrve repository on Jcenter, then you need to update the version number to 4.1.0.

Upgrading to Android SDK v4.0.5
-

No code changes are required to upgrade to Swrve Android SDK v4.0.5.

If you’re integrating the Android SDK libraries using Maven from the Swrve repository on Jcenter, then you need to update the version number to 4.0.5.

Upgrading to Android SDK v4.0.4
-

No code changes are required to upgrade to Swrve Android SDK v4.0.4.

If you’re integrating the Android SDK libraries using Maven from the Swrve repository on Jcenter, then you need to update the version number to 4.0.4.

Upgrading to Android SDK v4.0.3
-
No code changes are required to upgrade to Swrve Android SDK v4.0.3.

If you’re integrating the Android SDK libraries using Maven from the Swrve repository on Jcenter, then you need to update the version number to 4.0.3.

Upgrading to Android SDK v4.0.2
-
This section provides information to enable you to upgrade to Swrve Android SDK v4.0.2.

Google Cloud Messaging Push Integration

The GCM integration has been updated to the latest Google library. To upgrade your app, do the following:

Update `AndroidManifest.xml` with the following changes:

* In your broadcast receiver, change the package name `android:name="com.swrve.sdk.gcm.SwrveGcmBroadcastReceiver"` to android:name="com.google.android.gms.gcm.GcmReceiver" and add `android:exported="true"`.

* Add `<action android:name="com.google.android.c2dm.intent.RECEIVE" />` to the intent service.

* Add a new service `<service android:name="com.swrve.sdk.gcm.SwrveGcmInstanceIDListenerService">` with the intent filter action `<action android:name="com.google.android.gms.iid.InstanceID" />`.

When you’re done, your `AndroidManifest.xml` should have a section like this:

```
<application android:icon="@drawable/app_icon" android:label="@string/app_name" android:debuggable="false">
<!-- Swrve Push Plugin -->
<receiver
    android:name="com.google.android.gms.gcm.GcmReceiver"
    android:exported="true"
    android:permission="com.google.android.c2dm.permission.SEND" >
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
        <category android:name="com.example.gcm" />
    </intent-filter>
</receiver>
<service android:name="com.swrve.sdk.gcm.SwrveGcmIntentService">
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
    </intent-filter>
</service>
<service
    android:name="com.swrve.sdk.gcm.SwrveGcmInstanceIDListenerService"
    android:exported="false" >
    <intent-filter>
       <action android:name="com.google.android.gms.iid.InstanceID" />
    </intent-filter>
</service>
<!-- End Swrve Push Plugin -->
```

If you’re integrating the Android SDK libraries using Maven from the Swrve repository on Jcenter, then you need to update the version number to 4.0.2.

Upgrading to Android SDK v4.0.1
-
No code changes are required to upgrade to Swrve Android SDK v4.0.1.

If you’re integrating the Android SDK libraries using Maven from the Swrve repository on Jcenter, then you need to update the version number to 4.0.1.

Upgrading to Android SDK v4.0
-
This section provides information to enable you to upgrade to Swrve Android SDK v4.0.

### New SwrveSDK class
The `SwrveInstance` class has been deprecated in favor of the new `SwrveSDK` class. Replace all instances of `SwrveInstance` with `Swrve.SDK` (see examples below).

SYNTAX	EXAMPLE
Old	SwrveInstance.getInstance().METHOD	SwrveInstance.getInstance().event(“tutorial.end”);
Map<String, String> attributes = new HashMap<String, String>();
attributes.put(“subscriptionType”, “trial”);
SwrveInstance.getInstance().userUpdate(attributes);
New	SwrveSDK.METHOD	SwrveSDK.event(“tutorial.end”);
Map<String, String> attributes = new HashMap<String, String>();
attributes.put(“subscriptionType”, “trial”);
SwrveSDK.userUpdate(attributes);


### Swrve Initialization

The initialization of the Swrve SDK has changed to use a singleton style which should be done in the application's `onCreate` method. For example, see the `YourApplication` class (below snippet is a demo only):

```
public class YourApplication extends Application {
   @Override
   public void onCreate() {
       super.onCreate();
       try {
           SwrveSDK.createInstance(this, <app_id>, "<api_key>");
       } catch (IllegalArgumentException exp) {
           Log.e("SwrveDemo", "Could not initialize the Swrve SDK", exp);
       }
   }
}
```

Update the `onCreate`, `onResume`, `onPause`, `onDestroy` and `onLowMemory` methods of your activities to change `SwrveInstance.*` to `SwrveSDK.*`.

```
import com.swrve.sdk.*;
import com.swrve.sdk.config.*;
 
public class YourActivity extends Activity {
 
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      SwrveSDK.onCreate(this);
   }
 
   @Override
   protected void onPause() {
      super.onPause();
      SwrveSDK.onPause();
   }
 
   @Override
   protected void onResume() {
      super.onResume();
      SwrveSDK.onResume(this);
   }
 
   @Override
   protected void onDestroy() {
      SwrveSDK.onDestroy(this);
      super.onDestroy();
   }
 
   @Override
   public void onLowMemory() {
      super.onLowMemory();
      SwrveSDK.onLowMemory();
   }
}
```

### AAR Distribution

If you are using a previously provided JAR file, please use the new AAR package provided with the Android SDK 4.0.

Upgrading to Android SDK v3.4
-
This section provides information to enable you to upgrade to Swrve Android SDK v3.4.

### New onCreate method
The `init` and `initOrBind` methods have been deprecated. Please now use the `onCreate` method. For example:

```
SwrveInstance.getInstance().initOrBind(this, <app_id>, "<api_key>");
```
becomes

```
SwrveInstance.getInstance().onCreate(this, <app_id>, "<api_key>");
```

### Lifecycle Changes

The `onResume` and `onDestroy` methods are now required to pass the current Activity as follows:

```
   @Override
   protected void onResume() {
      super.onResume();
      SwrveInstance.getInstance().onResume(this);
   }
 
   @Override
   protected void onDestroy() {
      SwrveInstance.getInstance().onDestroy(this);
      super.onDestroy();
   }
```

### In-app Message Background Color default

The in-app message background color is now transparent by default. If you want to maintain a solid black background, you must configure it before initializing the SDK as follows:

```
config.defaultBackgroundColor = Color.BLACK;
```

Upgrading to Android SDK v3.3.1
-
No code changes are required to upgrade to Swrve Android SDK v3.3.1.

Upgrading to Android SDK v3.3
-
No code changes are required to upgrade to Swrve Android SDK v3.3.

Upgrading to Android SDK v3.2
-
This section provides information to enable you to upgrade to Swrve Android SDK v3.2.

### Link token and link server deprecated

Cross application install tracking has been deprecated. Please remove any reference to these attributes or methods:

* `SwrveConfig.LinkToken`
* `SwrveConfig.LinkServer`
* `SwrveSDK.clickThru (int gameId, string source)`

Upgrading to Android SDK v3.1
-
This section provides information to enable you to upgrade to Swrve Android SDK v3.1.

### Google Cloud Messaging Push Integration

The GCM integration on Android has been simplified.

If you created the `BroadcastListener` and `Service` in your app code but haven't overridden any of the given methods, remove those classes and point to `com.swrve.sdk.gcm.SwrveGcmBroadcastReceiver` and `com.swrve.sdk.gcm.SwrveGcmIntentService` in the `AndroidManifest.xml` instead.

The configuration is now specified as metadata in `AndroidManifest.xml` under the application tag as follows:

```
<meta-data android:name="SWRVE_PUSH_ICON" android:resource="@drawable/ic_launcher"/>
<meta-data android:name="SWRVE_PUSH_ACTIVITY" android:value=".MainActivity"/>
<meta-data android:name="SWRVE_PUSH_TITLE" android:value="Your app title"/>
```

You do not need to perform any upgrade tasks if you modified either the `BroadcastListener` or `Service` in your app.

Add the following intent filter in the Swrve Broadcast Receiver in your `AndroidManifest.xml`:

```
<intent-filter>
...
   <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
...
</intent-filter>
```

### In-app Messages at Session Start

The Session Start check box has been added to the Set Target screen of the in-app message wizard to enable you to configure in-app message display at session start. If you were previously using the following code for that purpose, you must now remove it:

```
config.setAutoShowMessageAfterDownloadEventNames
```

The session start timeout (the time that messages have to load and display after the session has started) is set to 5,000 milliseconds (5 seconds) by default. You can modify the timeout value using the following:

```
config.setAutoShowMessagesMaxDelay(5000); // milliseconds
```

If the user is on a slow network and the images cannot be downloaded within the timeout period you specify, they are not displayed in that session. They are instead cached and shown at the next session start for that user.

For more information about targeting in-app messages, see [Creating In-App Messages](http://docs.swrve.com/user-documentation/in-app-messaging/creating-in-app-messages/).

Upgrading to Android SDK v3.0
-
This section provides information to enable you to upgrade to Swrve Android SDK v3.0.

### Real-time targeting

Swrve has the ability to update segments on a near real-time basis. Swrve now automatically downloads user resources and campaign messages and keeps them up to date in near real-time during each session in order to reflect the latest segment membership. This is useful for time-sensitive A/B tests and messaging campaigns. These updates are only run if there has been a change in the segment membership of the user, therefore resulting in minimal impact on bandwidth.

Real-time refresh is enabled by default and, if you want to avail of it, you must perform the upgrade tasks detailed below.

* Configuration

  config.autoDownload, which was used to indicate whether campaigns should be downloaded automatically upon app start-up has been replaced with the following:

  ```
  SwrveConfig config = new SwrveConfig();
  config.setAutoDownloadCampaignsAndResources(autoDownload);
  ```

  By default, `config.isAutoDownloadCampaignsAndResources` is set to `true`; as a result, Swrve automatically keeps campaigns and resources up to date.

  If you decide to set it to false, campaigns and resources are always set to cached values at app start-up and it’s up to you to call the following function to update them; for example, at session start or at a key moment in your app flow:

  ```
  swrve.refreshCampaignsAndResources();
  ```

  If you disable `config.isAutoDownloadCampaignsAndResources`, the existing `getUserResources` function no longer works as resources are read from the cache and no longer updated.

* User Resources

  Swrve now automatically downloads user resources and keeps them up to date with any changes. Swrve supplies a Resource Manager to enable you to retrieve the most up-to-date values for resources at all times; you no longer need to explicitly tell Swrve when you want updates.

  If you use Swrve’s new Resource Manager, you no longer need to use the following two functions:

  ```
  void getUserResources(final ISwrveUserResourcesListener listener);
  void getUserResourcesDiff(final ISwrveUserResourcesDiffListener listener);
  ```

  The new resource manager behaves like `getUserResources`; it returns the value of the attribute for the current user. If the value in the Swrve service is changed, the Resource Manager reflects that straight away.

  To call the Resource Manager from the Swrve object, use the following:

  ```
  SwrveResourceManager resourceManager = swrve.getResourceManager();
  ```

  The `SwrveResourceManager` class has a number of methods to retrieve attribute values for specific resources, depending on the expected value type:

  ```
  String getAttributeAsString(String resourceId, String attributeId, String defaultValue);
  int getAttributeAsInt(String resourceId, String attributeId, int defaultValue);
  getAttributeAsFloat(String resourceId, String attributeId, float defaultValue);
  getAttributeAsBoolean(String resourceId, String attributeId, boolean defaultValue);
  ```

  Note that all these methods take default values that are returned when either the resource or the attribute doesn’t exist. For example, to get the price of a sword you might call the following:

  ```
  float price = resourceManager.getAttributeAsFloat("sword", "price", null);
  ```

  These methods always return the most up-to-date value for the attribute.

  Optionally, you can set up a callback function that is called every time updated resources are available. Use this if you want to implement your own resource manager, for example. You can set this as follows as part of `SwrveConfig`:

  ```
  swrve.setResourcesListener(new ISwrveResourcesListener() {
    public void onResourcesUpdated() {
      // Callback functionality
    }
  });
  ```

  The callback function takes no arguments, it just lets you know resources have been updated. You must then use the Resource Manager to get the new values. You can store all resources locally and keep them up to date using the following:

  ```
  swrve.setResourcesListener(new ISwrveResourcesListener() {
    public void onResourcesUpdated() {
      Map<String, SwrveResource> resources = resourceManager.getResources();
    }
  });
  ```

  The callback is called if there is a change in the user resources and also at the start as soon as resources have been loaded from cache.

### Event Intervals

The following functions, which controlled whether and how often events were automatically sent to Swrve, have been removed as Swrve now sends events automatically as part of the real-time targeting feature:

* `public SwrveConfigBase setSendQueuedEventsInterval(long sendQueuedEventsInterval);`
* `public SwrveConfigBase disableSendQueuedEventsInterval();`
* `public long getSendQueuedEventsInterval();`

### Multiple Activities

The code snippet below illustrates how to integrate the new SDK in each activity (or in a base activity). This hooks the same instance of the SDK into your activities to enable the automatic display of a dialog when an in-app message is triggered. As was the case for the previous SDKs, the context is used with a weak reference so it does not leak the activity context.


New integration
```
...
@Override
protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	SwrveInstance.getInstance().initOrBind(this, <app_ID>, "<api_key>");
}
 
@Override
protected void onPause() {
	SwrveInstance.getInstance().onPause();
	super.onPause();
}
 
@Override
protected void onResume() {
	SwrveInstance.getInstance().onResume();
	super.onResume();
}
 
@Override
protected void onDestroy() {
	SwrveInstance.getInstance().onDestroy();
	super.onDestroy();
}
	
@Override
public void onLowMemory() {
	// This will disable the in-app messages for this session
	// to avoid problems with memory
	SwrveInstance.getInstance().onLowMemory();
	super.onLowMemory();
}
...
```

### Dialog Listener

In-app messages are automatically displayed in a custom dialog and are reconstructed with the correct format (if available) when the app rotates. They are also dismissed when the activity is shutting down. You can, however, opt to receive the dialog in a custom listener if you want to manage its lifecycle:

```
swrve.setDialogListener(new ISwrveDialogListener() {
	@Override
	public void onDialog(SwrveDialog dialog) {
		// Manage the dialog instance
		dialog.show();
	}
});
```

### In-app Message Sample Images

You can select the minimum sample size that is used to load in-app message images. Sample size refers to the image stored in memory when an in-app message is displayed. If the sample size calculated by Swrve is less than the given sample size, Swrve now uses the minimum sample size value instead; this results in in-app message images that are less than optimal, but it is a more memory-efficient approach. This is particularly useful for low-end devices.

```
// Minimum image sample size (power of two)
config.setMinSampleSize(4);
```

### Cache folder

You can change where the images are stored on the device by setting the cache folder in the configuration as follows:

```
// Set the image cache folder
config.setCacheDir(externalCacheDir);
```

Upgrading to Android SDK v2.10
-
This section provides information to enable you to upgrade to Swrve Android SDK v2.10.

### Creating an instance of the SDK

To automatically disable the SDK in non-supported versions of Android, the constructor of the SDK has been made private. This means that if you are creating an instance of the Swrve SDK, you must do it with the new SwrveFactory:

```
ISwrve swrve = SwrveFactory.createInstance();
```

You must also update any references to the type Swrve to ISwrve as the returned value may be an active Swrve SDK or a dummy SDK.

### Custom Button Processing

The custom button processing has been divided into two individual listeners: one for custom actions and another for install actions with the ability to override the default behavior.

You must transform your code from the following:

```
SwrveInstance.getInstance().setButtonListener(new ISwrveButtonListener() {

   @Override
    public boolean onAction(SwrveActionType type, String action, int appId) {
        if (type == SwrveActionType.Custom) {
            // Custom code here
            // Notify the button was processed with custom code return true;
        }
        // Normal flow
        return true;
    }
});
```

into the following:

```
SwrveInstance.getInstance().setCustomButtonListener(new ISwrveCustomButtonListener() {
   @Override
   public void onAction(String customAction) {
      // Custom code here
   }
});
```

### In-app Messages

The function `setRootView(View)` is now deprecated as there is no longer a need for a root view. The SDK nows display in-app messages with a custom native dialog. Remove any message listener set with `setMessageListener` so that the SDK can manage in-app messages automatically.

Upgrading to Android SDK v2.9
-
No code changes are required to upgrade to Swrve Android SDK v2.9.

Upgrading to Android SDK v2.8
-
This section provides information to enable you to upgrade to Swrve Android SDK v2.8.

### Moving from game to app

The Swrve SDK method `getAppStoreURLForGame` becomes `getAppStoreURLForApp`:

```
SwrveInstance.getInstance().getAppStoreURLForGame(GAME_ID);
```

becomes

```
SwrveInstance.getInstance().getAppStoreURLForApp(APP_ID);
```

The `SwrveButton` property game id becomes app id:

```
button.getGameId();
```

becomes

```
button.getAppId();
```

### In-app Messaging enabled by default

In-app messaging is now enabled by default for all customers. If you want to disable in-app messaging, use the following configuration:

```
SwrveConfig config = new SwrveConfig();
config.setTalkEnabled(false);
```

Then, initialise the SDK normally but with the following additional configuration:

```
SwrveInstance.getInstance().init(this, appId, apiKey, config);
```

Upgrading to Android SDK v2.7
-
This section provides information to enable you to upgrade to Swrve Android SDK v2.7.

### Automatic User ID generation

In older SDK versions, you had to create you own Swrve user_id during initialisation. From version 2.7, an `ANDROID.ID` is used by default. In order to be backwards compatible with apps which are already live, the following logic applies:

* If you supply a user_id in the config or init method, this one is used.
* If you do not supply a user_id or if it is empty:
 * The SwrveInstance attempts to read a previously saved user ID from disk.
 * If none previously saved user ID exists, it uses ANDROID.ID, if available.
 * If ANDROID.ID is not available, it generates a random UUID.
* The user_id is saved to disk.

### SDK Singleton Access

You must now access the singleton object in the following way:

```
Swrve sdk = SwrveInstance.getInstance();
sdk.METHOD_TO_CALL();
```

### Analytics and In-app Messaging Integration

The SDK is now a single object, so if you were previously using in-app messaging you must upgrade your references to `SwrveTalk` and `SwrveTalkInstance` to point to the Swrve SDK and initialize the new flag in the configuration as follows:

```
// Configure the SDK to use Talk
SwrveConfig config = new SwrveConfig();
config.setTalkEnabled(true);

// Initialise the analytics and in-app messaging SDK
SwrveInstance.getInstance().init(this, APP_ID, API_KEY, USER_ID, config);
```

All the in-app messaging methods are now available in the Swrve class.

Upgrading to Android SDK v2.6
-
This section provides information to enable you to upgrade to Swrve Android SDK v2.6.

### Deprecation of the `buy_in` function

The old buy-in function was used to record purchases of in-app currency that were paid for with real-world money:

```
public void buyIn(String rewardCurrency,
                  int rewardAmount,
                  double localCost,
                  String localCurrency,
                  String paymentProvider);
```

Can be replaced by creating a `SwrveIAPRewards` object with the in-app currency details, and a call to iap() (where <...> matches one of the arguments to `buyIn`):

(if `<paymentProvider>` was `google`)

```
SwrveIAPRewards rewards = new SwrveIAPRewards(<rewardCurrency>, <rewardAmount>);

iapPlay(quantity,
        productId,
        <localCost>,
        <localCurrency>,
        rewards,
        receipt,
        receiptSignature);
```

(for any other value of `<paymentProvider>`)

```
SwrveIAPRewards rewards = new SwrveIAPRewards(<rewardCurrency>, <rewardAmount>);
iap(quantity, productId, <localCost>, <localCurrency>, rewards);
```

The productId should match a resource name in Swrve, and quantity the number of these products purchased.

For example, you might have `productId` = `"bagOfGold"` and `quantity` = `1`, and then record the in-app currencies as `rewardCurrency` = `"gold"` and `rewardAmount` = `200` to mean that the user purchased 1 bag of gold from the app store and for this received 200 gold coins.

Note: Base64 encoding is performed by the Swrve SDK; to avoid duplication of event encoding, pass the unencoded receipt from the Apple IAP transactions through to the IAP function.

Upgrading to Android SDK v2.4
-
This section provides information to enable you to upgrade to Swrve Android SDK v2.4.

### Migrating from Android SDK 2.3 TO Android SDK 2.4

The SDK app store setup has been changed from the `enum SwrveAppStore` to a `string`. You can now provide a custom value and set that app store in the dashboard. The configuration turns into:

* `swrveConfig.setAppStore(“google”);`
* `swrveConfig.setAppStore(“amazon”);`
* `swrveConfig.setAppStore(“other”);`

The SDK has been improved to avoid common instrumentation errors. If you want to configure a custom endpoint URL (ab test, content or link server) you now have to use a `java.net.URL` instance:

* `swrveConfig.setContentUrl(new java.net.URL(“http://customeventurl.com”));`

The link token now is a java.util.UUID. To set it up use the following snippet:

* `swrveConfig.setLinkToken(java.util.UUID.fromString(“uuid_value”));`

The SDK initialisation can now throw exceptions. The SDK throws an exception if:

* Using the Analytics SDK without an user id.
* Using the Analytics SDK without an api key.
* Using the Talk SDK without a link token.
* Using the Talk SDK without a language.
* Using the Talk SDK without an app store.

Also, the SDK will warn if you initialize the SDK with an `Object.toString()` as the user id:

“Please double-check your user id. It seems to be Object.toString(): UserIdStorage@12313”
