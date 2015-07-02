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
