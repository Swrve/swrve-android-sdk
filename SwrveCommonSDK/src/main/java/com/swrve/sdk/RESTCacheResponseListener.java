package com.swrve.sdk;

import android.util.Log;

import com.swrve.sdk.localstorage.IEntryStorage;
import com.swrve.sdk.localstorage.MemoryCachedEventLocalStorage;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Used internally to automatically save responses to local storage.
 */
abstract class RESTCacheResponseListener implements IRESTResponseListener {
    private IEntryStorage userEntryStorage;
    private String cacheCategory;
    private String defaultValue;
    private SwrveBase<?, ?> swrve;

    public RESTCacheResponseListener(SwrveBase<?, ?> swrve, IEntryStorage userEntryStorage, String cacheCategory, String defaultValue) {
        this.userEntryStorage = userEntryStorage;
        this.cacheCategory = cacheCategory;
        this.defaultValue = defaultValue;
        this.swrve = swrve;
    }

    @Override
    public void onResponse(RESTResponse response) {
        String rawResponse = null;

        if (SwrveHelper.successResponseCode(response.responseCode)) {
            rawResponse = response.responseBody;
            userEntryStorage.putUserSecureString(cacheCategory, response.responseBody);
        } else {
            try {
                rawResponse = userEntryStorage.getUserSecureString(cacheCategory, null);
            } catch (SecurityException e) {
                Log.i("REST", "Signature for " + cacheCategory + " invalid: could not retrieve data from cache");
                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("name", "Swrve.signature_invalid");
                swrve.queueEvent("event", parameters, null);
            }
        }

        if (SwrveHelper.isNullOrEmpty(rawResponse)) {
            rawResponse = defaultValue;
        }

        onResponseCached(response.responseCode, rawResponse);
    }

    public abstract void onResponseCached(int responseCode, String responseBody);
}
