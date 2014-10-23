package com.swrve.sdk.device;

/**
 * Used internally to obtain carrier information.
 */
public interface ITelephonyManager {
    String getSimOperatorName();
    String getSimCountryIso();
    String getSimOperator();
}
