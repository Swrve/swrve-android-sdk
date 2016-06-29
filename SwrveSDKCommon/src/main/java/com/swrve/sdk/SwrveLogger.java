package com.swrve.sdk;

import android.util.Log;

public class SwrveLogger {

    private static final String LOG_TAG = "SWRVE";

    // Config/Active/Inactive
    private static boolean isActive = true;

    /**
     * Enable the Swrve logger which will allow the Swrve sdk to send messages to logcat.
     */
    public void enableLogging() {
        SwrveLogger.setActive(true);
    }

    /**
     * Disable the Swrve logger which will stop the Swrve sdk from sending messages to logcat.
     */
    public void disableLogging() {
        SwrveLogger.setActive(false);
    }

    public static boolean isActive() {
        return SwrveLogger.isActive;
    }

    public static void setActive(boolean isActive) {
        SwrveLogger.isActive = isActive;
    }

    // Verbose
    public static void v(String message){
        verbose(message);
    }

    public static void v(String tag, String message){
        verbose(tag, message);
    }

    public static void verbose(String message){
        verbose(LOG_TAG, message);
    }

    public static void verbose(String tag, String message){
        if(!isActive){
            return;
        }
        Log.v(tag, message);
    }

    // Debug
    public static void d(String message){
        debug(LOG_TAG, message);
    }

    public static void d(String tag, String message){
        debug(tag, message);
    }

    public static void debug(String message){
        debug(LOG_TAG, message);
    }

    public static void debug(String tag, String message){
        if(!isActive){
            return;
        }
        Log.d(tag, message);
    }

    // Info
    public static void i(String message){
        info(LOG_TAG, message);
    }

    public static void i(String tag, String message){
        info(tag, message);
    }

    public static void info(String message){
        info(LOG_TAG, message);
    }

    public static void info(String tag, String message){
        if(!isActive){
            return;
        }
        Log.i(tag, message);
    }

    // Warn
    public static void w(String message){
        warn(LOG_TAG, message);
    }

    public static void w(String tag, String message){
        warn(tag, message);
    }

    public static void w(String tag, String message, Throwable t){
        warn(tag, message);
    }


    public static void warn(String message){
        warn(LOG_TAG, message);
    }

    public static void warn(String tag, String message){
        if(!isActive){
            return;
        }
        Log.w(tag, message);
    }

    public static void warn(String tag, String message,  Throwable t){
        if(!isActive){
            return;
        }
        Log.w(tag, message, t);
    }

    // Error
    public static void e(String message){
        error(LOG_TAG, message);
    }

    public static void e(String message, Throwable t){
        error(LOG_TAG, message, t);
    }

    public static void e(String tag, String message){
        error(tag, message);
    }

    public static void e(String tag, String message, Throwable t){
        error(tag, message, t);
    }

    public static void error(String message){
        error(LOG_TAG, message);
    }

    public static void error(String tag, String message){
        if(!isActive){
            return;
        }
        Log.e(tag, message);
    }

    public static void error(String tag, String message, Throwable t){
        if(!isActive){
            return;
        }
        Log.e(tag, message, t);
    }


    public static void wtf(String message){
        wtf(LOG_TAG, message);
    }

    public static void wtf(String tag, String message){
        if(!isActive){
            return;
        }
        Log.wtf(tag, message);
    }

    public static void wtf(String tag, String message,  Throwable t){
        if(!isActive){
            return;
        }
        Log.wtf(tag, message, t);
    }


}
