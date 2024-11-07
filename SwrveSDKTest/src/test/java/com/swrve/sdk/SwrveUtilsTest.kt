package com.swrve.sdk

import android.os.Build
import com.swrve.sdk.SwrveUtils.Companion.parseIso8601Date
import com.swrve.sdk.messaging.SwrveBaseCampaign.SwrveTimezoneType
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SwrveUtilsTest : SwrveBaseTest() {
    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    @After
    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O]) // API 26 and above
    @Throws(Exception::class)
    fun testParseIso8601Date_GlobalDateApi26Plus() {
        val isoDate = "2023-09-11T10:15:30Z" // ISO 8601 date in UTC
        val expectedDate = Date.from(Instant.parse(isoDate))
        val parsedDate = parseIso8601Date(isoDate, SwrveTimezoneType.GLOBAL)
        Assert.assertEquals(expectedDate, parsedDate)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O]) // API 26 and above
    @Throws(Exception::class)
    fun testParseIso8601Date_LocalDateApi26Plus() {
        val isoDate = "2023-09-11T10:15:30" // Local date, no timezone
        val localDateTime = LocalDateTime.parse(isoDate)
        val zoneId = ZoneId.systemDefault()
        val zoneOffset = zoneId.rules.getOffset(localDateTime)
        val expectedDate = Date.from(localDateTime.toInstant(zoneOffset))
        val parsedDate = parseIso8601Date(isoDate, SwrveTimezoneType.LOCAL)
        Assert.assertEquals(expectedDate, parsedDate)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N]) // API 25 and below
    @Throws(Exception::class)
    fun testParseIso8601Date_GlobalDateApi25AndBelow() {
        val isoDate = "2023-09-11T10:15:30Z" // ISO 8601 date in UTC
        // Expected date using SimpleDateFormat workaround (Z replaced with +0000)
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val expectedDate = sdf.parse("2023-09-11T10:15:30+0000")
        val parsedDate = parseIso8601Date(isoDate, SwrveTimezoneType.GLOBAL)
        Assert.assertEquals(expectedDate, parsedDate)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N]) // API 25 and below
    @Throws(Exception::class)
    fun testParseIso8601Date_LocalDateApi25AndBelow() {
        val isoDate = "2023-09-11T10:15:30" // Local date, no timezone
        // Expected date using SimpleDateFormat
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        val expectedDate = sdf.parse(isoDate)
        val parsedDate = parseIso8601Date(isoDate, SwrveTimezoneType.LOCAL)
        Assert.assertEquals(expectedDate, parsedDate)
    }
}
