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
 * Implement this interface to handle the result of Swrve::getUserResourceDiffs().
 *
 * Note: the methods in this object will be invoked from a different thread
 * than the one used to call Swrve::getUserResourceDiffs().
 */
public interface ISwrveUserResourcesDiffListener {

    /**
     * This method is invoked asynchronously to return the request response of
     * the Swrve::getUserResourceDiffs().
     *
     * Note: this method is invoked from a different thread than the thread used
     * to call Swrve::getUserResourceDiffs().
     *
     * @param oldResourcesValues the old values of AB Tested resources represented as a map in
     *                           the form uid->(attribute_name->attribute_value).
     * @param newResourcesValues the new values of AB Tested resources represented as a map in
     *                           the form uid->(attribute_name->attribute_value).
     * @param resourcesAsJSON    the resources as JSON.
     */
    void onUserResourcesDiffSuccess(final Map<String, Map<String, String>> oldResourcesValues,
                                    final Map<String, Map<String, String>> newResourcesValues,
                                    final String resourcesAsJSON);

    /**
     * Called back on error.
     */
    public void onUserResourcesDiffError(Exception exception);
}
