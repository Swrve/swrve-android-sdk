Android SDK Release Notes
=

For upgrade instructions, see [Android SDK Upgrade Guide](/docs/upgrade_guide.md).

- [Release 4.2](#release-42)
- [Release 4.1](#release-41)
- [Release 4.0.5](#release-405)
- [Release 4.0.4](#release-404)
- [Release 4.0.3](#release-403)
- [Release 4.0.2](#release-402)
- [Release 4.0.1](#release-401)
- [Release 4.0](#release-40)
- [Release 3.4](#release-34)
- [Release 3.3.1](#release-331)
- [Release 3.3](#release-33)
- [Release 3.2](#release-32)
- [Release 3.1](#release-31)
- [Release 3.0.1](#release-301)
- [Release 3.0](#release-30)
- [Release 2.10.1](#release-2101)
- [Release 2.10](#release-210)
- [Release 2.9](#release-29)
- [Release 2.8](#release-28)
  - [Release 2.8 Push Beta](#release-28-push-beta)
- [Release 2.7.1](#release-271)
- [Release 2.7](#release-27)
- [Previous Releases Summary](#previous-releases-summary)

Release 4.2
-
Release Date: February 11, 2016

Android SDK release 4.2 is focused on the following:

* New Message Center API enables you to list conversations and/or in-app messages in the app’s inbox. For more information, see the Swrve Message Center API Guide.
* Upgrade of the GCM library to 8.4.0.
* The SDK is ready to work with the future release of Location-based Campaigns.

Android SDK release 4.2 includes the following bug fixes:

* Improvements to the campaign state of in-app messages so it doesn’t get overwritten when campaigns are refreshed from the server.
* Fixed an issue where apps with an Activity configured with singleTask launchMode were not logging push notification engagements events. For more information, see the onNewIntent method in the [Android SDK Upgrade Guide](/docs/upgrade_guide.md).

Release 4.1
-
Release Date: November 30, 2015

Android SDK release 4.1 is focused on the following:

* Custom events beginning with Swrve. or swrve. are now blocked and no longer sent by the SDK.
* It is now easier to configure the SDK for EU data storage. For more information see [How Do I Configure the Swrve SDK for EU Data Storage?](http://docs.swrve.com/faqs/sdk-integration/configure-sdk-for-eu-data-storage/)
* It is now possible to turn off the Swrve SDK logs. For more information, see [How Do I Disable SDK Device Logging?](http://docs.swrve.com/faqs/sdk-integration/disable-sdk-device-logging/)
* It is now possible to log Google Advertising ID and Android ID as user properties. For more information, see [How Do I Log Advertising and Vendor IDs?](http://docs.swrve.com/faqs/app-integration/log-advertising-and-vendor-ids/)
* New flag added to control the status bar when displaying in-app messages.
* Added new Material icon, big icon and accent support for notifications.
* Conversations now rotate if the screen width is more than 600 dp.
* The SDK now logs device region.
* Updated demo app to use Google Material Design.
* When building with Gradle there is no need to declare `INTERNET` / `WRITE_EXTERNAL_STORAGE` permissions as they will be pulled from the SDK’s `AndroidManifest.xml` automatically.
* Android source has been refactored into one module producing a core library flavor of the SDK and a Google library flavor of the SDK.

Android SDK release 4.1 includes the following bug fix:

* Fixed issue with message rules so that resetting rules for QA users now only occurs once.

Release 4.0.5
-
Release Date: November 5, 2015

Android SDK release 4.0.5 is focused on the following:

* Updated Google Cloud Messaging libraries to 8.1.0.

Android SDK release 4.0.5 includes the following bug fix:

* Fixed an issue with Conversation impression and minimum delay rules.

Release 4.0.4
-
Release Date: October 21, 2015

Android SDK release 4.0.4 includes the following bug fix:

* Fixed issues that occurred when obtaining new tokens on the ID listener service.

Release 4.0.3
-
Release Date: October 15, 2015

Android SDK release 4.0.3 is focused on the following:

* Added compatibility with Android Marshmallow.
* Removed dependency on `HttpClient` as this is not available by default in Marshmallow.
* Configuring cache directory via `SwrveConfig` now requires the user’s permission when the app is compiled for Marshmallow. A dialog similar to below will appear on fresh installs to request permission. If permission is denied, then the internal cache will be used.

Release 4.0.2
-
Release Date: September 23, 2015

Android SDK release 4.0.2 is focused on the following:

* The push notifications plugin has been updated to the latest GCM library.
* The HTTP default timeout has been increased and you can now configure this setting.

Android SDK release 4.0.2 includes the following bug fixes:

* Fixed issue with in-app messages not appearing because of a NullPointerException.

Release 4.0.1
-
Release Date: August 18, 2015

Android SDK release 4.0.1 includes the following bug fix:

* Fixed issues with displaying bold and italic text styles in Conversations.

Release 4.0
-
Release Date: July 7, 2015

Android SDK release 4.0 is focused on the following:

* Added support for Conversations. For more information, see [Intro to Conversations](http://docs.swrve.com/user-documentation/conversations/intro-to-conversations/).
* Moved from JAR to AAR distribution.
* The `SwrveInstance` method has been deprecated and replaced with `SwrveSDK` singleton class. For more information, see [Android SDK Upgrade Guide](/docs/upgrade_guide.md).
* In-app message buttons configured as deeplinks are now opened as URLs by default.

Android SDK release 4.0 includes the following bug fixes:

* Fixed issues with app activity lifecycle and displaying of in-app messages.

Release 3.4
-
Release Date: April 8, 2015

Android SDK release 3.4 is focused on the following:

* The `init` and `initOrBind` methods have been deprecated in favor of a new `onCreate` method.
* The default background for in-app messages has been changed from solid black to transparent. You can configure the background color in the code, or contact your Customer Success Manager to configure it in the in-app message wizard template.
* Uses HTTPS for events by default.
* Removal of `android-support-v4.jar` from dependencies. It is now pulled from the Maven repository using Gradle script.

Android SDK release 3.4 includes the following bug fixes:

* In-app message problems related to Android activity lifecycle have been resolved with changes to the `onCreate`, `onResume` and `onDestroy` methods.
* The SDK will only display push notifications that come from Swrve as opposed to push notifications from a customer’s app.

Release 3.3.1
-
Release Date: January 21, 2015

Android SDK release 3.3.1 is focused on the following:

* The SDK now automatically captures and sends referrer information. For more information, see [User Acquisition Tracking Guide](http://docs.swrve.com/developer-documentation/technical-resources/user-acquisition-tracking-guide/).
* A new configuration parameter allows you to choose if you want to wait to load the previous user resources and campaigns cache when initializing the SDK. For more information, see [Instrumenting In-App Messaging](http://docs.swrve.com/developer-documentation/advanced-integration/instrumenting-in-app-messaging/).
* The SDK now pulls campaigns and resources only when events are sent.

Android SDK release 3.3.1 includes the following bug fixes:

* Push notifications now have unique IDs, resolving an issue that was causing multiple notifications on a device to all have the same payload.
* Campaign impressions are now maintained in all cases after a reload.

Release 3.3
-
Release Date: November 11, 2014

Android SDK release 3.3 is focused on the following:

* The SDK now logs mobile carrier information by default. Swrve tracks the name, country ISO code and carrier code of the registered SIM card. You can use the country ISO code to track and target users by country location.

Android SDK release 3.3 includes the following bug fixes:

* Better error notification when an empty response is received by the server.

Release 3.2
-
Release Date: October 21, 2014

Android SDK release 3.2 is focused on the following:

* SDKs no longer use Android IDs as device ids. The SDK now generates a random unique user id if no custom user id is provided at initialization.
* Deprecated cross application install tracking.
* Added Crashlytics metadata if the app uses the Crashlytics SDK.
* Use http for in-app campaigns requests.

Android SDK release 3.2 includes the following bug fixes:

* In-app message interval is also calculated from when the message is dismissed.
* Avoids crashing when a QA user goes back to being a normal user.
* Read secure values from cache at the same time as the signature. Avoids `invalid_signature` problems.
* Close database cursors and statements even after a exception.
* Rotate one-format in-app messages correctly to maintain appearance.

Release 3.1
-
Release Date: July 30, 2014

Android SDK release 3.1 is focused on the following:

* Google Cloud Messaging push notification integration has now been simplified. There is no longer any need to include the broadcast listener and service in the main app package; it is sufficient to target the broadcast listener and service provided with the SDK.
* Support for in-app message delivery at session start has been enhanced.
* The `android.permission.ACCESS_WIFI_STATE` and `android.permission.CHANGE_WIFI_STATE` permissions have been removed.

Android SDK release 3.1 includes the following bug fix:

* An issue whereby an attempt was made to send logging data to QA devices despite logging being disabled has been fixed.
* Google Cloud Messaging registration ids are also obtained from the Broadcast Receiver to workaround Google Play bugs.

Release 3.0.1
-
Release Date: May 27, 2014

Android SDK release 3.0.1 includes the following bug fixes:

* An error whereby an exception on the UI listener could cause an app to crash has been corrected.
* An issue whereby `SwrveIAPRewards` caused a crash on unsupported Android versions (Java 1.5) has been corrected.

Release 3.0
-
Release Date: May 15, 2014

Android SDK release 3.0 is focused on the following:

* Real-time targeting enhancements – Swrve now automatically downloads user resources and campaign messages and keeps them up to date in near real-time during each session in order to reflect the latest segment membership. This is useful for time-sensitive A/B tests and messaging campaigns.
* The Android SDK now includes separate JAR files for the plain vanilla SDK (`swrvesdk.jar`) and the Google Play Android SDK (`swrvegoogle.sdk`) respectively. This separation has been made because the Google Play SDK requires the use of Google external libraries that may not work or may prevent the app from being submitted in different app stores. No code changes are required to use either option, except when using either IAP Google Play or Google Cloud Messaging push notifications.
* Functions which controlled whether and how often events were automatically sent to Swrve have been removed as Swrve now sends events automatically as part of the real-time targeting feature.
* Support for multiple activities is now available.
* A dialog listener is now available to control in-app message dialogs.
* It is now possible to select the minimum sample size that is used to load in-app message images.
* It is now possible to change the image cache folder.

Android SDK release 3.0 includes the following bug fixes:

* A patch for a HTTP Google bug has been implemented.
* An issue with Google app store linking for in-app messages has been fixed.
* A potential race condition whereby SQLite connections could be used after the SDK was destroyed has been fixed.

Release 2.10.1
-
Release Date: April 3, 2014

An issue with push notification initialization has been fixed. This issue only affects customers using Swrve’s push notification functionality.

Release 2.10
-
Release Date: April 1, 2014

Android SDK release 2.10 is focused on the following:

* Processing of the Install and Deep Link in-app message actions has been simplified.
* Registration of QA devices has been simplified.
* All events can now act as triggers for in-app messages and push notifications.
* The SDK now runs on Android 2.2.x version devices (such as API Level 8 and Froyo) and it gracefully does nothing (that is, it does not send any data or campaigns; it acts as a dummy SDK).

Android SDK release 2.10 includes the following bug fixes:

* In-app messaging orientation issues have been fixed.
* The SDK now sets a client_time timestamp in every event in each batch.

Release 2.9
-
Release Date: March 4, 2014

Android SDK release 2.9 is focused on the following:

* Push notifications are now supported for Android devices.
* The automatic campaign event is now changed to `swrve.messages_downloaded`.
* Campaign, user resources and saved events signing processes have been added.
* GZIP support for campaigns and user resources has been added.

Release 2.8
-
Android SDK release 2.8 is focused on Swrve’s redesigned in-app messaging functionality. It includes the following:

* Swrve’s in-app message format rules are now enforced.
* The device friendly name is no longer logged as a user property.
* For information about upgrading to Android SDK 2.8, see Swrve Android SDK Upgrade Guide.

### Release 2.8 Push Beta

This SDK release contains the same updates as the 2.8 release but also enables you to send push notifications to Android devices on the Google Play app store. If you're interested in Beta testing push notifications, contact your Swrve account manager for more information.

Release 2.7.1
-
Android SDK release 2.7.1 fixes a bug which caused events to be sent to Swrve in a different order than they were queued by your app. This caused funnel reporting to be incorrect for android users in some cases. If you are using funnels in your dashboard you should upgrade to this SDK.

Release 2.7
-
Android SDK release 2.7 is focused on the auto-generation of user IDs and the unification of the in-app messaging and analytics SDKs. It includes the following:

* User IDs are now auto-generated (though they can be overwritten).
* The in-app messaging and analytics SDKs are now unified.
* For information about upgrading to Android SDK 2.7, see Swrve [Android SDK Upgrade Guide](/docs/upgrade_guide.md).

Previous Releases Summary
-
* Nov 12, 2013 – v2.6 – Added support for extended IAP event and bug fixes.
* Oct 16, 2013 – v2.5 – Added support for in-app messaging per campaign dismissal rules and bug fixes.
* Sep 17, 2013 – v2.4 – Added support for in-app messaging QA logging.
* Aug 20, 2013 – v2.3 – Added support for in-app messaging QA user functionality.
* July 26, 2013 – v2.2 – Bug fixes.
* July 2, 2013 – v.2.1 – Added support for app store filtering within in-app messaging.
* May 22, 2013 – v.2.01 – Fixed possible crash while displaying messages whose images were not fully downloaded.
* May 17, 2013 – First public release.
