package com.swrve.sdk.demo;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.gcm.SwrveGcmIntentService;

/*
 * Use this class when you want to create richer local notifications form a push notification
 * using a custom layout.
 * http://developer.android.com/guide/topics/ui/notifiers/notifications.html
 */
public class CustomLayoutSwrveGcmIntentService extends SwrveGcmIntentService {

    @Override
    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        String msgText = msg.getString("text");
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.custom_notification);

            // Fill the notification with data
            remoteViews.setTextViewText(R.id.notification_title, msgText);

            // You can provide another intent to this button to do a different action
            remoteViews.setOnClickPendingIntent(R.id.notification_button, contentIntent);
            String buttonText = msg.getString("button");
            if (buttonText != null && buttonText.length() != 0) {
                remoteViews.setTextViewText(R.id.notification_button, buttonText);
            }

            // Build notification
            boolean materialDesignIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setContent(remoteViews)
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent);

            if (materialDesignIcon) {
                mBuilder.setSmallIcon(R.mipmap.ic_launcher_material);
            } else {
                mBuilder.setSmallIcon(R.mipmap.ic_launcher);
            }

            String msgSound = msg.getString("sound");
            if (!SwrveHelper.isNullOrEmpty(msgSound)) {
                Uri soundUri;
                if (msgSound.equalsIgnoreCase("default")) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                } else {
                    String packageName = getApplicationContext().getPackageName();
                    soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/raw/" + msgSound);
                }
                mBuilder.setSound(soundUri);
            }
            return mBuilder.build();
        }

        return null;
    }
}
