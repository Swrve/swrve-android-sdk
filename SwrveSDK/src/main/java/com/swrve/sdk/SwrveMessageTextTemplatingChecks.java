package com.swrve.sdk;

import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveImage;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;

import java.util.Map;

// Check the validity of all message formats with the given personalisation before displaying the message.
class SwrveMessageTextTemplatingChecks {

    public static boolean checkTemplating(SwrveMessage message, Map<String, String> properties) {
        try {
            for (final SwrveMessageFormat format : message.getFormats()) {
                for (final SwrveImage image : format.getImages()) {
                    String imageText = image.getText();
                    if (!SwrveHelper.isNullOrEmpty(imageText)) {
                        // Need to render dynamic text
                        String personalisedText = SwrveTextTemplating.apply(imageText, properties);
                        if (SwrveHelper.isNullOrEmpty(personalisedText)) {
                            SwrveLogger.i("Text template could not be resolved: " + imageText + " in given properties.");
                            return false;
                        } else if (SwrveTextTemplating.hasPatternMatch(personalisedText)) {
                            SwrveLogger.i("Not showing campaign with personalisation outside of Message Center / without personalisation info provided.");
                            return false;
                        }
                    }
                }

                for (final SwrveButton button : format.getButtons()) {
                    String buttonText = button.getText();
                    if (!SwrveHelper.isNullOrEmpty(buttonText)) {
                        // Need to render dynamic text
                        String personalisedText = SwrveTextTemplating.apply(buttonText, properties);
                        if (SwrveHelper.isNullOrEmpty(personalisedText)) {
                            SwrveLogger.i("Text template could not be resolved: " + buttonText + " in given properties.");
                            return false;
                        } else if (SwrveTextTemplating.hasPatternMatch(personalisedText)) {
                            SwrveLogger.i("Not showing campaign with personalisation outside of Message Center / without personalisation info provided.");
                            return false;
                        }
                    }

                    // Need to personalise action
                    String personalisedButtonAction = button.getAction();
                    if ((button.getActionType() == SwrveActionType.Custom || button.getActionType() == SwrveActionType.CopyToClipboard) && !SwrveHelper.isNullOrEmpty(personalisedButtonAction)) {
                        personalisedButtonAction = SwrveTextTemplating.apply(personalisedButtonAction, properties);
                        if (SwrveHelper.isNullOrEmpty(personalisedButtonAction)) {
                            SwrveLogger.i("Button action template could not be resolved: " + button.getAction() + " in given properties.");
                            return false;
                        } else if (SwrveTextTemplating.hasPatternMatch(personalisedButtonAction)) {
                            SwrveLogger.i("Not showing campaign with personalisation outside of Message Center / without personalisation info provided.");
                            return false;
                        }
                    }
                }
            }
        } catch(SwrveSDKTextTemplatingException exp) {
            SwrveLogger.e("Not showing campaign, error with personalisation", exp);
            return false;
        }
        return true;
    }
}
