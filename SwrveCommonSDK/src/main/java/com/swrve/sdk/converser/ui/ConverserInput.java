package com.swrve.sdk.converser.ui;

import java.util.Map;

public interface ConverserInput {
    public void onReplyDataRequired(Map<String, Object> dataMap);

    /**
     * perform any validation needed
     *
     * @return null if validated ok. Otherwise a message detailing issue
     */
    public String validate();
}
