[![CircleCI](https://circleci.com/gh/Swrve/swrve-android-sdk.svg?style=shield)](https://circleci.com/gh/Swrve/swrve-android-sdk)

What is Swrve
-------------
Swrve is a single integrated platform delivering everything you need to drive mobile engagement and create valuable consumer relationships on mobile.  
This native Android SDK will enable your app to use all of these features.

Getting started
---------------
Have a look at the quick integration guide at http://docs.swrve.com/developer-documentation/integration/android/

Installation
------------
The SDK is published to Maven Central in three flavors. Add the one matching your push provider to your app's `build.gradle`:

```groovy
// Firebase Cloud Messaging
implementation 'com.swrve.sdk.android:swrve-firebase:12.3.0'

// Huawei Mobile Services
implementation 'com.swrve.sdk.android:swrve-huawei:12.3.0'

// No push support
implementation 'com.swrve.sdk.android:swrve:12.3.0'
```

Requirements
------------
### Android 6.0 (API 23) or later

Samples
-------
Have a look at the samples in the [samples folder](samples/README.md).

Building from source
--------------------
Most integrations should use the Maven Central artifacts above. To build the AAR yourself, run the following from the `SwrveSDK` folder:

`../gradlew clean build assemble`

The AAR is generated in `build/outputs/aar`, one per flavor: `firebase` for FCM push, `huawei` for HMS push, and `core` for no push.

Contributing
------------
We would love to see your contributions! Follow these steps:

1. Fork this repository.
2. Create a branch (`git checkout -b my_awesome_feature`)
3. Commit your changes (`git commit -m "Awesome feature"`)
4. Push to the branch (`git push origin my_awesome_feature`)
5. Open a Pull Request.

License
-------
© Copyright Swrve Mobile Inc or its licensors. Distributed under the [Apache 2.0 License](LICENSE).  
Google Play Services Library Copyright © 2012 The Android Open Source Project. Licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).  
Gradle Copyright © 2007-2011 the original author or authors. Licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).
