Swrve SDK Multiple FCM Providers Sample
---------------------------------------
Example of how to integrate Swrve Push Notifications when your application already makes use of another push notification provider.

It showcases a custom FCM messaging service that relays the information to the Swrve messaging service See:[MyFirebaseMessagingService](src/main/java/com/swrve/sdk/sample/MyFirebaseMessagingService.java)

Android Studio build instructions
---------------------------------
- Import MultipleFCMProviders.
- Replace YOUR_APP_ID in [SampleApplication.java](src/main/java/com/swrve/sdk/sample/minimalintegration/SampleApplication.java) with your Swrve app ID.
- Replace YOUR_API_KEY in [SampleApplication.java](src/main/java/com/swrve/sdk/sample/minimalintegration/SampleApplication.java) with your Swrve API key.
- Replace the google-services.json file with your google-services.json.
- Run MultipleFCMProviders app normally.
