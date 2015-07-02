package com.swrve.sdk;

/**
 * Used internally to handle response from the batch API.
 */
public interface IPostBatchRequestListener {
    void onResponse(boolean shouldDelete);
}
