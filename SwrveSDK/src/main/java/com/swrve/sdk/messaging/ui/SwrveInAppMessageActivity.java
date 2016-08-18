package com.swrve.sdk.messaging.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.view.Window;
import android.view.WindowManager;

import com.swrve.sdk.R;
import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.view.SwrveMessageView;
import com.swrve.sdk.messaging.view.SwrveMessageViewBuildException;

public class SwrveInAppMessageActivity extends Activity {

    protected static final String LOG_TAG = "SwrveMessagingSDK";
    public static final String MESSAGE_ID_KEY = "message_id";

    private SwrveBase sdk;
    private SwrveMessage message;
    private boolean hideToolbar = false;
    private int minSampleSize;
    private int defaultBackgroundColor;

    private SwrveMessageFormat format;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sdk = (SwrveBase)SwrveSDK.getInstance();
        if (sdk == null) {
            finish();
            return;
        }
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                int messageId = extras.getInt(MESSAGE_ID_KEY);
                message = sdk.getMessageForId(messageId);

                SwrveConfigBase config = sdk.getConfig();
                this.hideToolbar = config.isHideToolbar();
                this.minSampleSize = config.getMinSampleSize();
                this.defaultBackgroundColor = config.getDefaultBackgroundColor();
            }
        }

        if (message == null) {
            finish();
            return;
        }
        // Choose the current orientation. If it is not possible,
        // pick the first one and set the requested orientation.
        SwrveOrientation deviceOrientation = getDeviceOrientation();
        format = message.getFormat(deviceOrientation);
        if (format == null) {
            format = message.getFormats().get(0);
        }

        if (message.getFormats().size() == 1) {
            if (format.getOrientation() == SwrveOrientation.Landscape) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        // Add the status bar if configured that way
        if (!hideToolbar) {
            setTheme(R.style.Theme_InAppMessageWithToolbar);
        }

        try {
            // Create view and add as root of the activity
            SwrveMessageView view = new SwrveMessageView(this, message,
                    format, minSampleSize, defaultBackgroundColor);
            setContentView(view);
            if(savedInstanceState == null) {
                notifyOfImpression(format);
            }
        } catch (SwrveMessageViewBuildException e) {
            SwrveLogger.e(LOG_TAG, "Error while creating the SwrveMessageView", e);
        }
    }

    private SwrveOrientation getDeviceOrientation() {
        return SwrveOrientation.parse(getResources().getConfiguration().orientation);
    }

    public void notifyOfImpression(SwrveMessageFormat format) {
        sdk.messageWasShownToUser(format);
    }

    public void notifyOfInstallButtonPress(SwrveButton button) {
        // IAM install button press
        sdk.buttonWasPressedByUser(button);
        message.getCampaign().messageDismissed();

        String appInstallLink = sdk.getAppStoreURLForApp(button.getAppId());
        // In case the install link was not set correctly log issue and return early
        // without calling the install button listener not starting the install intent
        if (SwrveHelper.isNullOrEmpty(appInstallLink)) {
            SwrveLogger.e(LOG_TAG, "Could not launch install action as there was no app install link found. Please supply a valid app install link.");
            return;
        }
        boolean freeEvent = true;
        if (sdk.getInstallButtonListener() != null) {
            freeEvent = sdk.getInstallButtonListener().onAction(appInstallLink);
        }
        if (freeEvent) {
            // Launch app store
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(appInstallLink)));
            } catch (android.content.ActivityNotFoundException anfe) {
                SwrveLogger.e(LOG_TAG, "Couldn't launch install action. No activity found for: " + appInstallLink, anfe);
            } catch (Exception exp) {
                SwrveLogger.e(LOG_TAG, "Couldn't launch install action for: " + appInstallLink, exp);
            }
        }
    }

    public void notifyOfCustomButtonPress(SwrveButton button) {
        // IAM custom button press
        sdk.buttonWasPressedByUser(button);
        message.getCampaign().messageDismissed();

        if (sdk.getCustomButtonListener() != null) {
            sdk.getCustomButtonListener().onAction(button.getAction());
        } else {
            String buttonAction = button.getAction();
            // Parse action as an Uri
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(buttonAction)));
            } catch (Exception e) {
                SwrveLogger.e(LOG_TAG, "Couldn't launch default custom action: " + buttonAction, e);
            }
        }
    }

    @VisibleForTesting
    public SwrveMessageFormat getFormat() {
        return format;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (message != null && message.getCampaign() != null) {
            message.getCampaign().messageDismissed();
        }
    }
}
