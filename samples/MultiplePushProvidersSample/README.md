# Multiple Push Providers Sample

How to integrate Swrve push when your app already uses another push provider, in Kotlin and Jetpack Compose.

## Why this exists

Only one service can own the `com.google.firebase.MESSAGING_EVENT` intent, no matter how many SDKs declare one. Normally the winner is Swrve's `SwrveFirebaseMessagingService`, which the SDK declares for you and which needs no work on your part — see [PushNotificationSample](../PushNotificationSample/) for that setup.

**If another push SDK in your app also listens for that intent, you need your own `FirebaseMessagingService` to act as the single owner and delegate.** Without one, the two SDKs compete for the intent, one of them loses, and its pushes silently stop arriving — no crash, no error, just nothing.

Once you declare your own service, **every** message arrives there, Swrve's included, and it becomes your job to work out which SDK each one belongs to and hand it over. That routing is what this sample demonstrates.

Swrve declares its service at `android:priority="-1"` precisely so that an app-declared service wins this contest cleanly, rather than the outcome depending on merge order.

## Setup

1. Open `MultiplePushProvidersSample` in Android Studio (a standalone project with its own Gradle wrapper — open this directory, not the SDK repo root).
2. Replace [`app/google-services.json`](app/google-services.json) with the one from your Firebase project. The bundled file is a placeholder and push will not work with it. Its `package_name` must match the `applicationId` in [`app/build.gradle`](app/build.gradle) — currently `com.swrve.sdk.sample.multiplepushproviders` — or the build fails with *"No matching client found for package name"*. Change whichever of the two is easier; nothing else moves with it.
3. In [`SampleApplication.kt`](app/src/main/java/com/swrve/sdk/sample/multiplepushproviders/SampleApplication.kt), replace `YOUR_APP_ID` and `YOUR_API_KEY` with your own, from the Swrve dashboard under **Settings → Integration settings**.
4. Run the app, grant notification permission, and send yourself a push. The button appears only while the permission is outstanding — once granted it is replaced by a confirmation.

## Key API calls

The routing lives in [`MyFirebaseMessagingService.kt`](app/src/main/java/com/swrve/sdk/sample/multiplepushproviders/MyFirebaseMessagingService.kt) — read that file for the working version. It has two jobs.

**Forward the FCM token**, so Swrve can reach this device:

```kotlin
override fun onNewToken(token: String) {
    SwrveSDK.setRegistrationId(token)
    // your other provider gets the token here too
}
```

**Ask before delegating**, for each provider in turn:

```kotlin
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    if (SwrveSDK.isSwrvePush(remoteMessage.data)) {
        SwrveSDK.handleSwrvePush(
            this, remoteMessage.data, remoteMessage.messageId, remoteMessage.sentTime
        )
    // } else if (otherProvider.isMine(remoteMessage)) {
    //     // hand off to that SDK
    } else {
        showNonSwrveNotification(remoteMessage)
    }
}
```

`handleSwrvePush` returns a `Boolean`, so you can log or fall through when Swrve declines a message it was offered.

Each branch logs to logcat under the `MyFirebaseMsgService` tag, so filter on that to watch routing decisions as pushes arrive.

The `else` branch stands in for whatever your other push SDK would do with its own messages — it posts a plain notification so the non-Swrve path is visible on screen rather than only in the log. A real integration delegates to that SDK instead.

The manifest declaration that redirects delivery here is in [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml).

## Good to know

- **Everything else matches [PushNotificationSample](../PushNotificationSample/).** Notification channel, icons, accent colour and the Android 13+ permission prompt all work identically, so this sample deliberately does not repeat that material. Read that one first.
- **Register your other provider's dependency.** The commented-out line in `app/build.gradle` shows where it goes, and the `if`/`else if` chain in `MyFirebaseMessagingService` is shaped so adding a second branch is obvious.
- **Do not declare Swrve's service as well.** Adding `SwrveFirebaseMessagingService` to your manifest alongside your own creates two competing intent filters and defeats the SDK's `-1` priority.
- **Credentials here are sample-grade.** Hardcoding an API key in source is fine for a sample and wrong for a real app.


