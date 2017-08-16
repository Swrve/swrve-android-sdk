package com.swrve.sdk.model;

import java.io.Serializable;
import com.google.gson.annotations.SerializedName;

public class PayloadExpanded implements Serializable {

    private String title;

    private String body;

    private String iconUrl;

    /** getters **/

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getIconUrl() {
        return iconUrl;
    }
}
