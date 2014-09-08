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

import android.content.Intent;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.gcm.ISwrvePushNotificationListener;

/**
 * Empty implementation of the Swrve Google SDK. Will be returned when the SDK is used from an unsupported runtime version (< 2.3.3).
 */
public class SwrveEmpty extends SwrveBaseEmpty<ISwrve, SwrveConfig> implements ISwrve {

    @Override
    public void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener) {
    }

    @Override
    public void iapPlay(String productId, double productPrice, String currency, String receipt, String receiptSignature) {
    }

    @Override
    public void iapPlay(String productId, double productPrice, String currency, SwrveIAPRewards rewards, String receipt, String receiptSignature) {
    }

    @Override
    public void processIntent(Intent intent) {
    }
}
