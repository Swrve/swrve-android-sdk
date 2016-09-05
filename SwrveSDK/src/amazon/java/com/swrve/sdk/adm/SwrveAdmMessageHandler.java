package com.swrve.sdk.adm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;

import com.amazon.device.messaging.ADMConstants;
import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.amazon.device.messaging.ADMMessageReceiver;

/**
 * Created by johnokane on 02/09/16.
 */
public class SwrveAdmMessageHandler extends ADMMessageHandlerBase {
    /** Tag for logs. */
    private final static String TAG = "SwrveAdm";

    //SwrveMessageAlertReceiver listens for messages from ADM
    public static class SwrveMessageAlertReceiver extends ADMMessageReceiver {
        public SwrveMessageAlertReceiver() {
            super(SwrveAdmMessageHandler.class);
        }
    }

    public SwrveAdmMessageHandler() {
        super(SwrveAdmMessageHandler.class.getName());
    }

    public SwrveAdmMessageHandler(final String className)
    {
        super(className);
    }

    @Override
    protected void onMessage(final Intent intent) {
        Log.i(TAG, "SwrveAdmMessageHandler:onMessage");

        /* String to access message field from data JSON. */
        final String msgKey = SwrveAdmConstants.JSON_DATA_MSG_KEY;

        /* String to access timeStamp field from data JSON. */
        final String timeKey = SwrveAdmConstants.JSON_DATA_TIME_KEY;

        /* Intent action that will be triggered in onMessage() callback. */
        final String intentAction = SwrveAdmConstants.INTENT_MSG_ACTION;

        /* Extras that were included in the intent. */
        final Bundle extras = intent.getExtras();

        verifyMD5Checksum(extras);

        /* Extract message from the extras in the intent. */
        final String msg = extras.getString(msgKey);
        final String time = extras.getString(timeKey);

        if (msg == null || time == null) {
            Log.w(TAG, "SwrveAdmMessageHandler:onMessage Unable to extract message data." +
                    "Make sure that msgKey and timeKey values match data elements of your JSON message");
        }

        /* Create a notification with message data. */
        /* This is required to test cases where the app or device may be off. */
        //Code in sample.
        //postNotification(msgKey, timeKey, intentAction, msg, time);

        /* Intent category that will be triggered in onMessage() callback. */
        final String msgCategory = SwrveAdmConstants.INTENT_MSG_CATEGORY;

        /* Broadcast an intent to update the app UI with the message. */
        /* The broadcast receiver will only catch this intent if the app is within the onResume state of its lifecycle. */
        /* User will see a notification otherwise. */
        final Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(intentAction);
        broadcastIntent.addCategory(msgCategory);
        broadcastIntent.putExtra(msgKey, msg);
        broadcastIntent.putExtra(timeKey, time);
        this.sendBroadcast(broadcastIntent);
    }

    private void verifyMD5Checksum(final Bundle extras) {
        //TODO
    }

    @Override
    protected void onRegistrationError(final String string) {
        Log.e(TAG, "SwrveAdmMessageHandler:onRegistrationError " + string);
    }

    @Override
    protected void onRegistered(final String registrationId) {
        Log.i(TAG, "SwrveAdmMessageHandler:onRegistered");
        Log.i(TAG, registrationId);
    }

    @Override
    protected void onUnregistered(final String registrationId) {
        Log.i(TAG, "SwrveAdmMessageHandler:onUnregistered");
    }
}

