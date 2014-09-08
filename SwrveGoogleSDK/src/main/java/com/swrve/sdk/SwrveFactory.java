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
 * Use this class to obtain an instance of the Swrve Google SDK.
 */
public class SwrveFactory extends SwrveFactoryBase {

    /**
     * Create a new instance of the Swrve Google SDK.
     * @return new instance of the SDK.
     */
    public static ISwrve createInstance() {
        if (sdkAvailable()) {
            return new Swrve();
        }

        return new SwrveEmpty();
    }
}
