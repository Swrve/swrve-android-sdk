package com.swrve.sdk.localstorage;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Used internally to define a common storage for user persistent data.
 */
public interface IEntryStorage {

    void putGlobalString(String key, String value);

    String getGlobalString(String key, String defaultValue);

    void putUserInt(String key, int value);

    int getUserInt(String key, int defaultValue);

    void putUserString(String key, String value);

    String getUserString(String key, String defaultValue);

    void removeUserString(String key);

    void putUserSecureString(String key, String value);

    String getUserSecureString(String key, String defaultValue) throws SecurityException;
}