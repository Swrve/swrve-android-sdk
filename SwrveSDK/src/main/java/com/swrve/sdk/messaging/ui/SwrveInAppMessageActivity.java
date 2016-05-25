package com.swrve.sdk.messaging.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.Window;
import android.view.WindowManager;

import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.view.SwrveMessageView;
import com.swrve.sdk.messaging.view.SwrveMessageViewBuildException;

public class SwrveInAppMessageActivity extends Activity {

    private SwrveMessage message;
    private boolean hideToolbar = false;
    private int minSampleSize;
    private Messenger messenger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                this.message = (SwrveMessage) extras.getSerializable("inAppMessage");
                this.hideToolbar = extras.getBoolean("hideToolbar", true);
                this.minSampleSize = extras.getInt("minSampleSize", 1);
                this.messenger = (Messenger)extras.get("messenger");
            }
        }

        SwrveOrientation deviceOrientation = getDeviceOrientation();
        SwrveMessageFormat format = message.getFormat(deviceOrientation);
        if (format == null) {
            format = message.getFormats().get(0);
            if (format.getOrientation() != deviceOrientation) {
                if (format.getOrientation() == SwrveOrientation.Landscape) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        }

        if (message != null) {
            if (hideToolbar) {
                // Remove the status bar from the activity
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                requestWindowFeature(Window.FEATURE_NO_TITLE);
            }

            try {
                // Create view and add as root of the activity
                SwrveMessageView view = new SwrveMessageView(this, messenger, message, format, minSampleSize);
                setContentView(view);
                if(savedInstanceState == null) {
                    view.notifyOfImpression(message, format);
                }
            } catch (SwrveMessageViewBuildException e) {
                e.printStackTrace();
            }
        } else {
            finish();
        }
    }

    private SwrveOrientation getDeviceOrientation() {
        return SwrveOrientation.parse(getResources().getConfiguration().orientation);
    }
}
