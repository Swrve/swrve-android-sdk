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

package com.swrve.sdk.messaging;

/**
 * Implement this interface to handle custom deep-links in your app as result
 * of an in-app custom button.
 *
 * Note: the methods in this interface will be invoked from the UI thread.
 */
public interface ISwrveCustomButtonListener {
    /**
     * This method is invoked when a custom button has been pressed on an in-app message.
     *
     * @param customAction custom action of button that was pressed.
     */
    void onAction(String customAction);
}
