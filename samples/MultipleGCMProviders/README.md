Swrve SDK Multiple GCM Providers Sample
---------------------------------------
Example of how to integrate Swrve Push Notifications when your application already makes use of another push notification provider.

It showcases a custom GCM receiver to intercept push notifications that were intended for Swrve and redirect them, leaving all the others to the other provider:
- [CustomGcmReceiver](src/main/java/com/swrve/sdk/sample/CustomGcmReceiver.java)

Android Studio build instructions
---------------------------------
- Import MultipleGCMProviders.
- Replace YOUR_APP_ID in SampleApplication.java with your Swrve app ID.
- Replace YOUR_API_KEY in SampleApplication.java with your Swrve API key.
- Replace YOUR_SENDER_ID in SampleApplication.java with your Google Cloud Messaging Sender ID if you want to do push notifications.
- Run MultipleGCMProviders app normally.
