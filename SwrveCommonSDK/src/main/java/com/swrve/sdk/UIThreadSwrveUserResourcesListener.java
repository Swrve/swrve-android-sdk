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

import android.app.Activity;

import com.swrve.sdk.runnable.UIThreadSwrveResourcesRunnable;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * This helper class is used to run UI logic to handle
 * the result of Swrve::getUserResources()
 *
 * Note: the Runnable uiWork will be called from the context of
 * the Activity.
 */
public class UIThreadSwrveUserResourcesListener implements ISwrveUserResourcesListener {

    private final WeakReference<Activity> context;
    private final UIThreadSwrveResourcesRunnable uiWork;

    public UIThreadSwrveUserResourcesListener(Activity context, UIThreadSwrveResourcesRunnable uiWork) {
        this.context = new WeakReference<Activity>(context);
        this.uiWork = uiWork;
    }

    public void onUserResourcesSuccess(Map<String, Map<String, String>> resources, String resourcesAsJSON) {
        Activity ctx = context.get();
        if (ctx != null && !ctx.isFinishing()) {
            uiWork.setData(resources, resourcesAsJSON);
            ctx.runOnUiThread(uiWork);
        }
    }

    @Override
    public void onUserResourcesError(Exception exception) {
        Activity ctx = context.get();
        if (ctx != null && !ctx.isFinishing()) {
            uiWork.setException(exception);
            ctx.runOnUiThread(uiWork);
        }
    }
}
