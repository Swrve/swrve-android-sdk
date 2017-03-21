package com.swrve.sdk.gcm;

public final class SwrveGcmConstants {

    /**
     * @deprecated use {@link com.swrve.sdk.SwrvePushConstants#SWRVE_TRACKING_KEY} instead
     */
    @Deprecated
    public static final String SWRVE_TRACKING_KEY = "_p";

    /**
     * @deprecated use {@link com.swrve.sdk.SwrvePushConstants#PUSH_BUNDLE} instead
     */
    @Deprecated
    public static final String GCM_BUNDLE = "notification";

    public static final String SWRVE_DEFAULT_INTENT_SERVICE = "com.swrve.sdk.gcm.SwrveGcmIntentService";

    /**
     * @deprecated use {@link com.swrve.sdk.SwrvePushConstants#DEEPLINK_KEY} instead
     */
    @Deprecated
    public static final String DEEPLINK_KEY = "_sd";
}