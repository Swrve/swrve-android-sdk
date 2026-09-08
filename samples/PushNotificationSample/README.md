# Push Notification Sample

Baseline Swrve SDK integration with Google FCM push notifications, in Kotlin and Jetpack Compose.

This is the smallest complete push setup: create the SDK, create a notification channel, tell Swrve which icons and colour to use, and trigger the Android 13+ notification permission prompt.

## Why this exists

Push is the most common reason to integrate the SDK, and it is the part with the most moving pieces — a notification channel you own, a small icon that must be a valid status-bar drawable, an activity to open on tap, and on Android 13+ a runtime permission that the OS only lets you request once or twice. Getting any of them wrong fails quietly: no crash, just no notification.

## Setup

1. Open `PushNotificationSample` in Android Studio (a standalone project with its own Gradle wrapper — open this directory, not the SDK repo root).
2. Replace [`app/google-services.json`](app/google-services.json) with the one from your Firebase project. The bundled file is a placeholder and push will not work with it. Its `package_name` must match the `applicationId` in [`app/build.gradle`](app/build.gradle) — currently `com.swrve.sdk.sample.pushnotification` — or the build fails with *"No matching client found for package name"*. Change whichever of the two is easier; nothing else moves with it.
3. In [`SampleApplication.kt`](app/src/main/java/com/swrve/sdk/sample/pushnotification/SampleApplication.kt), replace `YOUR_APP_ID` and `YOUR_API_KEY` with your own, from the Swrve dashboard under **Settings → Integration settings**.
4. Run the app, grant notification permission, and send yourself a push. The button appears only while the permission is outstanding — once granted it is replaced by a confirmation.

## Key API calls

Everything that matters is in [`SampleApplication.kt`](app/src/main/java/com/swrve/sdk/sample/pushnotification/SampleApplication.kt) — read that file for the working version. The SDK must be created in `Application.onCreate`, once, before anything else uses it.

The shape of it:

```kotlin
val config = SwrveConfig()

config.notificationConfig = SwrveNotificationConfig.Builder(smallIconId, channel)
    .activityClass(MainActivity::class.java)   // opened when the notification is tapped
    .largeIconDrawableId(largeIconId)
    .accentColorHex("#RRGGBB")
    .pushNotificationPermissionEvents(listOf("notification_permission_request"))
    .build()

SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config)
```

**Trigger the OS permission prompt** by sending one of the events named above:

```kotlin
SwrveSDK.event("notification_permission_request")
```

## Good to know

- **Channel creation is guarded.** Notification channels only exist from Android 8 (API 26); below that you pass `null` and the SDK posts without one. See `createDefaultNotificationChannel()` in `SampleApplication.kt`.
- **You do not need to declare `SwrveFirebaseMessagingService` in your manifest.** The SDK declares it and it merges into your app automatically — deliberately at `android:priority="-1"`, so that if you later add your own `FirebaseMessagingService` yours takes precedence. Re-declaring it adds a default-priority filter and defeats that.

- **Ask for permission at the right moment.** Android stops showing the prompt after the user declines twice, so a denial is close to permanent. Send the trigger event after your UI has explained why you want notifications, not on first launch.
- **Which is why permission is usually asked for with an in-app message rather than app code.** A campaign can ask at a moment that makes sense, in your own words, with a button that requests notification permission — and a later campaign can send users who have already declined straight to their notification settings. Both are configured in the dashboard, so your app needs no permission handling of its own. The button in this sample is a shortcut so you can test push without setting up a campaign.
- **The small icon must be a silhouette.** Android renders it as a mask in the status bar, so a full-colour logo appears as a solid blob.
- **For Huawei (HMS) instead of FCM**, use the `swrve-huawei` artifact and follow the Huawei tab of the [Android integration guide](https://docs.swrve.com/developer-documentation/integration/android/).
- **This sample assumes Swrve owns push handling.** If your app already has its own `FirebaseMessagingService`, this setup does not apply — only one service can own the FCM messaging intent. See [MultiplePushProvidersSample](../MultiplePushProvidersSample/).
- **Credentials here are sample-grade.** Hardcoding an API key in source is fine for a sample and wrong for a real app — load it from your build configuration or a secure config provider.


