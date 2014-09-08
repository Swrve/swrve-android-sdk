/* 
 * SWRVE CONFIDENTIAL
 * 
 * (c) Copyright 2010-2014 Swrve New Media, Inc. and its licensors.
 * All Rights Reserved.
 * 
 * NOTICE: All information contained herein is and remains the property of Swrve
 * New Media, Inc or its licensors.  The intellectual property and technical
 * concepts contained herein are proprietary to Swrve New Media, Inc. or its
 * licensors and are protected by trade secret and/or copyright law.
 * Dissemination of this information or reproduction of this material is
 * strictly forbidden unless prior written permission is obtained from Swrve.
 */
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
