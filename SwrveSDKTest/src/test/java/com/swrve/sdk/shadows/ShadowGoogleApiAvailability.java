package com.swrve.sdk.shadows;

import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

/**
 * Minimal stand-in for org.robolectric:shadows-playservices' ShadowGoogleApiAvailability.
 * Robolectric removed the shadows-playservices module after 4.15.1, so there is no build of it
 * compatible with 4.16+. Only the availability code these tests exercise is shadowed.
 */
@Implements(GoogleApiAvailability.class)
public class ShadowGoogleApiAvailability {

    private static int googlePlayServicesAvailable = ConnectionResult.SERVICE_MISSING;

    public static void setIsGooglePlayServicesAvailable(int availabilityCode) {
        googlePlayServicesAvailable = availabilityCode;
    }

    @Resetter
    public static void reset() {
        googlePlayServicesAvailable = ConnectionResult.SERVICE_MISSING;
    }

    @Implementation
    protected int isGooglePlayServicesAvailable(Context context) {
        return googlePlayServicesAvailable;
    }
}
