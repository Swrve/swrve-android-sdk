package com.swrve.sdk.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;

interface ISwrveGcmHandler {

    @Deprecated
    boolean onHandleIntent(Intent intent, GoogleCloudMessaging gcm);

    boolean onMessageReceived(String from, Bundle data);

    void processNotification(Bundle msg);

    boolean mustShowNotification();

    int showNotification(NotificationManager notificationManager, Notification notification);

    NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg);

    Notification createNotification(Bundle msg, PendingIntent contentIntent);

    PendingIntent createPendingIntent(Bundle msg);

    Intent createIntent(Bundle msg);
}
