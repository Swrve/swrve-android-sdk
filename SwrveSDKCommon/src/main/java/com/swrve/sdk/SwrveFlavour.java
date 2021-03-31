package com.swrve.sdk;

public enum SwrveFlavour {
    AMAZON, CORE, FIREBASE, HUAWEI;

    @Override
    public String toString() {
        switch (this) {
            case AMAZON:
                return "amazon";
            case CORE:
                return "core";
            case FIREBASE:
                return "firebase";
            case HUAWEI:
                return "huawei";
            default:
                throw new IllegalArgumentException();
        }
    }
}
