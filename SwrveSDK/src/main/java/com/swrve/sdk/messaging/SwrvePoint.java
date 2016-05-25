package com.swrve.sdk.messaging;

import java.io.Serializable;

/* Wrapper class to make Point serializable */
public class SwrvePoint implements Serializable {
    public int x;
    public int y;

    public SwrvePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
