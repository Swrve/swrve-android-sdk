package com.swrve.sdk.rest;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class SwrveFilterInputStream extends FilterInputStream {
    private boolean hasMoreToRead = true;

    public SwrveFilterInputStream(InputStream stream) {
        super(stream);
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        if (hasMoreToRead) {
            int result = super.read(buffer, byteOffset, byteCount);
            if (result != -1) {
                return result;
            }
        }
        hasMoreToRead = false;
        return -1;
    }
}