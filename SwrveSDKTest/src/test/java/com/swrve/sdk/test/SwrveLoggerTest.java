package com.swrve.sdk.test;

import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveLogger;

import org.junit.Assert;
import org.junit.Test;

public class SwrveLoggerTest extends SwrveBaseTest {

    @Test
    public void testDebug() {
        try {
            SwrveLogger.d("I am the walrus");
        } catch (Exception ex) {
            Assert.fail("Exception generated in SwrveLogger.w: " + ex.getMessage());;
        }
    }

    @Test
    public void testVerbose() {
        try {
            SwrveLogger.v("I am the walrus");
        } catch (Exception ex) {
            Assert.fail("Exception generated in SwrveLogger.w: " + ex.getMessage());;
        }
    }

    @Test
    public void testInfo() {
        try {
            SwrveLogger.i("I am the walrus");
        } catch (Exception ex) {
            Assert.fail("Exception generated in SwrveLogger.w: " + ex.getMessage());
        }
    }

    @Test
    public void testWarn() {
        try {
            SwrveLogger.w("I am the walrus");
        } catch (Exception ex) {
            Assert.fail("Exception generated in SwrveLogger.w: " + ex.getMessage());;
        }
    }

    @Test
    public void testError() {
        try {
            SwrveLogger.e("I am the walrus");
        } catch (Exception ex) {
            Assert.fail("Exception generated in SwrveLogger.w: " + ex.getMessage());;
        }
    }

    @Test
    public void testWtf() {
        try {
            SwrveLogger.wtf("I am the walrus");
        } catch (Exception ex) {
            Assert.fail("Exception generated in SwrveLogger.w: " + ex.getMessage());;
        }
    }
}
