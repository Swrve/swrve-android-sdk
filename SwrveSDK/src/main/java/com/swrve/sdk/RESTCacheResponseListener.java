package com.swrve.sdk;

import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Used internally to automatically save responses to local storage.
 */
abstract class RESTCacheResponseListener implements IRESTResponseListener {

    private String userId;
    private SwrveMultiLayerLocalStorage multiLayerLocalStorage;
    private String cacheCategory;
    private String defaultValue;
    private SwrveBase<?, ?> swrve;

    public RESTCacheResponseListener(SwrveBase<?, ?> swrve, SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId, String cacheCategory, String defaultValue) {
        this.multiLayerLocalStorage = multiLayerLocalStorage;
        this.userId = userId;
        this.cacheCategory = cacheCategory;
        this.defaultValue = defaultValue;
        this.swrve = swrve;
    }

    @Override
    public void onResponse(RESTResponse response) {
        String rawResponse = null;

        if (SwrveHelper.successResponseCode(response.responseCode)) {
            rawResponse = response.responseBody;
            try {
                multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, cacheCategory, response.responseBody, swrve.getUniqueKey(userId));
            } catch (Exception e) {
                // If we cannot create a signature, we just won't cache the response
            }
        } else {
            try {
                String cachedResponse = multiLayerLocalStorage.getSecureCacheEntryForUser(userId, cacheCategory, swrve.getUniqueKey(userId));
                rawResponse = cachedResponse;
            } catch (SecurityException e) {
                SwrveLogger.i("Signature for %s invalid; could not retrieve data from cache", cacheCategory);
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("name", "Swrve.signature_invalid");
                swrve.queueEvent(userId, "event", parameters, null, false);
            }
        }

        if (rawResponse == null || rawResponse.equals("")) {
            rawResponse = defaultValue;
        }

        onResponseCached(response.responseCode, rawResponse);
    }

    public abstract void onResponseCached(int responseCode, String responseBody);
}
