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
package com.swrve.sdk;

/**
 * Use this class to get static access to the singleton instance the Swrve SDK.
 */
public class SwrveInstance {
    private static ISwrve instance;

    /**
     * Get a singleton Swrve SDK.
     * @return singleton SDK instance.
     */
    public static synchronized ISwrve getInstance() {
        if (instance == null) {
            instance = SwrveFactory.createInstance();
        }
        return instance;
    }
}
