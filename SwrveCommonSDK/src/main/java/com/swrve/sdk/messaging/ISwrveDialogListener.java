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
package com.swrve.sdk.messaging;

import com.swrve.sdk.messaging.view.SwrveDialog;

/**
 * Implement this interface to manage the in-app message dialogs created
 * by the SDK.
 */
public interface ISwrveDialogListener {
    /**
     * This method is invoked when a dialog message should be shown in your app.
     *
     * @param dialog in-app message dialog ready to be shown.
     */
    void onDialog(SwrveDialog dialog);
}
