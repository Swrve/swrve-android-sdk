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
 * Implement this interface to handle callbacks of install buttons
 * inside your in-app messages.
 *
 * Note: the methods in this interface will be invoked from the UI thread.
 */
public interface ISwrveInstallButtonListener {
    /**
     * This method is invoked when an install button has been pressed on an in-app message.
     *
     * @param appStoreLink app store install link.
     * @return boolean
     * returning false stops the normal flow of event processing
     * to enable custom logic. Return true otherwise.
     */
    boolean onAction(String appStoreLink);
}
