package com.swrve.sdk;

public enum SwrveFlavour {
    AMAZON, CORE, FIREBASE;

    @Override
    public String toString() {
        switch (this) {
            case AMAZON:
                return "amazon";
            case CORE:
                return "core";
            case FIREBASE:
                return "firebase";
            default:
                throw new IllegalArgumentException();
        }
    }
}
