package com.swrve.sdk.messaging;

/**
 * Button actions supported by in-app message buttons.
 */
public enum SwrveActionType {

    Dismiss,            // Cancel the message display
    Custom,             // Handle the custom action string associated with the button
    Install,            // Go to the url specified in the action string
    CopyToClipboard,    // Copy the contents of the action string associated to clipboard
    RequestCapabilty,   // Request capability
    PageLink,           // Link to another IAM page
    OpenNotificationSettings, // Open notification settings
    OpenAppSettings,    // Open app settings
    StartGeo;           // Start SwrveGeoSDK
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
        } else if (type.equalsIgnoreCase("copy_to_clipboard")) {
            return CopyToClipboard;
        } else if (type.equalsIgnoreCase("request_capability")) {
            return RequestCapabilty;
        } else if (type.equalsIgnoreCase("page_link")) {
            return PageLink;
        } else if (type.equalsIgnoreCase("open_notification_settings")) {
            return OpenNotificationSettings;
        } else if (type.equalsIgnoreCase("open_app_settings")) {
            return OpenAppSettings;
        } else if (type.equalsIgnoreCase("start_geo")) {
            return StartGeo;
        }
        return Custom;
    }
}
