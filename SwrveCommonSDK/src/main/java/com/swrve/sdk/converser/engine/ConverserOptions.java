package com.swrve.sdk.converser.engine;

public class ConverserOptions {
    private static ConverserOptions _instance;
    private boolean allowedPickWeekends = true;

    private ConverserOptions() {
    }

    public synchronized static ConverserOptions getInstance() {
        if (_instance == null) {
            _instance = new ConverserOptions();
        }
        return _instance;
    }

    /**
     * Used by date pickers. defaults to true.
     *
     * @return
     */
    public boolean isAllowedPickWeekends() {
        return allowedPickWeekends;
    }

    /**
     * Used by date pickers. defaults to true.
     *
     * @param allowedPickWeekends
     */
    public void setAllowedPickWeekends(boolean allowedPickWeekends) {
        this.allowedPickWeekends = allowedPickWeekends;
    }
}
