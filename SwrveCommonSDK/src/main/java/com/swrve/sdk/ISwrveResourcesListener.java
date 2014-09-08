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
 * Implement this interface to be notified of changes to the resources of your app.
 */
public interface ISwrveResourcesListener {
    /**
     * This method is invoked when user resources in the SwrveResourceManager have been initially
     * loaded and each time user resources are updated.
     *
     * Note: this method will be invoked from a different thread than the main UI thread.
     */
    void onResourcesUpdated();
}
