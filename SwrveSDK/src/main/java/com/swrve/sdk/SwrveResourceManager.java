package com.swrve.sdk;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Use this resource manager to obtain the latest resources and their values.
 */
public class SwrveResourceManager {

    protected static final String LOG_TAG = "SwrveSDK";
    protected Map<String, SwrveResource> resources;

    public SwrveResourceManager() {
        this.resources = new HashMap<String, SwrveResource>();
    }

    protected void _setResourcesFromJSON(JSONArray jsonResources) {
        try {
            // Convert to map
            int numResources = jsonResources.length();
            synchronized (this.resources) {
                this.resources = new HashMap<String, SwrveResource>();
                for (int i = 0; i < numResources; i++) {
                    JSONObject resourceJSON = jsonResources.getJSONObject(i);
                    String uid = resourceJSON.getString("uid");
                    SwrveResource resource = new SwrveResource(SwrveHelper.JSONToMap(resourceJSON));
                    this.resources.put(uid, resource);
                }
            }
        } catch (JSONException e) {
            Log.i(LOG_TAG, "Invalid JSON received for resources, resources not updated");
        }
    }

    protected Map<String, SwrveResource> _getResources() {
        return this.resources;
    }

    protected SwrveResource _getResource(String resourceId) {
        if (this.resources.containsKey(resourceId)) {
            return this.resources.get(resourceId);
        }
        return null;
    }

    protected String _getAttributeAsString(String resourceId, String attributeId, String defaultValue) {
        SwrveResource resource = this.getResource(resourceId);
        if (resource != null) {
            return resource.getAttributeAsString(attributeId, defaultValue);
        }
        return defaultValue;
    }

    protected int _getAttributeAsInt(String resourceId, String attributeId, int defaultValue) {
        SwrveResource resource = this.getResource(resourceId);
        if (resource != null) {
            return resource.getAttributeAsInt(attributeId, defaultValue);
        }
        return defaultValue;
    }

    protected float _getAttributeAsFloat(String resourceId, String attributeId, float defaultValue) {
        SwrveResource resource = this.getResource(resourceId);
        if (resource != null) {
            return resource.getAttributeAsFloat(attributeId, defaultValue);
        }
        return defaultValue;
    }

    protected boolean _getAttributeAsBoolean(String resourceId, String attributeId, boolean defaultValue) {
        SwrveResource resource = this.getResource(resourceId);
        if (resource != null) {
            return resource.getAttributeAsBoolean(attributeId, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Update the resources with the JSON content coming from the Swrve servers.
     * @param jsonResources
     */
    public void setResourcesFromJSON(JSONArray jsonResources) {
        try {
            _setResourcesFromJSON(jsonResources);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    /**
     * Get the latest resources available.
     * @return the latest resources available in the form of (uid->resource).
     */
    public Map<String, SwrveResource> getResources() {
        try {
            return _getResources();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    /**
     * Get a resource by its uid as set in the dashboard.
     * @param resourceId resource unique identifier.
     * @return resource.
     */
    public SwrveResource getResource(String resourceId) {
        try {
            return _getResource(resourceId);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    /**
     * Get a resource attribute as a string, or a default value.
     * @param resourceId resource resource unique identifier.
     * @param attributeId attribute identifier.
     * @param defaultValue default value.
     * @return value of the resource or default value.
     */
    public String getAttributeAsString(String resourceId, String attributeId, String defaultValue) {
        try {
            return _getAttributeAsString(resourceId, attributeId, defaultValue);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return defaultValue;
    }

    /**
     * Get a resource attribute as an integer, or a default value.
     * @param resourceId resource resource unique identifier.
     * @param attributeId attribute identifier.
     * @param defaultValue default value.
     * @return value of the resource or default value.
     */
    public int getAttributeAsInt(String resourceId, String attributeId, int defaultValue) {
        try {
            return _getAttributeAsInt(resourceId, attributeId, defaultValue);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return defaultValue;
    }

    /**
     * Get a resource attribute as a float, or a default value.
     * @param resourceId resource resource unique identifier.
     * @param attributeId attribute identifier.
     * @param defaultValue default value.
     * @return value of the resource or default value.
     */
    public float getAttributeAsFloat(String resourceId, String attributeId, float defaultValue) {
        try {
            return _getAttributeAsFloat(resourceId, attributeId, defaultValue);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return defaultValue;
    }

    /**
     * Get a resource attribute as a boolean, or a default value.
     * @param resourceId resource resource unique identifier.
     * @param attributeId attribute identifier.
     * @param defaultValue default value.
     * @return value of the resource or default value.
     */
    public boolean getAttributeAsBoolean(String resourceId, String attributeId, boolean defaultValue) {
        try {
            return _getAttributeAsBoolean(resourceId, attributeId, defaultValue);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return defaultValue;
    }
}
