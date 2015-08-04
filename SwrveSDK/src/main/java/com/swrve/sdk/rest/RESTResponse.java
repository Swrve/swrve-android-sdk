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
