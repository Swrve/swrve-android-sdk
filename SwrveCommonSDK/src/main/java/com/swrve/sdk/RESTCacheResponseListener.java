package com.swrve.sdk;

import android.util.Log;

import com.swrve.sdk.localstorage.MemoryCachedLocalStorage;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Used internally to automatically save responses to local storage.
 */
abstract class RESTCacheResponseListener implements IRESTResponseListener {

    private String userId;
    private MemoryCachedLocalStorage memorylocalStorage;
    private String cacheCategory;
    private String defaultValue;
    private SwrveBase<?, ?> swrve;

    public RESTCacheResponseListener(SwrveBase<?, ?> swrve, MemoryCachedLocalStorage memoryLocalStorage, String userId, String cacheCategory, String defaultValue) {
        this.memorylocalStorage = memoryLocalStorage;
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
                memorylocalStorage.setAndFlushSecureSharedEntryForUser(userId, cacheCategory, response.responseBody, swrve.getUniqueKey());
            } catch (Exception e) {
                // If we cannot create a signature, we just won't cache the response
            }
        } else {
            try {
                String cachedResponse = memorylocalStorage.getSecureCacheEntryForUser(userId, cacheCategory, swrve.getUniqueKey());
                rawResponse = cachedResponse;
            } catch (SecurityException e) {
                Log.i("REST", "Signature for " + cacheCategory + " invalid; could not retrieve data from cache");
                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("name", "Swrve.signature_invalid");
                swrve.queueEvent("event", parameters, null);
            }
        }

        if (rawResponse == null || rawResponse.equals("")) {
            rawResponse = defaultValue;
        }

        onResponseCached(response.responseCode, rawResponse);
    }

    public abstract void onResponseCached(int responseCode, String responseBody);
}
