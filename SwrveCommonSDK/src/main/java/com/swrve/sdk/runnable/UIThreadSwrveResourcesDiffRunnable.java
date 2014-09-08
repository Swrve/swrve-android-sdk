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

import com.swrve.sdk.ISwrveUserResourcesDiffListener;

import java.util.Map;

/**
 * This helper class is used to run UI logic to handle
 * the result of Swrve::getUserResourceDiffs()
 *
 * Note: the callback method onResourceDiffsSuccess will be called from the
 * same UI threat than the caller Activity.
 */
public abstract class UIThreadSwrveResourcesDiffRunnable extends UIThreadSwrveRunnable implements ISwrveUserResourcesDiffListener {
    private Map<String, Map<String, String>> oldResourcesValues;
    private Map<String, Map<String, String>> newResourcesValues;
    private String resourcesAsJSON;

    @Override
    public void run() {
        onUserResourcesDiffSuccess(oldResourcesValues, newResourcesValues, resourcesAsJSON);
    }

    public void setData(Map<String, Map<String, String>> oldResourcesValues, Map<String, Map<String, String>> newResourcesValues, String resourcesAsJSON) {
        this.oldResourcesValues = oldResourcesValues;
        this.newResourcesValues = newResourcesValues;
        this.resourcesAsJSON = resourcesAsJSON;
    }
}
