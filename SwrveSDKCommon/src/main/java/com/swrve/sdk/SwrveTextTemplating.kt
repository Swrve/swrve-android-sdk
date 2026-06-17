package com.swrve.sdk

import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

object SwrveTextTemplating {

    private val pattern: Pattern = Pattern.compile("\\$\\{([^}]*)\\}")
    private val patternFallback: Pattern = Pattern.compile("\\|fallback=\"([^}]*)\"\\}")
    private val patternJSONFallback: Pattern = Pattern.compile("\\|fallback=\\\\\"([^}]*)\\\\\"\\}")

    @JvmOverloads
    @JvmStatic
    @Throws(SwrveSDKTextTemplatingException::class)
    fun apply(text: String?, properties: Map<String, String>?, freemarkerEnabled: Boolean = false): String? =
        apply(text, properties, freemarkerEnabled, false)

    // Separate overload (no default args) rather than adding a defaulted parameter to the original
    // signature above — that would change its generated `apply$default` ABI and break already-compiled
    // Kotlin callers (e.g. android-geo-sdk) that linked against the previous default-argument signature.
    @JvmStatic
    @Throws(SwrveSDKTextTemplatingException::class)
    fun apply(text: String?, properties: Map<String, String>?, freemarkerEnabled: Boolean, useLocalTimezone: Boolean): String? {
        text ?: return null

        if (freemarkerEnabled) {
            try {
                return SwrveFreemarkerEvaluator.evaluate(text, properties ?: emptyMap<String, String>(), useLocalTimezone)
            } catch (e: Throwable) {
                if (e is Error && e !is StackOverflowError) throw e  // rethrow fatal JVM errors (OOM etc.)
                SwrveLogger.e("SwrveTextTemplating: FreeMarker evaluation failed: ${e.message}", e)
                throw SwrveSDKTextTemplatingException(e.message ?: "FreeMarker evaluation failed", e)
            }
        } else {
            return applySwrveTextTemplating(text, properties)
        }
    }

    private fun applySwrveTextTemplating(text: String, properties: Map<String, String>?): String {
        var result = text
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val templateFullValue = matcher.group(0)!!
            val fallback = getFallBack(templateFullValue)
            var property = matcher.group(1)!!
            if (fallback != null) {
                property = property.substring(0, property.indexOf("|fallback=\""))
            }
            val value = properties?.get(property)
            when {
                !SwrveHelper.isNullOrEmpty(value) -> result = result.replace(templateFullValue, value!!)
                fallback != null -> result = result.replace(templateFullValue, fallback)
                else -> throw SwrveSDKTextTemplatingException("TextTemplating: Missing property value for key: $property")
            }
        }
        return result
    }

    private fun getFallBack(templateFullValue: String): String? {
        val matcher = patternFallback.matcher(templateFullValue)
        return if (matcher.find()) matcher.group(1) else null
    }

    @JvmOverloads
    @JvmStatic
    @Throws(SwrveSDKTextTemplatingException::class)
    fun applytoJSON(json: String?, properties: Map<String, String>?, freemarkerEnabled: Boolean = false): String? =
        applytoJSON(json, properties, freemarkerEnabled, false)

    // Separate overload (no default args) — see the note on apply() above for the ABI rationale.
    @JvmStatic
    @Throws(SwrveSDKTextTemplatingException::class)
    fun applytoJSON(json: String?, properties: Map<String, String>?, freemarkerEnabled: Boolean, useLocalTimezone: Boolean): String? {
        json ?: return null

        if (freemarkerEnabled) {
            try {
                val props = properties ?: emptyMap()
                return when {
                    json.trimStart().startsWith("{") -> applyFreemarkerToJSONObject(JSONObject(json), props, useLocalTimezone).toString()
                    json.trimStart().startsWith("[") -> applyFreemarkerToJSONArray(JSONArray(json), props, useLocalTimezone).toString()
                    else -> throw SwrveSDKTextTemplatingException("FreeMarker evaluation failed: unsupported JSON root type")
                }
            } catch (e: Throwable) {
                if (e is Error && e !is StackOverflowError) throw e  // rethrow fatal JVM errors (OOM etc.)
                SwrveLogger.e("SwrveTextTemplating: FreeMarker evaluation failed: ${e.message}", e)
                throw SwrveSDKTextTemplatingException(e.message ?: "FreeMarker evaluation failed", e)
            }
        }

        var result = json
        val matcher = pattern.matcher(json)
        while (matcher.find()) {
            val templateFullValue = matcher.group(0)!!
            val fallback = getFallBackJSON(templateFullValue)
            var property = matcher.group(1)!!
            if (fallback != null) {
                property = property.substring(0, property.indexOf("|fallback=\\\""))
            }
            val value = properties?.get(property)
            when {
                !SwrveHelper.isNullOrEmpty(value) -> result = result!!.replace(templateFullValue, value!!)
                fallback != null -> result = result!!.replace(templateFullValue, fallback)
                else -> throw SwrveSDKTextTemplatingException("TextTemplating: Missing property value for key: $property")
            }
        }
        return result
    }

    private fun applyFreemarkerToJSONObject(jsonObject: JSONObject, properties: Map<String, String>, useLocalTimezone: Boolean): JSONObject {
        val result = JSONObject()
        for (key in jsonObject.keys()) {
            val resolvedKey = SwrveFreemarkerEvaluator.evaluate(key, properties, useLocalTimezone)
            val value = jsonObject.get(key)
            result.put(resolvedKey, applyFreemarkerToJSONValue(value, properties, useLocalTimezone))
        }
        return result
    }

    private fun applyFreemarkerToJSONArray(jsonArray: JSONArray, properties: Map<String, String>, useLocalTimezone: Boolean): JSONArray {
        val result = JSONArray()
        for (i in 0 until jsonArray.length()) {
            result.put(applyFreemarkerToJSONValue(jsonArray.get(i), properties, useLocalTimezone))
        }
        return result
    }

    private fun applyFreemarkerToJSONValue(value: Any, properties: Map<String, String>, useLocalTimezone: Boolean): Any {
        return when (value) {
            is String -> SwrveFreemarkerEvaluator.evaluate(value, properties, useLocalTimezone)
            is JSONObject -> applyFreemarkerToJSONObject(value, properties, useLocalTimezone)
            is JSONArray -> applyFreemarkerToJSONArray(value, properties, useLocalTimezone)
            else -> value
        }
    }

    private fun getFallBackJSON(templateFullValue: String): String? {
        val matcher = patternJSONFallback.matcher(templateFullValue)
        return if (matcher.find()) matcher.group(1) else null
    }

    @JvmStatic
    fun hasPatternMatch(text: String?): Boolean {
        text ?: return false
        return pattern.matcher(text).find()
    }
}
