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

import java.util.Map;

/**
 * Implement this interface to handle the result of Swrve::getUserResources().
 *
 * Note: the methods in this object will be invoked from a different thread
 * than the one used to call Swrve::getUserResources().
 */
public interface ISwrveUserResourcesListener {

    /**
     * This method is invoked asynchronously to return the request response of
     * the Swrve::getUserResources().
     *
     * Note: this method is invoked from a different thread than the thread used
     * to call Swrve::getUserResources().
     *
     * @param resources       the resources represented as a map in the form
     *                        uid->(attribute_name->attribute_value).
     * @param resourcesAsJSON the resources as JSON.
     */
    void onUserResourcesSuccess(final Map<String, Map<String, String>> resources,
                                final String resourcesAsJSON);

    /**
     * Called back on error.
     */
    public void onUserResourcesError(Exception exception);
}
