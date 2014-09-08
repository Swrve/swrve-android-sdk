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
package com.swrve.sdk.config;

import com.swrve.sdk.SwrveHelper;

/**
 * Configuration for the Swrve Google SDK.
 */
public class SwrveConfig extends SwrveConfigBase {

    /**
     * Android Google Cloud Messaging sender id.
     */
    private String senderId;

    /**
     * Returns an instance of SwrveConfig with the sender id.
     *
     * @param senderId
     * @return
     */
    public static SwrveConfig withPush(String senderId) {
        return new SwrveConfig().setSenderId(senderId);
    }

    /**
     * @return the sender id.
     */
    public String getSenderId() {
        return this.senderId;
    }

    /**
     * @param senderId the Google Cloud Messaging sender id to set
     */
    public SwrveConfig setSenderId(String senderId) {
        this.senderId = senderId;
        return this;
    }

    /**
     * @return if push is enabled
     */
    public boolean isPushEnabled() {
        return !SwrveHelper.isNullOrEmpty(this.senderId);
    }
}
