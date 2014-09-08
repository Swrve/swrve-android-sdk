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

import java.util.List;
import java.util.Map;

/**
 * Used internally to wrap REST responses.
 */
public class RESTResponse {
    public int responseCode;
    public String responseBody;
    public Map<String, List<String>> responseHeaders;

    public RESTResponse(int responseCode, String responseBody, Map<String, List<String>> responseHeaders) {
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.responseHeaders = responseHeaders;
    }

    public String getHeaderValue(String key) {
        if (this.responseHeaders != null) {
            List<String> header = this.responseHeaders.get(key);
            if (header != null && header.size() > 0) {
                return header.get(0);
            }
        }
        return null;
    }
}
