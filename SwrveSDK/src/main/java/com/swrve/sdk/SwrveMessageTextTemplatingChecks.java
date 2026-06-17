package com.swrve.sdk;

import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveImage;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveMessagePage;

import java.util.ArrayList;
import java.util.Map;

// Check the validity of all message formats with the given personalization before displaying the message.
class SwrveMessageTextTemplatingChecks {

    public static boolean checkTextTemplating(SwrveMessage message, Map<String, String> properties) {
        try {
            checkTextTemplatingOrThrow(message, properties);
        } catch (SwrveSDKTextTemplatingException exp) {
            SwrveLogger.e("Not showing campaign, error with personalization", exp);
            return false;
        }
        return true;
    }

    // Throws SwrveSDKTextTemplatingException with a descriptive reason for all failure cases so callers can surface it in QA logs.
    static void checkTextTemplatingOrThrow(SwrveMessage message, Map<String, String> properties) throws SwrveSDKTextTemplatingException {
        boolean freemarkerEnabled = message.getCampaign().isFreemarkerEnabled();
        boolean useLocalTimezone = message.getCampaign().useLocalTimezone();

        if (message.getMessageCenterDetails() != null) {
            ArrayList<String> messageCenterDetails = new ArrayList<>();
            messageCenterDetails.add(message.getMessageCenterDetails().getSubject());
            messageCenterDetails.add(message.getMessageCenterDetails().getDescription());
            messageCenterDetails.add(message.getMessageCenterDetails().getImageAccessibilityText());
            messageCenterDetails.add(message.getMessageCenterDetails().getImageURL());
            for (String textToPersonalize : messageCenterDetails) {
                if (!SwrveHelper.isNullOrEmpty(textToPersonalize)) {
                    String personalizedText = SwrveTextTemplating.apply(textToPersonalize, properties, freemarkerEnabled, useLocalTimezone);
                    if (freemarkerEnabled ? personalizedText == null : SwrveHelper.isNullOrEmpty(personalizedText)) {
                        throw new SwrveSDKTextTemplatingException("Message center detail text template could not be resolved: " + textToPersonalize);
                    } else if (!freemarkerEnabled && SwrveTextTemplating.hasPatternMatch(personalizedText)) {
                        throw new SwrveSDKTextTemplatingException("Message center detail personalization info not provided for: " + textToPersonalize);
                    }
                }
            }
        }

        for (final SwrveMessageFormat format : message.getFormats()) {

            for (Map.Entry<Long, SwrveMessagePage> entry : format.getPages().entrySet()) {
                SwrveMessagePage page = entry.getValue();

                // check images
                for (final SwrveImage image : page.getImages()) {
                    String imageText = image.getText();
                    if (!SwrveHelper.isNullOrEmpty(imageText)) {
                        String personalizedText = SwrveTextTemplating.apply(imageText, properties, freemarkerEnabled, useLocalTimezone);
                        if (freemarkerEnabled ? personalizedText == null : SwrveHelper.isNullOrEmpty(personalizedText)) {
                            throw new SwrveSDKTextTemplatingException("Image text template could not be resolved: " + imageText);
                        } else if (!freemarkerEnabled && SwrveTextTemplating.hasPatternMatch(personalizedText)) {
                            throw new SwrveSDKTextTemplatingException("Image personalization info not provided for: " + imageText);
                        }
                    }
                }

                // check buttons
                for (final SwrveButton button : page.getButtons()) {
                    String buttonText = button.getText();
                    if (!SwrveHelper.isNullOrEmpty(buttonText)) {
                        String personalizedText = SwrveTextTemplating.apply(buttonText, properties, freemarkerEnabled, useLocalTimezone);
                        if (freemarkerEnabled ? personalizedText == null : SwrveHelper.isNullOrEmpty(personalizedText)) {
                            throw new SwrveSDKTextTemplatingException("Button text template could not be resolved: " + buttonText);
                        } else if (!freemarkerEnabled && SwrveTextTemplating.hasPatternMatch(personalizedText)) {
                            throw new SwrveSDKTextTemplatingException("Button personalization info not provided for: " + buttonText);
                        }
                    }

                    // Need to personalize action
                    String personalizedButtonAction = button.getAction();
                    if ((button.getActionType() == SwrveActionType.Custom || button.getActionType() == SwrveActionType.CopyToClipboard) && !SwrveHelper.isNullOrEmpty(personalizedButtonAction)) {
                        personalizedButtonAction = SwrveTextTemplating.apply(personalizedButtonAction, properties, freemarkerEnabled, useLocalTimezone);
                        if (freemarkerEnabled ? personalizedButtonAction == null : SwrveHelper.isNullOrEmpty(personalizedButtonAction)) {
                            throw new SwrveSDKTextTemplatingException("Button action template could not be resolved: " + button.getAction());
                        } else if (!freemarkerEnabled && SwrveTextTemplating.hasPatternMatch(personalizedButtonAction)) {
                            throw new SwrveSDKTextTemplatingException("Button action personalization info not provided for: " + button.getAction());
                        }
                    }
                }
            }
        }

        message.validateVisibleIfExpressions(properties);
    }
}
