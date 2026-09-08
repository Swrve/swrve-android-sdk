Swrve SDK Samples
-----------------
- [Push Notifications](PushNotificationSample/)
- [Multiple push providers](MultiplePushProvidersSample/)
- [Message Center API](MessageCenterSample/)
- [User Identity](IdentitySample/)
- [Push Inbox](PushInboxSample/)
- [Embedded Campaigns](SwrveEmbeddedSample/)

Each sample is a standalone Android Studio project with its own Gradle wrapper — open the sample's
directory, not the SDK repo root. They consume the published Swrve SDK from Maven Central, so no
part of this repository needs to be built first.

The samples target a higher `minSdk` than the SDK does, so they can use Jetpack Compose. See each
sample's `app/build.gradle` for its actual values, and the [Android integration
guide](https://docs.swrve.com/developer-documentation/integration/android/) for the SDK's own
supported versions.

To build a sample against local SDK source instead of the published artifact,
uncomment *two* things in that sample and re-sync — the `includeBuild` block in its
`settings.gradle` and `missingDimensionStrategy` in its `app/build.gradle`. Both are needed: the SDK
project publishes several artifacts (`swrve`, `swrve-firebase`, `swrve-huawei`) from one Gradle
project, so the substitution and the flavour have to be named explicitly.
