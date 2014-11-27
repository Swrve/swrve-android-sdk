package com.swrve.sdk.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.qa.SwrveQAUser;

import java.util.Date;
import java.util.Iterator;

/**
 * Used internally to process push notifications inside for your app.
 */
public class SwrveGcmIntentService extends IntentService {
    private static final String TAG = "SwrveGcmIntentService";
    private static final String SWRVE_PUSH_ICON_METADATA = "SWRVE_PUSH_ICON";
    private static final String SWRVE_PUSH_ACTIVITY_METADATA = "SWRVE_PUSH_ACTIVITY";
    private static final String SWRVE_PUSH_TITLE_METADATA = "SWRVE_PUSH_TITLE";

    private boolean failedInitialisation = false;

    private Class<?> activityClass;
    private int iconDrawableId;
    private String notificationTitle;

    public SwrveGcmIntentService() {
        super("SwrveGcmIntentService");
    }

    public SwrveGcmIntentService(Class<?> activityClass, int iconDrawableId, String notificationTitle) {
        super("SwrveGcmIntentService");
        init(activityClass, iconDrawableId, notificationTitle);
    }

    private void init(Class<?> activityClass, int iconDrawableId, String notificationTitle) {
        try {
            this.activityClass = activityClass;
            this.iconDrawableId = iconDrawableId;
            this.notificationTitle = notificationTitle;
        } catch (Exception exp) {
            // Stop the service as there was an initialization error
            failedInitialisation = true;
            exp.printStackTrace();
            this.stopSelf();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Try to read config from metadata
        if (activityClass == null || SwrveHelper.isNullOrEmpty(notificationTitle)) {
            readConfigFromMetadata();
        }
    }

    private void readConfigFromMetadata() {
        try {
            // Read config from the apps metadata
            ApplicationInfo app = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = app.metaData;
            if (metaData == null) {
                throw new RuntimeException("No SWRVE metadata specified in AndroidManifest.xml");
            }

            int pushIconId = metaData.getInt(SWRVE_PUSH_ICON_METADATA, -1);
            if (pushIconId < 0) {
                // No icon specified in the metadata
                throw new RuntimeException("No " + SWRVE_PUSH_ICON_METADATA + " specified in AndroidManifest.xml");
            }

            Class<?> pushActivityClass = null;
            String pushActivity = metaData.getString(SWRVE_PUSH_ACTIVITY_METADATA);
            if (SwrveHelper.isNullOrEmpty(pushActivity)) {
                // No activity specified in the metadata
                throw new RuntimeException("No " + SWRVE_PUSH_ACTIVITY_METADATA + " specified in AndroidManifest.xml");
            } else {
                // Check that the Activity exists and can be found
                pushActivityClass = getClassForActivityClassName(pushActivity);
                if (pushActivityClass == null) {
                    throw new RuntimeException("The Activity with name " + pushActivity + " could not be found");
                }
            }

            String pushTitle = metaData.getString(SWRVE_PUSH_TITLE_METADATA);
            if (SwrveHelper.isNullOrEmpty(pushTitle)) {
                // No activity specified in the metadata
                throw new RuntimeException("No " + SWRVE_PUSH_TITLE_METADATA + " specified in AndroidManifest.xml");
            }

            init(pushActivityClass, pushIconId, pushTitle);
        } catch (Exception exp) {
            // Stop the service as there was an initialization error
            failedInitialisation = true;
            exp.printStackTrace();
            this.stopSelf();
        }
    }

    private Class<?> getClassForActivityClassName(String className) {
        if (!SwrveHelper.isNullOrEmpty(className)) {
            if (className.startsWith(".")) {
                // Append application package as it starts with .
                className = getApplication().getPackageName() + className;
                int h = 5;
            }
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!failedInitialisation) {
            Bundle extras = intent.getExtras();
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
            // The getMessageType() intent parameter must be the intent you received
            // in your BroadcastReceiver.
            String messageType = gcm.getMessageType(intent);

            if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
                /*
	             * Filter messages based on message type. Since it is likely that GCM
	             * will be extended in the future with new message types, just ignore
	             * any message types you're not interested in, or that you don't
	             * recognize.
	             */
                if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                    Log.e(TAG, "Send error: " + extras.toString());
                } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                    Log.e(TAG, "Deleted messages on server: " + extras.toString());
                    // If it's a regular GCM message, do some work.
                } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                    // Process notification.
                    processRemoteNotification(extras);
                    Log.i(TAG, "Received notification: " + extras.toString());
                }
            }
            // Release the wake lock provided by the WakefulBroadcastReceiver.
            SwrveGcmBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    private void processRemoteNotification(Bundle msg) {
        // Notify binded clients
        Iterator<SwrveQAUser> iter = SwrveQAUser.getBindedListeners().iterator();
        while (iter.hasNext()) {
            SwrveQAUser sdkListener = iter.next();
            sdkListener.pushNotification(msg);
        }

        // Process notification
        processNotification(msg);
    }

    /**
     * Override this function to process notifications in a different way.
     *
     * @param msg
     */
    public void processNotification(final Bundle msg) {
        if (mustShowNotification()) {
            try {
                // Put the message into a notification and post it.
                final NotificationManager mNotificationManager = (NotificationManager)
                        this.getSystemService(Context.NOTIFICATION_SERVICE);
                final PendingIntent contentIntent = createPendingIntent(msg);
                if (contentIntent != null) {
                    final Notification notification = createNotification(msg, contentIntent);
                    if (notification != null) {
                        showNotification(mNotificationManager, notification);
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error processing push notification", ex);
            }
        }
    }

    /**
     * Override this function to decide when to show a notification.
     *
     * @return true when you want to display notifications
     */
    public boolean mustShowNotification() {
        return true;
    }

    /**
     * Override this function to change the way a notification is shown.
     *
     * @param notificationManager
     * @param notification
     * @return the notification id so that it can be dismissed by other UI elements
     */
    public int showNotification(NotificationManager notificationManager, Notification notification) {
        int notificationId = (int)(new Date().getTime() % Integer.MAX_VALUE);
        notificationManager.notify(notificationId, notification);
        return notificationId;
    }

    /**
     * Override this function to change the attributes of a notification.
     *
     * @param msgText
     * @param msg
     * @return
     */
    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        String msgSound = msg.getString("sound");

        // Build notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(iconDrawableId)
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msgText))
                .setContentText(msgText)
                .setAutoCancel(true);

        if (!SwrveHelper.isNullOrEmpty(msgSound)) {
            Uri soundUri;
            if (msgSound.equalsIgnoreCase("default")) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            } else {
                String packageName = getApplicationContext().getPackageName();
                soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                        + "://" + packageName + "/raw/" + msgSound);
            }
            mBuilder.setSound(soundUri);
        }
        return mBuilder;
    }

    /**
     * Override this function to change the way the notifications are created.
     *
     * @param msg
     * @param contentIntent
     * @return
     */
    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        String msgText = msg.getString("text");
        if (!SwrveHelper.isNullOrEmpty(msgText)) {
            // Build notification
            NotificationCompat.Builder mBuilder = createNotificationBuilder(msgText, msg);
            mBuilder.setContentIntent(contentIntent);
            return mBuilder.build();
        }

        return null;
    }

    /**
     * Override this function to change what the notification will do
     * once clicked by the user.
     *
     * Note: sending the Bundle in an extra parameter
     * "notification" is essential so that the Swrve SDK
     * can be notified that the app was opened from the
     * notification.
     *
     * @param msg
     * @return
     */
    public PendingIntent createPendingIntent(Bundle msg) {
        // Add notification to bundle
        Intent intent = createIntent(msg);
        if (intent != null) {
            int notificationId = (int)(new Date().getTime() % Integer.MAX_VALUE);
            return PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return null;
    }

    /**
     * Override this function to change what the notification will do
     * once clicked by the user.
     *
     * Note: sending the Bundle in an extra parameter
     * "notification" is essential so that the Swrve SDK
     * can be notified that the app was opened from the
     * notification.
     *
     * @param msg
     * @return
     */
    public Intent createIntent(Bundle msg) {
        Intent intent = null;
        if (activityClass != null) {
            intent = new Intent(this, activityClass);
            intent.putExtra("notification", msg);
            intent.setAction("openActivity");
        }
        return intent;
    }
}
