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
package com.swrve.sdk.messaging.view;

import android.content.Context;
import android.util.Log;

import com.swrve.sdk.messaging.ISwrveCustomButtonListener;
import com.swrve.sdk.messaging.ISwrveInstallButtonListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveOrientation;

/**
 * View factory used to create visual representation of in-app messages.
 */
public class SwrveMessageViewFactory {
    protected static final String LOG_TAG = "SwrveMessagingSDK";

    private static SwrveMessageViewFactory instance;

    /**
     * Get instance of the Swrve Message View Factory.
     *
     * @return singleton instance
     */
    public static SwrveMessageViewFactory getInstance() {
        if (instance == null) {
            instance = new SwrveMessageViewFactory();
        }

        return instance;
    }

    /**
     * Build a message view from the message with the given orientation.
     *
     * @param context               Android context to build the view.
     * @param message               message to be rendered.
     * @param orientation           orientation of the format to be rendered. It can also be Both
     *                              to select any format available in the message.
     * @param installButtonListener install button listener to process clicks on the view
     * @param customButtonListener  custom button listener to process clicks on the view
     * @param firstTime             indicates if it is the first time the message is displayed.
     *                              It should be false when the message is re-displayed after a rotation.
     * @return SwrveMessageView
     * Message view with the given orientation. If no format is found with that
     * orientation then the result will be null.
     * @throws SwrveMessageViewBuildException
     */
    public SwrveMessageView buildLayout(Context context, SwrveMessage message, SwrveOrientation orientation, ISwrveInstallButtonListener installButtonListener, ISwrveCustomButtonListener customButtonListener, boolean firstTime, int minSampleSize) throws SwrveMessageViewBuildException {
        try {
            boolean hasToRotate = false;
            if (message != null && message.getFormats().size() > 0) {
                Log.i(LOG_TAG, "Creating layout for message " + message.getId() + " with orientation " + orientation.toString());
                // Try to get current orientation
                SwrveMessageFormat format = message.getFormat(orientation);
                if (format == null && !firstTime) {
                    // This view has to be rotated
                    format = message.getFormats().get(0);
                    hasToRotate = true;
                }

                if (format != null) {
                    return new SwrveMessageView(context, message, format, installButtonListener, customButtonListener, firstTime, hasToRotate, minSampleSize);
                }
            }
        } catch (SwrveMessageViewBuildException e) {
            throw e;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while building SwrveMessageView view", e);
        }

        throw new SwrveMessageViewBuildException("No format with the given orientation was found");
    }
}
