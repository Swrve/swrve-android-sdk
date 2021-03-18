[![CircleCI](https://circleci.com/gh/Swrve/swrve-android-sdk.svg?style=shield)](https://circleci.com/gh/Swrve/swrve-android-sdk)

What is Swrve
-------------
Swrve is a single integrated platform delivering everything you need to drive mobile engagement and create valuable consumer relationships on mobile.  
This native Android SDK will enable your app to use all of these features.

Getting started
---------------
Have a look at the quick integration guide at http://docs.swrve.com/developer-documentation/integration/android/

Requirements
------------
### Android 14+
The SDK supports Android API 14+ but will handle older versions with a dummy SDK.

### Gradle (distributed with the SDK)
Used to build the SDK and its dependencies.

Samples
-------
Have a look at the samples in the [samples folder.](samples/README.md)

How to build the SDK
--------------------
To build the Swrve AAR library, run the following command from the SwrveSDK folder:
`../gradlew clean build assemble`
This will generate the AAR library in the build/outputs/aar folder. Use the `google` AAR for push related campaigns.

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
