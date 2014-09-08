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

import android.app.Activity;
import android.app.Dialog;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.swrve.sdk.messaging.SwrveMessage;

/**
 * Dialog used to display in-app messages.
 */
public class SwrveDialog extends Dialog {

    private SwrveMessageView innerView;
    private SwrveMessage message;
    private LayoutParams originalParams;

    private boolean dismissed = false;

    public SwrveDialog(Activity context, SwrveMessage message, SwrveMessageView innerView, int theme) {
        super(context, theme);
        this.message = message;
        this.innerView = innerView;
        this.originalParams = context.getWindow().getAttributes();
        setContentView(innerView);
        innerView.setContainerDialog(this);
    }

    public SwrveMessage getMessage() {
        return message;
    }

    public SwrveMessageView getInnerView() {
        return innerView;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Remove the status bar from the activity
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        goneAway();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        goneAway();
    }

    private void goneAway() {
        if (!dismissed) {
            dismissed = true;
            try {
                // Restore the window attributes
                getWindow().setAttributes(originalParams);
            } catch (IllegalArgumentException exp) {
                // Dialog was not on assigned to a parent view
                exp.printStackTrace();
            }
        }
    }
}
