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
package com.swrve.sdk.gcm;

import android.os.Bundle;

/**
 * Implement this interface to be notified of any Swrve push notification
 * to your app.
 */
public interface ISwrvePushNotificationListener {

    /**
     * This method will be called when a push notification is received by your app,
     * after the user has reacted to it.
     * @param bundle push notification information including custom payloads.
     */
    void onPushNotification(Bundle bundle);
}
