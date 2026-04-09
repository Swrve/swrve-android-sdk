package com.swrve.sdk;

import com.swrve.sdk.rest.RESTResponse;

/**
 * Used internally to handle response from the batch API.
 */
public interface IPostBatchRequestListener {

    void onResponse(boolean shouldDelete, RESTResponse response, String userId);
}
