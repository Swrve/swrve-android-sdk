package com.swrve.sdk.sample;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.gcm.SwrveGcmIntentService;

import java.io.IOException;

/*
 * Use this class when you want to create richer local notifications form a push notification
 * using the BigPicture style and grouping and/or others.
 * http://developer.android.com/guide/topics/ui/notifiers/notifications.html
 * http://developer.android.com/training/notify-user/expanded.html
 * http://developer.android.com/training/wearables/notifications/stacks.html
 */
public class BigNotificationSwrveGcmIntentService extends SwrveGcmIntentService {

    @Override
    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        String msgText = msg.getString("text");
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            // Customizable title with the custom payload 'title'
            String msgTitle = msg.getString("title");
            if (SwrveHelper.isNullOrEmpty(msgTitle)) {
                msgTitle = "Send 'title' payload to customize";
            }
            // Customizable big style summary with the custom payload 'summary'
            String msgSummary = msg.getString("summary");
            if (SwrveHelper.isNullOrEmpty(msgSummary)) {
                msgSummary = "Send 'summary' payload to customize";
            }
            // Group notifications
            String msgGroup = msg.getString("group");
            if (SwrveHelper.isNullOrEmpty("group")) {
                msgGroup = "group_key_emails";
            }

            Bitmap largeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_large_image);
            // NOTE: You could also download an image using a library like Picasso and a custom payload 'image_url'
            NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle()
                    .setSummaryText(msgSummary)
                    .bigPicture(largeBitmap);

            boolean materialDesignIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setAutoCancel(true)
                    .setContentTitle(msgTitle)
                    .setContentText(msgText)
                    .setContentIntent(contentIntent)
                    .setGroup(msgGroup)
                    .setStyle(bigPictureStyle);

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
