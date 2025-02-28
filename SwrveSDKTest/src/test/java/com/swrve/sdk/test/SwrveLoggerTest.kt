package com.swrve.sdk.test

import com.swrve.sdk.SwrveBaseTest
import com.swrve.sdk.SwrveLogger
import org.junit.Assert
import org.junit.Test

class SwrveLoggerTest : SwrveBaseTest() {
    @Test
    fun testDebug() {
        try {
            SwrveLogger.d("I am the walrus")
        } catch (ex: Exception) {
            Assert.fail("Exception generated in SwrveLogger.w: " + ex.message)
        }
    }

    @Test
    fun testVerbose() {
        try {
            SwrveLogger.v("I am the walrus")
        } catch (ex: Exception) {
            Assert.fail("Exception generated in SwrveLogger.w: " + ex.message)
        }
    }

    @Test
    fun testInfo() {
        try {
            SwrveLogger.i("I am the walrus")
        } catch (ex: Exception) {
            Assert.fail("Exception generated in SwrveLogger.w: " + ex.message)
        }
    }

    @Test
    fun testWarn() {
        try {
            SwrveLogger.w("I am the walrus")
        } catch (ex: Exception) {
            Assert.fail("Exception generated in SwrveLogger.w: " + ex.message)
        }
    }

    @Test
    fun testError() {
        try {
            SwrveLogger.e("I am the walrus")
        } catch (ex: Exception) {
            Assert.fail("Exception generated in SwrveLogger.w: " + ex.message)
        }
    }

    @Test
    fun testWtf() {
        try {
            SwrveLogger.wtf("I am the walrus")
        } catch (ex: Exception) {
            Assert.fail("Exception generated in SwrveLogger.w: " + ex.message)
        }
    }
}
