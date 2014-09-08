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
 * Implement this interface to handle the rendering of in-app messages
 * completely from your app. You will have to render and manage these
 * messages yourself.
 */
public interface ISwrveMessageListener {
    /**
     * This method is invoked when a message should be shown in your app.
     *
     * @param message   message to be shown.
     * @param firstTime indicates if this message was already showing and the app
     *                  rotated.
     */
    void onMessage(SwrveMessage message, boolean firstTime);
}
