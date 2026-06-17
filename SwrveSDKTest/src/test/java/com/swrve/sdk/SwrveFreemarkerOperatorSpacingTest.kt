package com.swrve.sdk

import org.junit.Assert.assertEquals
import org.junit.Test

class SwrveFreemarkerOperatorSpacingTest : SwrveFreemarkerTestBase() {

    @Test
    fun testEqualsNoSpaces() {
        assertEquals("Match", eval("<#if Recipient.country==\"UK\">Match<#else>No match</#if>", mapOf("Recipient.country" to "UK")))
    }

    @Test
    fun testEqualsNoSpacesNonMatch() {
        assertEquals("No match", eval("<#if Recipient.country==\"UK\">Match<#else>No match</#if>", mapOf("Recipient.country" to "US")))
    }

    @Test
    fun testNotEqualsNoSpaces() {
        assertEquals("Not UK", eval("<#if Recipient.country!=\"UK\">Not UK<#else>UK</#if>", mapOf("Recipient.country" to "US")))
    }

    @Test
    fun testEqualsLeadingSpaceOnly() {
        assertEquals("Match", eval("<#if Recipient.country ==\"UK\">Match</#if>", mapOf("Recipient.country" to "UK")))
    }

    @Test
    fun testEqualsTrailingSpaceOnly() {
        assertEquals("Match", eval("<#if Recipient.country== \"UK\">Match</#if>", mapOf("Recipient.country" to "UK")))
    }

    @Test
    fun testEqualsNoSpacesWithTrim() {
        assertEquals("Match", eval("<#if Recipient.name?trim==\"Alice\">Match<#else>No match</#if>", mapOf("Recipient.name" to "  Alice  ")))
    }
}
