# Geo Sample Project
 
### SwrveGeoSDK
* See [SwrveSDK documentation](https://docs.swrve.com/developer-documentation/integration/android/) for installing SwrveSDK.
* See [SwrveGeoSDK documentation](https://docs.swrve.com/developer-documentation/integration/swrve-geo-sdk/) for installing SwrveGeoSDK.
* Use the `permissionPrePrompt` config to explain why the location permission is required. Alternatively, configure your own messaging with your custom app look and feel before calling `SwrveGeoSDK.start`. Note that the instructions for allowing the permission varies accross OS level. See the `pre_prompt_rationale` string text in res/values, res/values-v29, res/values-v30, etc as examples.
* Call `SwrveGeoSDK.init` directly after `SwrveSDK.createInstance` to initialise. See [SampleApplication](app/src/main/java/com/swrve/sdk/geo/sample/SampleApplication.java) for example of init.
* See [MainActivity](app/src/main/java/com/swrve/sdk/geo/sample/MainActivity.java) for example of timing the location permission request.

### License
© Copyright Swrve Mobile Inc or its licensors. Distributed under the [Apache 2.0 License](LICENSE).  
