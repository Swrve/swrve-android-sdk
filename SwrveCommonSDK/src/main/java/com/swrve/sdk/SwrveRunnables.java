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

import android.util.Log;

/**
 * User internally to assure exceptions won't bubble up when executing a runnable
 * in an executor.
 */
final class SwrveRunnables {

    protected static final String LOG_TAG = "SwrveSDK";

    public static Runnable withoutExceptions(final Runnable r) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Exception exp) {
                    Log.e(LOG_TAG, "Error executing runnable: ", exp);
                }
            }
        };
    }
}
