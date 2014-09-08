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
package com.swrve.sdk.rest;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Used internally to define a REST client.
 */
public interface IRESTClient {
    void get(String endpoint, IRESTResponseListener callback);

    void get(String endpoint, Map<String, String> params, IRESTResponseListener callback) throws UnsupportedEncodingException;

    void post(String endpoint, String encodedBody, IRESTResponseListener callback);

    void post(String endpoint, String encodedBody, IRESTResponseListener callback, String contentType);
}
