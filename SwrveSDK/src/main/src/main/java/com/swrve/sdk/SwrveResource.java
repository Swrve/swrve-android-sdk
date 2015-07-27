package com.swrve.sdk;

import android.util.Log;

import java.util.Map;

/**
 * Represents a resource set up in the dashboard.
 */
public class SwrveResource {

    protected static final String LOG_TAG = "SwrveSDK";
    protected Map<String, String> attributes;

    public SwrveResource(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    protected String _getAttributeAsString(String attributeId, String defaultValue) {
        if (this.attributes.containsKey(attributeId)) {
            return this.attributes.get(attributeId);
        }
        return defaultValue;
    }

    protected int _getAttributeAsInt(String attributeId, int defaultValue) {
        if (this.attributes.containsKey(attributeId)) {
            try {
                return Integer.parseInt(this.attributes.get(attributeId));
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, "Could not retrieve attribute " + attributeId + " as integer value, returning default value instead");
            }
        }
        return defaultValue;
    }

    protected float _getAttributeAsFloat(String attributeId, float defaultValue) {
        if (this.attributes.containsKey(attributeId)) {
            try {
                return Float.parseFloat(this.attributes.get(attributeId));
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, "Could not retrieve attribute " + attributeId + " as float value, returning default value instead");
            }
        }
        return defaultValue;
    }

    protected boolean _getAttributeAsBoolean(String attributeId, boolean defaultValue) {
        if (this.attributes.containsKey(attributeId)) {
            String value = this.attributes.get(attributeId);
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) {
                return true;
            } else {
                return false;
            }
        }
        return defaultValue;
    }


    /**
     * Get a resource attribute as a string, or a default value.
     * @param attributeId attribute identifier.
     * @param defaultValue default attribute value.
     * @return value of the resource or default value.
     */
    public String getAttributeAsString(String attributeId, String defaultValue) {
        try {
            return _getAttributeAsString(attributeId, defaultValue);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return defaultValue;
    }

    /**
     * Get a resource attribute as an integer, or a default value.
     * @param attributeId attribute identifier.
     * @param defaultValue default attribute value.
     * @return value of the resource or default value.
     */
    public int getAttributeAsInt(String attributeId, int defaultValue) {
        try {
            return _getAttributeAsInt(attributeId, defaultValue);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return defaultValue;
    }

    /**
     * Get a resource attribute as a float, or a default value.
     * @param attributeId attribute identifier.
     * @param defaultValue default attribute value.
     * @return value of the resource or default value.
     */
    public float getAttributeAsFloat(String attributeId, float defaultValue) {
        try {
            return _getAttributeAsFloat(attributeId, defaultValue);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return defaultValue;
    }

    /**
     * Get a resource attribute as a boolean, or a default value.
     * @param attributeId attribute identifier.
     * @param defaultValue default attribute value.
     * @return value of the resource or default value.
     */
    public boolean getAttributeAsBoolean(String attributeId, boolean defaultValue) {
        try {
            return _getAttributeAsBoolean(attributeId, defaultValue);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return defaultValue;
    }
}
