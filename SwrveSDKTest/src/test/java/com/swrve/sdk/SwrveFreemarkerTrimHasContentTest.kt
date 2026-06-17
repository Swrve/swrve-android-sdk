package com.swrve.sdk

import org.junit.Assert.assertEquals
import org.junit.Test

class SwrveFreemarkerTrimHasContentTest : SwrveFreemarkerTestBase() {

    // MARK: - ?trim

    @Test
    fun testTrimStripsWhitespace() {
        assertEquals("Alice", eval("\${Recipient.name?trim}", mapOf("Recipient.name" to "  Alice  ")))
    }

    @Test
    fun testTrimLeadingOnly() {
        assertEquals("Bob", eval("\${Recipient.name?trim}", mapOf("Recipient.name" to "  Bob")))
    }

    @Test
    fun testTrimTrailingOnly() {
        assertEquals("Carol", eval("\${Recipient.name?trim}", mapOf("Recipient.name" to "Carol  ")))
    }

    @Test
    fun testTrimNoWhitespaceUnchanged() {
        assertEquals("Dave", eval("\${Recipient.name?trim}", mapOf("Recipient.name" to "Dave")))
    }

    @Test
    fun testTrimMissingKeySuppresses() {
        assertSuppresses { eval("\${Recipient.name?trim}", emptyMap()) }
    }

    @Test
    fun testTrimWhitespaceOnlyBecomesEmpty() {
        assertEquals("", eval("\${Recipient.name?trim}", mapOf("Recipient.name" to "   ")))
    }

    @Test
    fun testTrimStripsNewlines() {
        assertEquals("Alice", eval("\${Recipient.name?trim}", mapOf("Recipient.name" to "\n  Alice  \n")))
    }

    @Test
    fun testTrimStripsCRLF() {
        assertEquals("Bob", eval("\${Recipient.name?trim}", mapOf("Recipient.name" to "\r\n  Bob  \r\n")))
    }

    @Test
    fun testTrimNewlineOnlyBecomesEmpty() {
        assertEquals("", eval("\${Recipient.name?trim}", mapOf("Recipient.name" to "\n\r\n")))
    }

    // MARK: - ?has_content

    @Test
    fun testHasContentKeyPresentNonEmpty() {
        assertEquals("Hello Alice", eval("<#if Recipient.name?has_content>Hello \${Recipient.name}<#else>Hello there</#if>", mapOf("Recipient.name" to "Alice")))
    }

    @Test
    fun testHasContentKeyPresentEmpty() {
        assertEquals("No name", eval("<#if Recipient.name?has_content>Hi<#else>No name</#if>", mapOf("Recipient.name" to "")))
    }

    @Test
    fun testHasContentKeyMissing() {
        assertEquals("No name", eval("<#if Recipient.name?has_content>Hi<#else>No name</#if>", emptyMap()))
    }

    @Test
    fun testHasContentNegated() {
        assertEquals("No name", eval("<#if !Recipient.name?has_content>No name<#else>Has name</#if>", emptyMap()))
    }

    @Test
    fun testHasContentDistinctFromExistsOnEmptyString() {
        val props = mapOf<String, Any>("Recipient.name" to "")
        assertEquals("exists", eval("<#if Recipient.name??>exists</#if>", props))
        assertEquals("", eval("<#if Recipient.name?has_content>non-empty</#if>", props))
    }

    @Test
    fun testHasContentInInterpolationSuppresses() {
        assertSuppresses { eval("\${Recipient.name?has_content}", mapOf("Recipient.name" to "Alice")) }
    }

    // MARK: - ?trim?has_content chain

    @Test
    fun testTrimHasContentWhitespaceOnlyIsFalse() {
        assertEquals("Hi there", eval("<#if Recipient.name?trim?has_content>Hi \${Recipient.name?trim}<#else>Hi there</#if>", mapOf("Recipient.name" to "   ")))
    }

    @Test
    fun testTrimHasContentNonEmptyAfterTrim() {
        assertEquals("Hi Alice", eval("<#if Recipient.name?trim?has_content>Hi \${Recipient.name?trim}<#else>Hi there</#if>", mapOf("Recipient.name" to "  Alice  ")))
    }

    @Test
    fun testTrimHasContentMissingKeySuppresses() {
        assertSuppresses { eval("<#if Recipient.name?trim?has_content>Hi<#else>No name</#if>", emptyMap()) }
    }

    // MARK: - ?trim in equality conditions

    @Test
    fun testTrimEqualsMatchAfterTrim() {
        assertEquals("Domestic", eval("<#if Recipient.country?trim == \"US\">Domestic<#else>International</#if>", mapOf("Recipient.country" to "  US  ")))
    }

    @Test
    fun testTrimEqualsNoMatchAfterTrim() {
        assertEquals("International", eval("<#if Recipient.country?trim == \"US\">Domestic<#else>International</#if>", mapOf("Recipient.country" to "  UK  ")))
    }

    @Test
    fun testTrimEqualsNoWhitespaceBehavesNormally() {
        assertEquals("Domestic", eval("<#if Recipient.country?trim == \"US\">Domestic<#else>International</#if>", mapOf("Recipient.country" to "US")))
    }

    @Test
    fun testTrimNotEqualsMatchAfterTrim() {
        assertEquals("International", eval("<#if Recipient.country?trim != \"US\">International<#else>Domestic</#if>", mapOf("Recipient.country" to "  UK  ")))
    }

