package com.swrve.sdk

import org.junit.Assert.assertEquals
import org.junit.Test

class SwrveFreemarkerGroupingConcatTest : SwrveFreemarkerTestBase() {

    // MARK: - Parenthesis grouping

    @Test
    fun testParenGroupingStripsOuterParens() {
        assertEquals("Gold", eval("<#if (Recipient.tier == \"gold\")>Gold</#if>", mapOf("Recipient.tier" to "gold")))
    }

    @Test
    fun testNestedParensStripped() {
        assertEquals("Gold", eval("<#if ((Recipient.tier == \"gold\"))>Gold</#if>", mapOf("Recipient.tier" to "gold")))
    }

    @Test
    fun testParenGroupingChangesOperatorPrecedence() {
        val template = "<#if (Recipient.loyalty_points?number gt 1000 || Recipient.account_status == \"active\") && Recipient.region == \"US\">Match</#if>"
        assertEquals("", eval(template, mapOf("Recipient.loyalty_points" to "1200", "Recipient.account_status" to "inactive", "Recipient.region" to "CA")))
        assertEquals("Match", eval(template, mapOf("Recipient.loyalty_points" to "1200", "Recipient.account_status" to "inactive", "Recipient.region" to "US")))
    }

    @Test
    fun testParenGroupingOrBeforeAnd() {
        val withGrouping = "<#if (Recipient.x == \"a\" || Recipient.x == \"b\") && Recipient.y == \"yes\">Hit</#if>"
        assertEquals("", eval(withGrouping, mapOf("Recipient.x" to "a", "Recipient.y" to "no")))
        val withoutGrouping = "<#if Recipient.x == \"a\" || Recipient.x == \"b\" && Recipient.y == \"yes\">Hit</#if>"
        assertEquals("Hit", eval(withoutGrouping, mapOf("Recipient.x" to "a", "Recipient.y" to "no")))
    }

    // MARK: - String concatenation

    @Test
    fun testConcatLiteralsAndVariables() {
        assertEquals(
            "Hi John, you have 1200 points", eval(
                "\${\"Hi \" + Recipient.first_name + \", you have \" + Recipient.loyalty_points + \" points\"}",
                mapOf("Recipient.first_name" to "John", "Recipient.loyalty_points" to "1200")
            )
        )
    }

    @Test
    fun testConcatLiteralOnly() {
        assertEquals("Hello World", eval("\${\"Hello\" + \" World\"}", emptyMap()))
    }

    @Test
    fun testConcatMissingKeySuppresses() {
        assertSuppressesWithMessage("Recipient.missing") {
            eval("\${\"Hello \" + Recipient.missing}", emptyMap())
        }
    }

    @Test
    fun testConcatTrimLeadingPart() {
        assertEquals("Alice world", eval("\${Recipient.first_name?trim + \" world\"}", mapOf("Recipient.first_name" to "  Alice  ")))
    }

    @Test
    fun testConcatTrimMiddlePart() {
        assertEquals("Hello Bob!", eval("\${\"Hello \" + Recipient.first_name?trim + \"!\"}", mapOf("Recipient.first_name" to "  Bob  ")))
    }

    @Test
    fun testConcatTrimPreservesOtherWhitespace() {
        assertEquals("prefix: Alice Smith", eval("\${\"prefix: \" + Recipient.first_name?trim + Recipient.last_name}", mapOf("Recipient.first_name" to "  Alice  ", "Recipient.last_name" to " Smith")))
    }

    @Test
    fun testConcatTrimTrailingPart() {
        assertEquals("prefix: Carol", eval("\${\"prefix: \" + Recipient.first_name?trim}", mapOf("Recipient.first_name" to "  Carol  ")))
    }

    @Test
    fun testConcatTrimMissingKeySuppresses() {
        assertSuppressesWithMessage("Recipient.missing") {
            eval("\${\"Hello \" + Recipient.missing?trim}", emptyMap())
        }
    }

    @Test
    fun testConcatUpperCaseTransform() {
        assertEquals("HELLO world", eval("\${Recipient.x?upper_case + \" world\"}", mapOf("Recipient.x" to "hello")))
    }

    @Test
    fun testConcatLowerCaseTransform() {
        assertEquals("Prefix: alice", eval("\${\"Prefix: \" + Recipient.name?lower_case}", mapOf("Recipient.name" to "ALICE")))
    }

    @Test
    fun testConcatDefaultOperatorSuppresses() {
        assertSuppressesWithMessage("'!'") {
            eval("\${\"Hello \" + Recipient.name!\"fallback\"}", emptyMap())
        }
    }

    @Test
    fun testConcatUnsupportedBuiltinSuppresses() {
        assertSuppressesWithMessage("Only ?trim, ?lower_case, and ?upper_case are supported") {
            eval("\${Recipient.x?boolean + \" world\"}", mapOf("Recipient.x" to "true"))
        }
    }
}
