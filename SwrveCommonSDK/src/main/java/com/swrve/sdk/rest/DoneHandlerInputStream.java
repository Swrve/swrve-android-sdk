package com.swrve.sdk.rest;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This input stream won't read() after the underlying stream is exhausted.
 * http://code.google.com/p/android/issues/detail?id=14562
 */
public final class DoneHandlerInputStream extends FilterInputStream {
    private boolean done;

    public DoneHandlerInputStream(InputStream stream) {
        super(stream);
    }

    @Override
    public int read(byte[] bytes, int offset, int count) throws IOException {
        if (!done) {
            int result = super.read(bytes, offset, count);
            if (result != -1) {
                return result;
            }
        }
        done = true;
        return -1;
    }
}