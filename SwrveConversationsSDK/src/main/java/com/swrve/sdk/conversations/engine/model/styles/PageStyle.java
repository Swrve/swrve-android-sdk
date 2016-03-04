package com.swrve.sdk.conversations.engine.model.styles;

import java.io.Serializable;

public class PageStyle implements Serializable {
    private BackgroundStyle bg;

    public PageStyle(){

    }

    public BackgroundStyle getBg() {
        return bg;
    }

    public void setBg(BackgroundStyle bg) {
        this.bg = bg;
    }
}