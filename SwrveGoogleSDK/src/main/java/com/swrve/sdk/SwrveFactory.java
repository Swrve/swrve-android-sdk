package com.swrve.sdk;

/**
 * Use this class to obtain an instance of the Swrve Google SDK.
 */
public class SwrveFactory extends SwrveFactoryBase {

    /**
     * Create a new instance of the Swrve Google SDK.
     * @return new instance of the SDK.
     */
    public static ISwrve createInstance() {
        if (sdkAvailable()) {
            return new Swrve();
        }

        return new SwrveEmpty();
    }
}
