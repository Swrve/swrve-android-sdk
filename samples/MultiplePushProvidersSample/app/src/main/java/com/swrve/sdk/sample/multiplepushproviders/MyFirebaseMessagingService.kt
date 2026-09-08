package com.swrve.sdk.sample.multiplepushproviders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.swrve.sdk.SwrveSDK

/**
 * This is the whole point of this sample.
 *
 * Only one service can own the `com.google.firebase.MESSAGING_EVENT` intent, so when your app declares its own, Swrve's `SwrveFirebaseMessagingService` never runs.
 * Every message — Swrve's included — arrives here instead, and it is your job to route it.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    /** Swrve needs the FCM token to send to this device. Forward every refresh — miss it and everything still compiles and runs while push never arrives. */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        SwrveSDK.setRegistrationId(token)
        // Give the token to your other push provider here too.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Ask each provider in turn whether the message is one of its own, then delegate.
        if (SwrveSDK.isSwrvePush(remoteMessage.data)) {
            val handled = SwrveSDK.handleSwrvePush(
                this,
                remoteMessage.data,
                remoteMessage.messageId,
                remoteMessage.sentTime
            )
            Log.v(TAG, if (handled) "Swrve handled the push" else "Swrve did not handle the push")

//        // Your other provider goes here, in the same shape — check its own docs for the equivalent of isSwrvePush:
//        } else if (otherProvider.isMine(remoteMessage)) {
//        // hand off to that SDK, which posts its own notification

        } else {
            Log.v(TAG, "Not a Swrve push — handling it ourselves")
            showNonSwrveNotification(remoteMessage)
        }
    }

    /**
     * Stand-in for whatever your other push SDK would do with its own messages, so that the non-Swrve branch is visible rather than only appearing in the log.
     */
    private fun showNonSwrveNotification(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Non-Swrve push"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Routed by MyFirebaseMessagingService"

        val notification = NotificationCompat.Builder(this, SampleApplication.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.mg_notification_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(NON_SWRVE_NOTIFICATION_ID, notification)
        } else {
            Log.w(TAG, "Notification permission not granted — cannot show the non-Swrve push")
        }
    }

    private companion object {
        const val TAG = "MyFirebaseMsgService"
        const val NON_SWRVE_NOTIFICATION_ID = 1001
    }
}
