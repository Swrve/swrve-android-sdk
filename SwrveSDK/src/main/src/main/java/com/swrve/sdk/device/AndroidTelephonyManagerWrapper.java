package com.swrve.sdk.device;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * Used internally to obtain carrier information.
 */
public class AndroidTelephonyManagerWrapper implements ITelephonyManager {
    private final TelephonyManager manager;

    public AndroidTelephonyManagerWrapper(Context context) {
        manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public String getSimOperatorName() {
        if (manager != null) {
            return manager.getSimOperatorName();
        }
        return null;
    }

    @Override
    public String getSimCountryIso() {
        if (manager != null) {
            return manager.getSimCountryIso();
        }
        return null;
    }

    @Override
    public String getSimOperator() {
        if (manager != null) {
            return manager.getSimOperator();
        }
        return null;
    }
}
