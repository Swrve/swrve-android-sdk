package com.swrve.sdk.push;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrvePushHelper;
import com.swrve.sdk.SwrvePushSDK;
import com.swrve.sdk.SwrvePushService;

import java.util.Map;

public class SwrvePushServiceDefault implements SwrvePushService {

    private static final int JOB_ID = R.integer.swrve_push_service_default_job_id;

    private SwrvePushSDK pushSDK;

    /**
     * Use this method to override Swrve's implementation of another push provider such as
     * FirebaseInstanceIdService with your own implementation. See samples directory in the public
     * repository on how to use it.
     * @param context A context
     * @param data A map containing swrve properties used for silent push or rich notifications.
     * @return true if successfully scheduled, false otherwise
     */
    public static boolean handle(Context context, Map<String, String> data) {
        boolean handled = false;
        if (data != null) {
            Bundle extras = new Bundle();
            for (String key : data.keySet()) {
                extras.putString(key, data.get(key));
            }
            handled = handle(context, extras);
        }
        return handled;
    }

    /**
     * Use this method to override Swrve's implementation of another push provider such as
     * GcmListenerService with your own implementation. See samples directory in the public
     * repository on how to use it.
     * @param context A context
     * @param intent An intent containing a bundle of swrve properties used for silent push or rich notifications.
     * @return true if successfully scheduled, false otherwise
     */
    public static boolean handle(Context context, Intent intent) {
        boolean scheduled = false;
        Bundle extras = intent.getExtras();
        if (extras != null) {
            scheduled = handle(context, extras);
        }
        return scheduled;
    }

    /**
     * Use this method to process a swrve rich notification. Requires swrve push properties to be part
     * of the bundle.
     * @param context A context
     * @param extras A bundle containing swrve properties used for silent push or rich notifications.
     * @return true if successfully scheduled, false otherwise
     */
    public static boolean handle(Context context, Bundle extras) {
        boolean handled = false;
        if (!SwrveHelper.isSwrvePush(extras)) {
            SwrveLogger.d("Not a Swrve push.");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent();
            intent.putExtras(extras);
            SwrvePushServiceDefaultJobIntentService.enqueueWork(context, SwrvePushServiceDefaultJobIntentService.class, JOB_ID, intent);
            handled = true;
        } else {
            Intent intent = new Intent(context, SwrvePushServiceDefaultReceiver.class);
            intent.putExtras(extras);
            context.sendBroadcast(intent);
            handled = true;
        }
        return handled;
    }

    public SwrvePushServiceDefault() {
        pushSDK = SwrvePushSDK.getInstance();
        if (pushSDK != null) {
            pushSDK.setService(this);
        }
    }

    @Override
    public void processNotification(final Bundle msg) {
        if (pushSDK != null) {
            pushSDK.processNotification(msg);
            SwrvePushHelper.qaUserPushNotification(msg);
        }
    }

    @Override
    public boolean mustShowNotification() {
        return true;
    }

    @Override
    public int showNotification(NotificationManager notificationManager, Notification notification) {
        return pushSDK.showNotification(notificationManager, notification);
    }

    @Override
    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        return pushSDK.createNotificationBuilder(msgText, msg);
    }

    @Override
    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        return pushSDK.createNotification(msg, contentIntent);
    }

    @Override
    public PendingIntent createPendingIntent(Bundle msg) {
        return pushSDK.createPendingIntent(msg);
    }

    @Override
    public Intent createIntent(Bundle msg) {
        return pushSDK.createIntent(msg);
    }
}
