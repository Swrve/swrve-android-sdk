Swrve SDK Advanced Push Notifications Sample
--------------------------------------------
Example of how to create an advanced UI for your push notifications, taking advantage of Android's latest features and Swrve's custom payloads for extra data.

This example extends and replaces the SwrveGcmIntentService to provide a big notification style and grouping:
- [BigNotificationSwrveGcmIntentService](src/main/java/com/swrve/sdk/sample/BigNotificationSwrveGcmIntentService.java)


For more information on Android's notification features have a look at the following links:
- http://developer.android.com/guide/topics/ui/notifiers/notifications.html
- http://developer.android.com/training/notify-user/expanded.html
- http://developer.android.com/training/wearables/notifications/stacks.html
- http://developer.android.com/guide/topics/ui/notifiers/notifications.html

Android Studio build instructions
---------------------------------
- Import AdvancedPushNotifications.
- Replace YOUR_APP_ID in SampleApplication.java with your Swrve app ID.
- Replace YOUR_API_KEY in SampleApplication.java with your Swrve API key.
- Replace YOUR_SENDER_ID in SampleApplication.java with your Google Cloud Messaging Sender ID if you want to do push notifications.
- Run AdvancedPushNotifications app normally.
