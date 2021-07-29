package com.swrve.sdk;

import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveImage;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;

import java.util.Map;

// Check the validity of all message formats with the given personalization before displaying the message.
class SwrveMessageTextTemplatingChecks {

    public static boolean checkTextTemplating(SwrveMessage message, Map<String, String> properties) {
        try {
            for (final SwrveMessageFormat format : message.getFormats()) {
                for (final SwrveImage image : format.getImages()) {
                    String imageText = image.getText();
                    if (!SwrveHelper.isNullOrEmpty(imageText)) {
                        // Need to render dynamic text
                        String personalizedText = SwrveTextTemplating.apply(imageText, properties);
                        if (SwrveHelper.isNullOrEmpty(personalizedText)) {
                            SwrveLogger.i("Text template could not be resolved: " + imageText + " in given properties.");
                            return false;
                        } else if (SwrveTextTemplating.hasPatternMatch(personalizedText)) {
                            SwrveLogger.i("Not showing campaign with personalization outside of Message Center / without personalization info provided.");
                            return false;
                        }
                    }
                }

                for (final SwrveButton button : format.getButtons()) {
                    String buttonText = button.getText();
                    if (!SwrveHelper.isNullOrEmpty(buttonText)) {
                        // Need to render dynamic text
                        String personalizedText = SwrveTextTemplating.apply(buttonText, properties);
                        if (SwrveHelper.isNullOrEmpty(personalizedText)) {
                            SwrveLogger.i("Text template could not be resolved: " + buttonText + " in given properties.");
                            return false;
                        } else if (SwrveTextTemplating.hasPatternMatch(personalizedText)) {
                            SwrveLogger.i("Not showing campaign with personalization outside of Message Center / without personalization info provided.");
                            return false;
                        }
                    }

                    // Need to personalize action
                    String personalizedButtonAction = button.getAction();
                    if ((button.getActionType() == SwrveActionType.Custom || button.getActionType() == SwrveActionType.CopyToClipboard) && !SwrveHelper.isNullOrEmpty(personalizedButtonAction)) {
                        personalizedButtonAction = SwrveTextTemplating.apply(personalizedButtonAction, properties);
                        if (SwrveHelper.isNullOrEmpty(personalizedButtonAction)) {
                            SwrveLogger.i("Button action template could not be resolved: " + button.getAction() + " in given properties.");
                            return false;
                        } else if (SwrveTextTemplating.hasPatternMatch(personalizedButtonAction)) {
                            SwrveLogger.i("Not showing campaign with personalization outside of Message Center / without personalization info provided.");
                            return false;
                        }
                    }
                }
            }
        } catch(SwrveSDKTextTemplatingException exp) {
            SwrveLogger.e("Not showing campaign, error with personalization", exp);
            return false;
        }
        return true;
    }

    public static boolean checkImageUrlTemplating(SwrveMessage message, Map<String, String> properties) {
        try {
            for (final SwrveMessageFormat format : message.getFormats()) {
                for (final SwrveImage image : format.getImages()) {
                    String imageUrl = image.getDynamicImageUrl();
                    // if we already have a fallback image to resolve so we don't need to check
                    if (SwrveHelper.isNotNullOrEmpty(imageUrl) && SwrveHelper.isNullOrEmpty(image.getFile())) {
                        String personalizedText = SwrveTextTemplating.apply(imageUrl, properties);
                        if (SwrveHelper.isNullOrEmpty(personalizedText)) {
                            SwrveLogger.i(" Dynamic image url text template could not be resolved: " + imageUrl + " in given properties.");
                            return false;
                        } else if (SwrveTextTemplating.hasPatternMatch(personalizedText)) {
                            SwrveLogger.i("Not showing personalized image / without personalization info provided.");
                            return false;
                        }
                    }
                }

                for (final SwrveButton button : format.getButtons()) {
                    String buttonImageUrl = button.getDynamicImageUrl();
                    // if we already have a fallback image to resolve so we don't need to check
                    if (SwrveHelper.isNotNullOrEmpty(buttonImageUrl) && SwrveHelper.isNullOrEmpty(button.getImage())) {
                        String personalizedText = SwrveTextTemplating.apply(buttonImageUrl, properties);
                        if (SwrveHelper.isNullOrEmpty(personalizedText)) {
                            SwrveLogger.i("Dynamic button image url text template could not be resolved: " + buttonImageUrl + " in given properties.");
                            return false;
                        } else if (SwrveTextTemplating.hasPatternMatch(personalizedText)) {
                            SwrveLogger.i("Not showing personalized image / without personalization info provided.");
                            return false;
                        }
                    }
                }
            }
        } catch(SwrveSDKTextTemplatingException exp) {
            SwrveLogger.e("Not showing campaign, error with personalization", exp);
            return false;
        }
        return true;
    }
}
