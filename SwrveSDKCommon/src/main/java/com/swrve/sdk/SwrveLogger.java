package com.swrve.sdk;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import timber.log.Timber;

/**
 * A wrapper around Timber Logger.
 */
public class SwrveLogger {

    private static final String LOG_TAG = "SwrveSDK";

    private static final int LOG_LEVEL_DEFAULT = Log.WARN;
    private static int logLevel = -1;
    private static boolean logLevelSet = false;
    private static boolean swrveLoggerPlanted = false;

    private static synchronized void plantSwrveLogger() {
        if (logLevelSet == false) {
            logLevel = getLogLevelFromSystemProps();
            logLevelSet = true;
        }
        if (swrveLoggerPlanted == false) {
            Timber.plant(new SwrveLoggerTree());
            swrveLoggerPlanted = true;
        }
    }

    public static void v(String message, Object... args){
        verbose(LOG_TAG, message, args);
    }

    private static void verbose(String tag, String message, Object... args){
        plantSwrveLogger();
        Timber.tag(tag);
        Timber.v(message, args);
    }

    public static void d(String message, Object... args){
        debug(LOG_TAG, message, args);
    }

    private static void debug(String tag, String message, Object... args){
        plantSwrveLogger();
        Timber.tag(tag);
        Timber.d(message, args);
    }

    public static void i(String message, Object... args){
        info(LOG_TAG, message, args);
    }

    private static void info(String tag, String message, Object... args){
        plantSwrveLogger();
        Timber.tag(tag);
        Timber.i(message, args);
    }

    public static void w(String message, Object... args){
        warn(LOG_TAG, message, args);
    }

    private static void warn(String tag, String message, Object... args){
        plantSwrveLogger();
        Timber.tag(tag);
        Timber.w(message, args);
    }

    public static void w(String tag, String message, Throwable t, Object... args){
        warn(tag, message, t, args);
    }

    private static void warn(String tag, String message, Throwable t, Object... args){
        plantSwrveLogger();
        Timber.tag(tag);
        Timber.w(t, message, args);
    }

    public static void e(String message, Object... args){
        error(LOG_TAG, message, args);
    }

    private static void error(String tag, String message, Object... args){
        plantSwrveLogger();
        Timber.tag(tag);
        Timber.e(message, args);
    }

    public static void e(String message, Throwable t, Object... args){
        error(LOG_TAG, message, t, args);
    }

    private static void error(String tag, String message, Throwable t, Object... args){
        plantSwrveLogger();
        Timber.tag(tag);
        Timber.e(t, message, args);
    }

    public static void wtf(String message, Object... args){
        wtf(LOG_TAG, message, args);
    }

    public static void wtf(String tag, String message, Object... args){
        plantSwrveLogger();
        Timber.tag(tag);
        Timber.wtf(message, args);
    }

    public static void wtf(String tag, String message, Throwable t, Object... args){
        plantSwrveLogger();
        Timber.tag(tag);
        Timber.wtf(t, message, args);
    }

    public static int getLogLevel() {
        return SwrveLogger.logLevel;
    }

    public static void setLogLevel(int logLevel) {
        SwrveLogger.logLevel = logLevel;
        SwrveLogger.logLevelSet = true;
    }

    protected static int getLogLevelFromSystemProps() {
        int logLevel = LOG_LEVEL_DEFAULT;
        String systemProp = getSystemProp("log.tag." + LOG_TAG);
        if (SwrveHelper.isNotNullOrEmpty(systemProp)) {
            try {
                logLevel = Integer.valueOf(systemProp);
            } catch (Exception ex) {
                // using Android Log here instead of SwrveLogger
                Log.e(LOG_TAG, "Found SwrveLogger system loglevel prop but failed to read it. systemProp:" + systemProp, ex);
            }
        }
        return logLevel;
    }

    /*
     * Note this is reading the native System Properties and not the java System properties.
     * Hence it doesn't use System.getProperty.
     */
    private static String getSystemProp(String propName) {
        String propValue = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            String command = "/system/bin/getprop";
            Process process = Runtime.getRuntime().exec(new String[]{command, propName});
            inputStreamReader = new InputStreamReader(process.getInputStream());
            bufferedReader = new BufferedReader(inputStreamReader);
            propValue = bufferedReader.readLine();
        } catch (Exception e) {
            // using Android Log here instead of SwrveLogger
            Log.w(LOG_TAG, "Failure to read prop:" + propName + ". Using Swrve default log level:" + LOG_LEVEL_DEFAULT);
        } finally {
            if (inputStreamReader != null) try { inputStreamReader.close(); } catch (Exception ex) { }
            if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception ex) { }
        }
        return propValue;
    }

    protected static class SwrveLoggerTree extends Timber.DebugTree {
        @Override
        protected boolean isLoggable(String tag, int priority) {
            return priority >= logLevel;
        }
    }
}
