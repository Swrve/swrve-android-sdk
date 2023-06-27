package com.swrve.sdk.messaging;

/**
 * This class provides meta data associated with in-app message buttons.
 * See SwrveMessageListener.
 */
public class SwrveMessageButtonDetails {
    final private String buttonName;
    final private String buttonText;
    final private SwrveActionType actionType;
    final private String actionString;

    public SwrveMessageButtonDetails(String buttonName, String buttonText, SwrveActionType actionType, String actionString) {
        this.buttonName = buttonName;
        this.buttonText = buttonText;
        this.actionType = actionType;
        this.actionString = actionString;
    }

    /**
     * @return the button name
     */
    public String getButtonName() {
        return buttonName;
    }

    /**
     * @return the button title text
     */
    public String getButtonText() {
        return buttonText;
    }

    /**
     * @return the action type for this button
     */
    public SwrveActionType getActionType() {
        return actionType;
    }

    /**
     * @return the action string associated with this button
     */
    public String getActionString() {
        return actionString;
    }
}

