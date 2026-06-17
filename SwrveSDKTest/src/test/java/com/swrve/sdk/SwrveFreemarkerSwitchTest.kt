package com.swrve.sdk

import org.junit.Assert.assertEquals
import org.junit.Test

class SwrveFreemarkerSwitchTest : SwrveFreemarkerTestBase() {

    @Test
    fun testSwitchMatchesFirstCase() {
        assertEquals(
            "US message",
            eval("<#switch Recipient.country><#case \"US\">US message<#break><#case \"UK\">UK message<#break><#default>Default message</#switch>", mapOf("Recipient.country" to "US"))
        )
    }

    @Test
    fun testSwitchMatchesMiddleCase() {
        assertEquals(
            "UK message",
            eval("<#switch Recipient.country><#case \"US\">US message<#break><#case \"UK\">UK message<#break><#default>Default message</#switch>", mapOf("Recipient.country" to "UK"))
        )
    }

    @Test
    fun testSwitchNoMatchFallsToDefault() {
        assertEquals(
            "Default message",
            eval("<#switch Recipient.country><#case \"US\">US message<#break><#case \"UK\">UK message<#break><#default>Default message</#switch>", mapOf("Recipient.country" to "CA"))
        )
    }

    @Test
    fun testSwitchMissingVariableSuppresses() {
        assertSuppressesWithMessage("Recipient.country") {
            eval("<#switch Recipient.country><#case \"US\">US message<#break><#default>Default message</#switch>", emptyMap())
        }
    }

    @Test
    fun testSwitchNoCasesReturnsEmpty() {
        assertEquals("", eval("<#switch Recipient.country></#switch>", mapOf("Recipient.country" to "US")))
    }

    @Test
    fun testSwitchNoMatchNoDefaultReturnsEmpty() {
        assertEquals("", eval("<#switch Recipient.country><#case \"US\">US message<#break></#switch>", mapOf("Recipient.country" to "CA")))
    }

    @Test
    fun testSwitchFallThroughWithoutBreak() {
        assertEquals("AB", eval("<#switch Recipient.x><#case \"a\">A<#case \"b\">B<#break><#default>D</#switch>", mapOf("Recipient.x" to "a")))
    }

    @Test
    fun testSwitchFallThroughIntoDefault() {
        assertEquals("AD", eval("<#switch Recipient.x><#case \"a\">A<#default>D</#switch>", mapOf("Recipient.x" to "a")))
    }

    @Test
    fun testSwitchUnclosedSuppresses() {
        assertSuppresses { eval("<#switch Recipient.x><#case \"a\">text", mapOf("Recipient.x" to "a")) }
    }

    @Test
    fun testSwitchDuplicateDefaultSuppresses() {
        assertSuppresses { eval("<#switch Recipient.x><#default>D1<#default>D2</#switch>", emptyMap()) }
    }

    @Test
    fun testSwitchCaseAfterDefaultSuppresses() {
        assertSuppresses { eval("<#switch Recipient.x><#default>D<#case \"a\">A<#break></#switch>", mapOf("Recipient.x" to "a")) }
    }

    @Test
    fun testSwitchNonWhitespaceTextBetweenDirectivesSuppresses() {
        assertSuppresses { eval("<#switch Recipient.x>oops<#case \"a\">A<#break></#switch>", mapOf("Recipient.x" to "a")) }
    }

    @Test
    fun testSwitchBreakInsideNestedIfSuppresses() {
        assertSuppressesWithMessage("<#break>") {
            eval("<#switch Recipient.x><#case \"a\"><#if Recipient.y == \"b\">text<#break></#if></#switch>", mapOf("Recipient.x" to "a", "Recipient.y" to "b"))
        }
    }

    @Test
    fun testSwitchWhitespaceBetweenDirectivesAllowed() {
        assertEquals("A", eval("<#switch Recipient.x>\n  <#case \"a\">A<#break>\n</#switch>", mapOf("Recipient.x" to "a")))
    }

    @Test
    fun testSwitchEndTagNewlineNotStrippedWhenInline() {
        // </#switch> is mid-line (after "Other"), so trailing \n is kept — matching FreeMarker behaviour
        assertEquals("Gold\n", eval("<#switch Recipient.tier>\n<#case \"gold\">\nGold<#break>\n<#default>\nOther</#switch>\n", mapOf("Recipient.tier" to "gold")))
    }

    @Test
    fun testDefaultDirectiveNewlineStripped() {
        assertEquals("Other", eval("<#switch Recipient.tier>\n<#case \"gold\">\nGold<#break>\n<#default>\nOther</#switch>", mapOf("Recipient.tier" to "bronze")))
    }

    // MARK: - Unsupported syntax in switch expression

    @Test
    fun testSwitchDefaultOperatorKeyAbsentMatchesCase() {
        val result = eval("<#switch Recipient.tier!\"free\"><#case \"free\">Free<#break><#case \"pro\">Pro<#break></#switch>", emptyMap())
        assertEquals("Free", result)
    }

    // MARK: - String transforms on switch expression

    @Test
    fun testSwitchUpperCaseTransformMatches() {
        assertEquals("United States", eval("<#switch Recipient.country?upper_case><#case \"US\">United States<#break><#default>Other</#switch>", mapOf("Recipient.country" to "us")))
    }

    @Test
    fun testSwitchLowerCaseTransformMatches() {
        assertEquals("Gold", eval("<#switch Recipient.tier?lower_case><#case \"gold\">Gold<#break><#default>Other</#switch>", mapOf("Recipient.tier" to "GOLD")))
    }

    @Test
    fun testSwitchTrimLowerCaseChainMatches() {
        assertEquals("Gold", eval("<#switch Recipient.tier?trim?lower_case><#case \"gold\">Gold<#break><#default>Other</#switch>", mapOf("Recipient.tier" to "  GOLD  ")))
    }
}
