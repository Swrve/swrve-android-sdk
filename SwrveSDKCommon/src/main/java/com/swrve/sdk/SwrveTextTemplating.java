package com.swrve.sdk;

import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwrveTextTemplating {
    private static final String patternMatch = "\\$\\{([^\\}]*)\\}"; // match any content beginning with ${ and ending in }
    private static final Pattern pattern = Pattern.compile(patternMatch);
    private static final String patternFallbackMatch = "\\|fallback=\"([^\\}]*)\"\\}";
    private static final Pattern patternFallback = Pattern.compile(patternFallbackMatch);

    public static String apply(String text, Map<String, String> properties) throws SwrveSDKTextTemplatingException {
        if (text == null || properties == null) {
            return text;
        }
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String templateFullValue = matcher.group(0);
            String fallback = getFallBack(templateFullValue);
            String property = matcher.group(1);
            if (fallback != null) {
                property = property.substring(0, property.indexOf("|fallback=\"")); // remove fallback text
            }

            if (!SwrveHelper.isNullOrEmpty(properties.get(property))) {
                text = text.replace(templateFullValue, properties.get(property));
            } else if (fallback != null) {
                text = text.replace(templateFullValue, fallback);
            } else {
                throw new SwrveSDKTextTemplatingException("TextTemplating: Missing property value for key " + property);
            }
        }
        return text;
    }

    // Example of expected template syntax:
    // ${item.property|fallback="fallback text"}
    private static String getFallBack(String templateFullValue) {
        String fallback = null;
        Matcher matcher = patternFallback.matcher(templateFullValue);
        while (matcher.find()) {
            fallback = matcher.group(1);
        }
        return fallback;
    }

    // Checks if the pattern exists within a given piece of text
    public static boolean hasPatternMatch(String text) {

        if (text == null) {
            return false;
        }

        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }
}
