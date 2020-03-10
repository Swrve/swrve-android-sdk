package com.swrve.sdk;

import java.util.Map;

public interface SwrveRealTimeUserPropertiesListener {

    /**
     * This method is invoked asynchronously to return the request response of
     * the Swrve::getRealTimeUserProperties().
     *
     * Note: this method is invoked from a different thread than the thread used
     * to call Swrve::getRealTimeUserProperties().
     *
     * @param properties       the real time user properties represented as a map.
     * @param propertiesAsJSON the properties as JSON.
     */
    void onRealTimeUserPropertiesSuccess(final Map<String, String> properties, String propertiesAsJSON);

    /**
     * Called back on error.
     *
     * @param exception Exception caused when trying to obtain user resources.
     */
    void onRealTimeUserPropertiesError(Exception exception);
}
