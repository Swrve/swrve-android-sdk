package com.swrve.sdk.localstorage;

public class LocalStorageTestUtils {

    public static void closeSQLiteOpenHelperInstance() {
        SwrveSQLiteOpenHelper.closeInstance();
    }
}
