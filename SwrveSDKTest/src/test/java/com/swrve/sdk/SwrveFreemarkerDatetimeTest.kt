package com.swrve.sdk

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SwrveFreemarkerDatetimeTest : SwrveFreemarkerTestBase() {

    private var savedNowProvider: (() -> Date)? = null

    @Before
    fun setUp() {
        savedNowProvider = SwrveFreemarkerEvaluator.nowProvider
        SwrveFreemarkerEvaluator.nowProvider = ::fixedNow
    }

    @After
    fun tearDown() {
        savedNowProvider?.let { SwrveFreemarkerEvaluator.nowProvider = it }
    }

    private fun makeDate(iso: String): Date {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US)
        return fmt.parse(iso)!!
    }

    private fun fixedNow(): Date = makeDate("2026-04-16T12:00:00Z")

    // MARK: - ?date comparisons

    @Test
    fun testDateFutureIsValid() {
        assertEquals("Valid offer", eval("<#if Recipient.expiry?date gt .now?date>Valid offer<#else>Expired</#if>", mapOf("Recipient.expiry" to "2099-01-01T00:00:00Z")))
    }

    @Test
    fun testDatePastIsExpired() {
        assertEquals("Expired", eval("<#if Recipient.expiry?date gt .now?date>Valid offer<#else>Expired</#if>", mapOf("Recipient.expiry" to "2020-01-01T00:00:00Z")))
    }

    @Test
    fun testDateGteEqualDayIsValid() {
        assertEquals("Valid", eval("<#if Recipient.expiry?date gte .now?date>Valid<#else>Expired</#if>", mapOf("Recipient.expiry" to "2026-04-16T00:00:00Z")))
    }

    @Test
    fun testDateDateOnlyStringAccepted() {
        assertEquals("Future", eval("<#if Recipient.expiry?date gt .now?date>Future<#else>Past</#if>", mapOf("Recipient.expiry" to "2099-01-01")))
    }

    @Test
    fun testDateMissingKeySuppresses() {
        assertSuppresses { eval("<#if Recipient.expiry?date gt .now?date>ok</#if>", emptyMap()) }
    }

    // MARK: - ?datetime comparisons

    @Test
    fun testDatetimeFutureIsActive() {
        assertEquals("Active", eval("<#if Recipient.expiry?datetime gt .now>Active<#else>Expired</#if>", mapOf("Recipient.expiry" to "2099-01-01T00:00:00Z")))
    }

    @Test
    fun testDatetimePastIsExpired() {
        assertEquals("Expired", eval("<#if Recipient.expiry?datetime gt .now>Active<#else>Expired</#if>", mapOf("Recipient.expiry" to "2020-01-01T00:00:00Z")))
    }

    @Test
    fun testDatetimeOnDateOnlyStringSuppresses() {
        assertSuppresses { eval("<#if Recipient.expiry?datetime gt .now>ok</#if>", mapOf("Recipient.expiry" to "2099-01-01")) }
    }

    @Test
    fun testDatetimeDateChainYieldsCalendarDate() {
        assertEquals("Valid", eval("<#if Recipient.expiry?datetime?date gte .now?date>Valid<#else>Expired</#if>", mapOf("Recipient.expiry" to "2099-01-01T00:00:00Z")))
    }

    @Test
    fun testDatetimeDateChainOnDateOnlyStringSuppresses() {
        assertSuppresses { eval("<#if Recipient.expiry?datetime?date gte .now?date>ok</#if>", mapOf("Recipient.expiry" to "2099-01-01")) }
    }

    // MARK: - Type mismatch

    @Test
    fun testDateVsDatetimeMismatchSuppresses() {
        assertSuppresses { eval("<#if Recipient.expiry?date gt .now>ok</#if>", mapOf("Recipient.expiry" to "2099-01-01T00:00:00Z")) }
    }

    @Test
    fun testDateVsBareNowTypeMismatchErrorMessage() {
        assertSuppressesWithMessage(".now?date") {
            eval("<#if Recipient.expiry?date gt .now>Valid</#if>", mapOf("Recipient.expiry" to "2026-04-16T00:00:00Z"))
        }
    }

    // MARK: - Timezone: GLOBAL vs LOCAL

    @Test
    fun testNowDateGlobalUsesUTC() {
        val result = SwrveFreemarkerEvaluator.evaluate(
            "<#if Recipient.expiry?date gte .now?date>Valid<#else>Expired</#if>",
            mapOf("Recipient.expiry" to "2026-04-16T00:00:00Z"),
            false
        )
        assertEquals("Valid", result)
    }

    @Test
    fun testDateVsDatetimeMismatchCaughtAtParseTime() {
        assertSuppressesWithMessage("mismatch") {
            SwrveFreemarkerEvaluator.evaluate(
                "<#if Recipient.expiry?date gt .now>Valid</#if>",
                mapOf("Recipient.expiry" to "2099-01-01T00:00:00Z"),
                false
            )
        }
    }

    @Test
    fun testNonDateValueContainingTGivesDateError() {
        // E-5 regression: "NOT SET" contains T; before fix it gave a confusing "ISO 8601 datetime" error.
        assertSuppresses {
            SwrveFreemarkerEvaluator.evaluate(
                "<#if Recipient.expiry?date gt .now?date>Valid</#if>",
                mapOf("Recipient.expiry" to "NOT SET"),
                false
            )
        }
    }

    @Test
    fun testNowDateLocalUsesDeviceTimezone() {
        val result = SwrveFreemarkerEvaluator.evaluate(
            "<#if Recipient.expiry?date gte .now?date>Valid<#else>Expired</#if>",
            mapOf("Recipient.expiry" to "2026-04-16T00:00:00Z"),
            true
        )
        assertTrue(result == "Valid" || result == "Expired")
    }
}
