package com.swrve.sdk.rest;

/**
 * Used internally to define an interface to handle REST client responses.
 */
public interface IRESTResponseListener {
    void onResponse(RESTResponse response);

    void onException(Exception exp);
}
