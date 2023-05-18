package com.swrve.sdk;

import java.util.HashMap;

/**
 * Used internally.
 */
public class SwrveAssetsTypes {

    public static final HashMap<String, String> MIMETYPES;

    static {
        MIMETYPES = new HashMap<String, String>() {
            {
                put("image/jpeg", ".jpeg");
                put("image/png", ".png");
                put("image/gif", ".gif");
                put("image/bmp", ".bmp");
            }
        };
    }
}
