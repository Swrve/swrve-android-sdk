package com.swrve.sdk.messaging;

/**
 * Button actions supported by in-app message buttons.
 */
public enum SwrveActionType {
    // Cancel the message display
    Dismiss,
    // Handle the custom action string associated with the button
    Custom,
    // Go to the url specified in the action string
    Install;

    /**
     * Convert from string to SwrveActionType.
     *
     * @param type string to parse as SwrveActionType
     * @return SwrveActionType
     * Parsed SwrveActionType value. If the type is unknown
     * it will default to SwrveActionType.Custom.
     */
    public static SwrveActionType parse(String type) {
        if (type.equalsIgnoreCase("install")) {
            return Install;
        } else if (type.equalsIgnoreCase("dismiss")) {
            return Dismiss;
        }
        return Custom;
    }
}
