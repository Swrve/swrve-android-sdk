package com.swrve.sdk.messaging.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;

import com.swrve.sdk.QaUser;
import com.swrve.sdk.R;
import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.view.SwrveMessageView;
import com.swrve.sdk.messaging.view.SwrveMessageViewBuildException;
import java.util.Map;

public class SwrveInAppMessageActivity extends Activity {

    public static final String MESSAGE_ID_KEY = "message_id";
    public static final String SWRVE_PERSONALISATION_KEY = "message_personalization";
    private static final String SWRVE_AD_MESSAGE = "ad_message_key";


    private SwrveBase sdk;
    private SwrveMessage message;
    private int minSampleSize;
    private SwrveInAppMessageConfig inAppConfig;
    private Map<String, String> inAppPersonalisation;

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

                if (message == null) {
                    // Check if loaded from SwrveDeeplinkManager
                    if (extras.getBoolean(SWRVE_AD_MESSAGE)) {
                        message = sdk.getAdMesage();
                    }
                }

                this.inAppPersonalisation = (Map<String, String>) extras.getSerializable(SWRVE_PERSONALISATION_KEY);

                SwrveConfigBase config = sdk.getConfig();
                this.minSampleSize = config.getMinSampleSize();
                this.inAppConfig = config.getInAppMessageConfig();
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
            try {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O && SwrveHelper.getTargetSdkVersion(this) >= 27) {
                    // Cannot call setRequestedOrientation with translucent attribute, otherwise "IllegalStateException: Only fullscreen activities can request orientation"
                    // https://github.com/Swrve/swrve-android-sdk/issues/271
                    // workaround is to not change orientation
                    SwrveLogger.w("Oreo bug with setRequestedOrientation so Message may appear in wrong orientation.");
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    if (format.getOrientation() == SwrveOrientation.Landscape) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
                    }
                } else {
                    if (format.getOrientation() == SwrveOrientation.Landscape) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                    }
                }
            } catch(RuntimeException ex) {
                SwrveLogger.e("Bugs with setRequestedOrientation can happen: https://issuetracker.google.com/issues/68454482", ex);
            }
        }

        // Add the status bar if configured that way
        if (!inAppConfig.isHideToolbar()) {
            setTheme(R.style.Theme_InAppMessageWithToolbar);
        }

        try {
            // Create view and add as root of the activity
            SwrveMessageView view = new SwrveMessageView(this, message, format, minSampleSize, inAppConfig, inAppPersonalisation);
            setContentView(view);
            if(savedInstanceState == null) {
                notifyOfImpression(format);
            }
        } catch (SwrveMessageViewBuildException e) {
            SwrveLogger.e("Error while creating the SwrveMessageView", e);
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
            SwrveLogger.e("Could not launch install action as there was no app install link found. Please supply a valid app install link.");
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
                SwrveLogger.e("Couldn't launch install action. No activity found for: %s", anfe, appInstallLink);
            } catch (Exception exp) {
                SwrveLogger.e("Couldn't launch install action for: %s", exp, appInstallLink);
            }
        }
        qaUserCampaignButtonClicked(button);
    }

    public void notifyOfCustomButtonPress(SwrveButton button, String resolvedButtonAction) {
        // IAM custom button press
        sdk.buttonWasPressedByUser(button);
        message.getCampaign().messageDismissed();

        if (sdk.getCustomButtonListener() != null) {
            sdk.getCustomButtonListener().onAction(resolvedButtonAction);
        } else {
            // Parse action as an Uri
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(resolvedButtonAction)));
            } catch (Exception e) {
                SwrveLogger.e("Couldn't launch default custom action: %s", e, resolvedButtonAction);
            }
        }
        qaUserCampaignButtonClicked(button);
    }

    public void notifyOfClipboardButtonPress(SwrveButton button, String stringToCopy) {
        // IAM copy-to-clipboard press
        sdk.buttonWasPressedByUser(button);
        message.getCampaign().messageDismissed();

        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("simple text", stringToCopy);
            clipboard.setPrimaryClip(clip);

            if (sdk.getClipboardButtonListener() != null){
                sdk.getClipboardButtonListener().onAction(stringToCopy);
            }

        } catch (Exception e) {

            SwrveLogger.e("Couldn't copy text to clipboard: %s", e, stringToCopy);
        }


    }

    public void notifyOfDismissButtonPress(SwrveButton button) {
        if (sdk.getDismissButtonListener() != null) {
            sdk.getDismissButtonListener().onAction(message.getCampaign().getSubject(), button.getName());
        }
        qaUserCampaignButtonClicked(button);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (sdk.getDismissButtonListener() != null) {
            sdk.getDismissButtonListener().onAction(message.getCampaign().getSubject(), null);
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

    private void qaUserCampaignButtonClicked(SwrveButton button) {
        if (!QaUser.isLoggingEnabled()) {
            return;
        }
        int campaignId = message.getCampaign().getId();
        int variantId = message.getCampaign().getVariantIdAtIndex(0);
        String buttonName = button.getName();
        String actionType = "";
        switch (button.getActionType()) {
            case Install:
                actionType = "install";
                break;
            case Dismiss:
                actionType = "dismiss";
                break;
            case Custom:
                actionType = "deeplink";
                break;
            case CopyToClipboard:
                actionType = "clipboard";
                break;
        }
        String actionValue = SwrveHelper.isNullOrEmpty(button.getAction()) ? actionType : button.getAction();
        QaUser.campaignButtonClicked(campaignId, variantId, buttonName, actionType, actionValue);
    }
}
