package com.swrve.sdk.messaging.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.view.Window;
import android.view.WindowManager;

import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.view.SwrveMessageView;
import com.swrve.sdk.messaging.view.SwrveMessageViewBuildException;

public class SwrveInAppMessageActivity extends Activity {

    protected static final String LOG_TAG = "SwrveMessagingSDK";

    private SwrveMessage message;
    private boolean hideToolbar = false;
    private int minSampleSize;
    private Messenger messenger;
    private int defaultBackgroundColor;

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
                this.defaultBackgroundColor = extras.getInt("defaultBackgroundColor", 0);
            }
        }

        if (message != null) {
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

            if (hideToolbar) {
                // Remove the status bar from the activity
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                requestWindowFeature(Window.FEATURE_NO_TITLE);
            }

            try {
                // Create view and add as root of the activity
                SwrveMessageView view = new SwrveMessageView(this, message,
                        format, minSampleSize, defaultBackgroundColor);
                setContentView(view);
                if(savedInstanceState == null) {
                    notifyOfImpression(message, format);
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

    public void notifyOfImpression(SwrveMessage message, SwrveMessageFormat format) {
        Bundle bundle = new Bundle();
        bundle.putInt("formatIndex", message.getFormats().indexOf(format));
        sendMessage("impression", bundle);
    }

    public void notifyOfInstallButtonPress(SwrveButton button) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("button", button);
        sendMessage("installButtonPress", bundle);
    }

    public void notifyOfCustomButtonPress(SwrveButton button) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("button", button);
        sendMessage("customButtonPress", bundle);
    }

    private void sendMessage(String eventType, Bundle bundle) {
        try {
            Message msg = new Message();
            bundle.putString("eventType", eventType);
            msg.setData(bundle);
            messenger.send(msg);
        } catch(Exception exp) {
            SwrveLogger.e(LOG_TAG, "Swrve IAM error sending event signal", exp);
        }
    }
}
