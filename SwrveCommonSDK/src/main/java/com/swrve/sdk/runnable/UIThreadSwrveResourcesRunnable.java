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
package com.swrve.sdk.runnable;

import com.swrve.sdk.ISwrveUserResourcesListener;

import java.util.Map;

/**
 * This helper class is used to run UI logic to handle
 * the result of Swrve::getUserResources()
 *
 * Note: the callback method onResourcesSuccess will be called from the
 * same UI threat than the caller Activity.
 */
public abstract class UIThreadSwrveResourcesRunnable extends UIThreadSwrveRunnable implements ISwrveUserResourcesListener {
    private Map<String, Map<String, String>> resources;
    private String resourcesAsJSON;

    @Override
    public void run() {
        if (exception != null)
            onUserResourcesError(exception);
        else
            onUserResourcesSuccess(resources, resourcesAsJSON);
    }

    public void setData(Map<String, Map<String, String>> resources, String resourcesAsJSON) {
        this.resources = resources;
        this.resourcesAsJSON = resourcesAsJSON;
    }
}
