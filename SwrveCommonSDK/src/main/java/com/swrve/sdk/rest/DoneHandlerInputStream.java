/*
 * SWRVE CONFIDENTIAL
 *
 * (c) Copyright 2010-2014 Swrve New Media, Inc. and its licensors.
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is and remains the property of Swrve
 * New Media, Inc or its licensors.  The intellectual property and technical
 * concepts contained herein are proprietary to Swrve New Media, Inc. or its
 * licensors and are protected by trade secret and/or copyright law.
 * Dissemination of this information or reproduction of this material is
 * strictly forbidden unless prior written permission is obtained from Swrve.
 */
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