    @Test
    fun testTrimEqualsMissingKeySuppresses() {
        assertSuppresses { eval("<#if Recipient.country?trim == \"US\">Domestic</#if>", emptyMap()) }
    }

    // MARK: - ?trim?number chain

    @Test
    fun testTrimNumberStripsBeforeParsing() {
        assertEquals("Eligible", eval("<#if Recipient.balance?trim?number gt 100>Eligible<#else>Not eligible</#if>", mapOf("Recipient.balance" to "  150  ")))
    }

    @Test
    fun testTrimNumberNoWhitespaceBehavesNormally() {
        assertEquals("Eligible", eval("<#if Recipient.balance?trim?number gte 100>Eligible<#else>Not eligible</#if>", mapOf("Recipient.balance" to "100")))
    }

    @Test
    fun testTrimNumberStripsNewlineBeforeParsing() {
        assertEquals("Eligible", eval("<#if Recipient.balance?trim?number gt 100>Eligible<#else>Not eligible</#if>", mapOf("Recipient.balance" to "\n150\n")))
    }

    @Test
    fun testTrimNumberNonParsableAfterTrimSuppresses() {
        assertSuppresses { eval("<#if Recipient.balance?trim?number gt 100>A</#if>", mapOf("Recipient.balance" to "  not_a_number  ")) }
    }

    // MARK: - ?trim!"default" chain

    @Test
    fun testTrimDefaultKeyPresentTrimsValue() {
        assertEquals("Hello Alice", eval("Hello \${Recipient.name?trim!\"there\"}", mapOf("Recipient.name" to "  Alice  ")))
    }

    @Test
    fun testTrimDefaultKeyMissingSuppresses() {
        assertSuppresses { eval("Hello \${Recipient.name?trim!\"there\"}", emptyMap()) }
    }

    @Test
    fun testTrimDefaultDoesNotTriggerOnEmptyString() {
        assertEquals("", eval("\${Recipient.name?trim!\"fallback\"}", mapOf("Recipient.name" to "")))
    }

    @Test
    fun testTrimDefaultSingleQuotedKeyMissingSuppresses() {
        assertSuppresses { eval("Hello \${Recipient.name?trim!'there'}", emptyMap()) }
    }

    // MARK: - ?lower_case / ?upper_case in ${} interpolation

    @Test
    fun testLowerCaseInterpolation() {
        assertEquals("us", eval("\${Recipient.country?lower_case}", mapOf("Recipient.country" to "US")))
    }

    @Test
    fun testUpperCaseInterpolation() {
        assertEquals("US", eval("\${Recipient.country?upper_case}", mapOf("Recipient.country" to "us")))
    }

    @Test
    fun testLowerCaseWithDefaultKeyMissingSuppresses() {
        assertSuppresses { eval("\${Recipient.country?lower_case!\"unknown\"}", emptyMap()) }
    }

    @Test
    fun testTrimLowerCaseChainInterpolation() {
        assertEquals("active", eval("\${Recipient.status?trim?lower_case}", mapOf("Recipient.status" to "  ACTIVE  ")))
    }

    @Test
    fun testLowerCaseTrimChainInterpolation() {
        assertEquals("active", eval("\${Recipient.status?lower_case?trim}", mapOf("Recipient.status" to "  ACTIVE  ")))
    }

    @Test
    fun testUpperCaseWithDefaultKeyMissingSuppresses() {
        assertSuppresses { eval("\${Recipient.tier?upper_case!\"UNKNOWN\"}", emptyMap()) }
    }

    // MARK: - ?lower_case / ?upper_case in conditions

    @Test
    fun testLowerCaseBooleanChainTrue() {
        assertEquals("yes", eval("<#if Recipient.flag?lower_case?boolean>yes<#else>no</#if>", mapOf("Recipient.flag" to "TRUE")))
    }

    @Test
    fun testLowerCaseBooleanChainFalse() {
        assertEquals("no", eval("<#if Recipient.flag?lower_case?boolean>yes<#else>no</#if>", mapOf("Recipient.flag" to "FALSE")))
    }

    @Test
    fun testUpperCaseBooleanChain() {
        assertEquals("yes", eval("<#if Recipient.flag?upper_case?boolean>yes<#else>no</#if>", mapOf("Recipient.flag" to "true")))
    }

    @Test
    fun testTrimLowerCaseBooleanChain() {
        assertEquals("yes", eval("<#if Recipient.flag?trim?lower_case?boolean>yes<#else>no</#if>", mapOf("Recipient.flag" to "  TRUE  ")))
    }

    @Test
    fun testLowerCaseTrimBooleanChain() {
        assertEquals("yes", eval("<#if Recipient.flag?lower_case?trim?boolean>yes<#else>no</#if>", mapOf("Recipient.flag" to "  TRUE  ")))
    }

    @Test
    fun testLowerCaseHasContentChain() {
        assertEquals("yes", eval("<#if Recipient.name?lower_case?has_content>yes<#else>no</#if>", mapOf("Recipient.name" to "ALICE")))
    }

    @Test
    fun testUpperCaseEqualsChain() {
        assertEquals("yes", eval("<#if Recipient.country?upper_case == \"US\">yes<#else>no</#if>", mapOf("Recipient.country" to "us")))
    }

    @Test
    fun testLowerCaseEqualsChain() {
        assertEquals("Gold", eval("<#if Recipient.tier?lower_case == \"gold\">Gold<#else>Other</#if>", mapOf("Recipient.tier" to "GOLD")))
    }
}
