package com.swrve.sdk.messaging.view;

/**
 * Exception raised when constructing an in-app message and there is no memory or other exception happened.
 */
public class SwrveMessageViewBuildException extends Exception {

    private static final long serialVersionUID = 1L;

    public SwrveMessageViewBuildException(String detailMessage) {
        super(detailMessage);
    }

}
