package com.swrve.sdk.localstorage;

import static com.swrve.sdk.SwrveTestUtils.removeSingleton;

public class LocalStorageTestUtils {

    public static void removeSQLiteOpenHelperSingletonInstance() throws Exception {
        removeSingleton(SwrveSQLiteOpenHelper.class, "instance");
    }
}
