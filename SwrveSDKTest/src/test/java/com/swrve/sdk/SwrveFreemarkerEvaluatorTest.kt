package com.swrve.sdk

import org.junit.Assert.assertEquals
import org.junit.Test

class SwrveFreemarkerEvaluatorTest : SwrveFreemarkerTestBase() {

    // MARK: - ${Recipient.x} interpolation

    @Test
    fun testSimpleInterpolation() {
        assertEquals("Hello Joe!", eval("Hello \${Recipient.firstName}!", mapOf("Recipient.firstName" to "Joe")))
    }

    @Test
    fun testInterpolationMissingKeySuppresses() {
        assertSuppresses { eval("Hello \${Recipient.firstName}", emptyMap()) }
    }

    // MARK: - Default value operator !

    @Test
    fun testDefaultValueKeyPresent() {
        assertEquals("Hello Joe", eval("""Hello ${'$'}{Recipient.firstName!"there"}""", mapOf("Recipient.firstName" to "Joe")))
    }

    @Test
    fun testDefaultValueKeyMissing() {
        assertEquals("Hello there", eval("""Hello ${'$'}{Recipient.firstName!"there"}""", emptyMap()))
    }

    @Test
    fun testDefaultValueDoesNotTriggerOnEmptyString() {
        assertEquals("", eval("""${'$'}{Recipient.name!"fallback"}""", mapOf("Recipient.name" to "")))
    }

    @Test
    fun testDefaultValueNonStringLiteralSuppresses() {
        assertSuppresses { eval("\${Recipient.count!0}", emptyMap()) }
    }

    @Test
    fun testDefaultValueSingleQuotedKeyMissing() {
        assertEquals("Hello there", eval("Hello \${Recipient.firstName!'there'}", emptyMap()))
    }

    @Test
    fun testDefaultValueSingleQuotedKeyPresent() {
        assertEquals("Hello Joe", eval("Hello \${Recipient.firstName!'there'}", mapOf("Recipient.firstName" to "Joe")))
    }

    // MARK: - Numeric comparisons

    @Test
    fun testNumericGt() {
        assertEquals("Eligible", eval("<#if Recipient.balance?number gt 100>Eligible<#else>Not eligible</#if>", mapOf("Recipient.balance" to "150")))
    }

    @Test
    fun testNumericGte() {
        assertEquals("Eligible", eval("<#if Recipient.balance?number gte 100>Eligible<#else>Not eligible</#if>", mapOf("Recipient.balance" to "100")))
    }

    @Test
    fun testNumericLt() {
        assertEquals("Low", eval("<#if Recipient.balance?number lt 100>Low<#else>OK</#if>", mapOf("Recipient.balance" to "50")))
    }

    @Test
    fun testNumericLte() {
        assertEquals("C", eval("<#if Recipient.balance?number lte 100>C</#if>", mapOf("Recipient.balance" to "100")))
    }

    @Test
    fun testNumericGtBoundaryExcludes() {
        assertEquals("", eval("<#if Recipient.balance?number gt 100>A</#if>", mapOf("Recipient.balance" to "100")))
    }

    @Test
    fun testNumericLtBoundaryExcludes() {
        assertEquals("", eval("<#if Recipient.balance?number lt 100>B</#if>", mapOf("Recipient.balance" to "100")))
    }

    @Test
    fun testNumericNonParsableSuppresses() {
        assertSuppresses { eval("<#if Recipient.balance?number gt 100>A</#if>", mapOf("Recipient.balance" to "not_a_number")) }
    }

    @Test
    fun testNumericMissingKeySuppresses() {
        assertSuppresses { eval("<#if Recipient.balance?number gt 100>A</#if>", emptyMap()) }
    }

    @Test
    fun testNumericFloat() {
        assertEquals("High", eval("<#if Recipient.score?number gt 3.0>High<#else>Low</#if>", mapOf("Recipient.score" to "3.14")))
    }

    // MARK: - Multiple interpolations

    @Test
    fun testMultipleInterpolations() {
        assertEquals("Joe Smith", eval("\${Recipient.firstName} \${Recipient.lastName}", mapOf("Recipient.firstName" to "Joe", "Recipient.lastName" to "Smith")))
    }

    @Test
    fun testMultipleInterpolationsOneMissingSuppresses() {
        assertSuppresses { eval("\${Recipient.firstName} \${Recipient.lastName}", mapOf("Recipient.firstName" to "Joe")) }
    }

    // MARK: - Whitespace inside tags

    @Test
    fun testWhitespaceInsideIfTag() {
        assertEquals("Shown", eval("<#if  Recipient.flag == \"yes\" >Shown</#if>", mapOf("Recipient.flag" to "yes")))
    }

    @Test
    fun testWhitespaceInsideEndIfTag() {
        assertEquals("Shown", eval("<#if Recipient.flag == \"yes\">Shown</# if >", mapOf("Recipient.flag" to "yes")))
    }

    // MARK: - Plain text pass-through

    @Test
    fun testPlainTextPassThrough() {
        assertEquals("Hello World", eval("Hello World"))
    }

    @Test
    fun testEmptyTemplate() {
        assertEquals("", eval(""))
    }

    @Test
    fun testNilTemplateSuppresses() {
        assertSuppresses { SwrveFreemarkerEvaluator.evaluate(null, emptyMap()) }
    }

    // MARK: - Non-string property values

    @Test
    fun testNumberPropertyCoercedToString() {
        assertEquals("Points: 1200", eval("Points: \${Recipient.points}", mapOf("Recipient.points" to 1200)))
    }

    @Test
    fun testNonCoerciblePropertyValueIgnored() {
        assertSuppresses { eval("\${Recipient.data}", mapOf("Recipient.data" to listOf<String>())) }
    }

    // MARK: - Error handling

    @Test
    fun testOrphanedElseIfSuppresses() {
        assertSuppresses { eval("<#elseif Recipient.x == \"y\">body", emptyMap()) }
    }

    @Test
    fun testOrphanedElseSuppresses() {
        assertSuppresses { eval("<#else>body", emptyMap()) }
    }

    @Test
    fun testOrphanedEndIfSuppresses() {
        assertSuppresses { eval("some text</#if>", emptyMap()) }
    }

    @Test
    fun testUnclosedIfTagSuppresses() {
        assertSuppresses { eval("<#if unclosed", emptyMap()) }
    }

    @Test
    fun testUnclosedIfBodySuppresses() {
        assertSuppresses { eval("<#if Recipient.x == \"y\">body with no end", mapOf("Recipient.x" to "y")) }
    }

    @Test
    fun testUnclosedInterpolationSuppresses() {
        assertSuppresses { eval("Hello \${Recipient.name", mapOf("Recipient.name" to "Joe")) }
    }

    @Test
    fun testDefaultValueContainingClosingBrace() {
        assertEquals("New York}World", eval("\${Recipient.city!\"New York}World\"}", emptyMap()))
    }

    @Test
    fun testDefaultValueContainingClosingBraceSingleQuote() {
        assertEquals("a}b", eval("\${Recipient.city!'a}b'}", emptyMap()))
    }

    @Test
    fun testInterpolationAfterDefaultWithBrace() {
        assertEquals("New York}World is great", eval("\${Recipient.city!\"New York}World\"} is great", emptyMap()))
    }

    @Test
    fun testUnsupportedDirectiveSuppresses() {
        assertSuppresses { eval("<#list items as item>\${item}</#list>", emptyMap()) }
    }

    // MARK: - Regression: equality condition with comparison keyword in string literal

    @Test
    fun testEqualityWithGtInStringLiteralNotMisinterpretedAsNumericCompare() {
        // "a gt b" contains " gt " — must be treated as a string literal, not a numeric operator
        assertEquals("yes", eval("<#if Recipient.label == \"a gt b\">yes<#else>no</#if>", mapOf("Recipient.label" to "a gt b")))
    }

    @Test
    fun testEqualityWithLtInStringLiteralNotMisinterpretedAsNumericCompare() {
        assertEquals("yes", eval("<#if Recipient.label == \"a lt b\">yes<#else>no</#if>", mapOf("Recipient.label" to "a lt b")))
    }

    @Test
    fun testEqualityWithGteInStringLiteralNotMisinterpretedAsNumericCompare() {
        assertEquals("yes", eval("<#if Recipient.label == \"a gte b\">yes<#else>no</#if>", mapOf("Recipient.label" to "a gte b")))
    }

    // MARK: - Interpolation input validation

    @Test
    fun testLeadingBangInInterpolationSuppresses() {
        assertSuppressesWithMessage("'!'") {
            eval("\${!Recipient.name}", mapOf("Recipient.name" to "Alice"))
        }
    }

    @Test
    fun testBareSpaceConcatWorks() {
        val result = eval("\${Recipient.first+Recipient.last}", mapOf("Recipient.first" to "John", "Recipient.last" to "Doe"))
        assertEquals("JohnDoe", result)
    }

    // MARK: - NaN / Infinity as RHS literal

    @Test
    fun testNumericNaNAsRHSLiteralSuppresses() {
        assertSuppressesWithMessage("finite") {
            eval("<#if Recipient.score?number == NaN>bad</#if>", mapOf("Recipient.score" to "42"))
        }
    }

    @Test
    fun testNumericInfinityAsRHSLiteralSuppresses() {
        assertSuppressesWithMessage("finite") {
            eval("<#if Recipient.score?number gt Infinity>bad</#if>", mapOf("Recipient.score" to "42"))
        }
    }

    // MARK: - NaN / Infinity guards

    @Test
    fun testNumericNaNSuppresses() {
        assertSuppressesWithMessage("finite") {
            eval("<#if Recipient.score?number == 0>yes</#if>", mapOf("Recipient.score" to "NaN"))
        }
    }

    @Test
    fun testNumericInfinitySuppresses() {
        assertSuppressesWithMessage("finite") {
            eval("<#if Recipient.score?number gt 0>yes</#if>", mapOf("Recipient.score" to "Infinity"))
        }
    }

    // MARK: - Numeric equality

    @Test
    fun testNumericEqualsMatch() {
        assertEquals("yes", eval("<#if Recipient.points?number == 10>yes<#else>no</#if>", mapOf("Recipient.points" to "10")))
    }

    @Test
    fun testNumericEqualsNoMatch() {
        assertEquals("no", eval("<#if Recipient.points?number == 10>yes<#else>no</#if>", mapOf("Recipient.points" to "5")))
    }

    @Test
    fun testNumericNotEqualsMatch() {
        assertEquals("yes", eval("<#if Recipient.points?number != 0>yes<#else>no</#if>", mapOf("Recipient.points" to "10")))
    }

    @Test
    fun testNumericEqualsFloat() {
        assertEquals("yes", eval("<#if Recipient.score?number == 9.5>yes<#else>no</#if>", mapOf("Recipient.score" to "9.5")))
    }

    @Test
    fun testNumericEqualsWithTrimTransform() {
        assertEquals("yes", eval("<#if Recipient.points?trim?number == 10>yes<#else>no</#if>", mapOf("Recipient.points" to "  10  ")))
    }

    @Test
    fun testNumericEqualsWithQuotedRHSSuppresses() {
        assertSuppresses { eval("<#if Recipient.points?number == \"10\">yes</#if>", mapOf("Recipient.points" to "10")) }
    }

    @Test
    fun testBareParenNumericEqualsSuppresses() {
        // Paren-containing key on ?number == path — must suppress (parity with gt/lt path)
        assertSuppressesWithMessage("parentheses") { eval("<#if (Recipient.x)?number == 10>yes</#if>", mapOf("Recipient.x" to "10")) }
    }

    @Test
    fun testBareParenNumericNotEqualsSuppresses() {
        assertSuppressesWithMessage("parentheses") { eval("<#if (Recipient.x)?number != 10>yes</#if>", mapOf("Recipient.x" to "10")) }
    }

    // MARK: - String literal parser edge cases

    @Test
    fun testEmptyStringLiteralIsValid() {
        assertEquals("", eval("\${Recipient.name!\"\"}", emptyMap()))
    }

    @Test
    fun testBareDefaultOperatorKeyMissingReturnsEmpty() {
        assertEquals("", eval("\${Recipient.name!}", emptyMap()))
    }

    @Test
    fun testBareDefaultOperatorWithTrimKeyMissingSuppresses() {
        assertSuppresses { eval("\${Recipient.name?trim!}", emptyMap()) }
    }

    @Test
    fun testBareDefaultOperatorKeyPresentReturnsValue() {
        assertEquals("Alice", eval("\${Recipient.name!}", mapOf("Recipient.name" to "Alice")))
    }

    @Test
    fun testBareQuoteCharacterAsDefaultSuppresses() {
        assertSuppresses {
            eval("\${Recipient.name!\"}", emptyMap())
        }
    }

    // MARK: - Parenthesised default in conditions: (key!"default")?builtin

    @Test
    fun testParenthesizedDefaultBoolean_keyPresent() {
        val result = eval(
            "<#if (Recipient.is_premium!\"false\")?boolean>yes<#else>no</#if>",
            mapOf("Recipient.is_premium" to "true")
        )
        assertEquals("yes", result)
    }

    @Test
    fun testParenthesizedDefaultBoolean_keyAbsent_usesDefault() {
        val result = eval(
            "<#if (Recipient.is_premium!\"false\")?boolean>yes<#else>no</#if>",
            emptyMap()
        )
        assertEquals("no", result)
    }

    @Test
    fun testParenthesizedDefaultNumeric_keyPresent() {
        val result = eval(
            "<#if (Recipient.points!\"0\")?number gte 100>vip<#else>basic</#if>",
            mapOf("Recipient.points" to "500")
        )
        assertEquals("vip", result)
    }

    @Test
    fun testParenthesizedDefaultNumeric_keyAbsent_usesDefault() {
        val result = eval(
            "<#if (Recipient.points!\"0\")?number gte 100>vip<#else>basic</#if>",
            emptyMap()
        )
        assertEquals("basic", result)
    }

    @Test
    fun testParenthesizedDefaultHasContent_keyPresent() {
        val result = eval(
            "<#if (Recipient.promo!\"\")?has_content>show<#else>hide</#if>",
            mapOf("Recipient.promo" to "SAVE20")
        )
        assertEquals("show", result)
    }

    @Test
    fun testParenthesizedDefaultHasContent_keyAbsent_usesEmptyDefault() {
        val result = eval(
            "<#if (Recipient.promo!\"\")?has_content>show<#else>hide</#if>",
            emptyMap()
        )
        assertEquals("hide", result)
    }

    // MARK: - Switch with !"default"

    @Test
    fun testSwitchWithDefault_keyPresent_matchesCase() {
        val result = eval(
            "<#switch Recipient.tier!\"bronze\"><#case \"gold\">Gold<#break><#case \"silver\">Silver<#break><#default>Bronze</#switch>",
            mapOf("Recipient.tier" to "silver")
        )
        assertEquals("Silver", result)
    }

    @Test
    fun testSwitchWithDefault_keyAbsent_usesDefault() {
        val result = eval(
            "<#switch Recipient.tier!\"bronze\"><#case \"gold\">Gold<#break><#case \"silver\">Silver<#break><#default>Bronze</#switch>",
            emptyMap()
        )
        assertEquals("Bronze", result)
    }

    // MARK: - Concatenation without spaces around "+"

    @Test
    fun testBareConcat_noSpaces() {
        val result = eval(
            "\${Recipient.first+\" \"+Recipient.last}",
            mapOf("Recipient.first" to "John", "Recipient.last" to "Doe")
        )
        assertEquals("John Doe", result)
    }

    @Test
    fun testBareConcat_mixedSpacing() {
        val result = eval(
            "\${Recipient.first +\" \"+ Recipient.last}",
            mapOf("Recipient.first" to "John", "Recipient.last" to "Doe")
        )
        assertEquals("John Doe", result)
    }

    // MARK: - Parenthesized bare variable suppresses (validateBareKey)

    @Test
    fun testBareParenInterpolationSuppresses() {
        // ${(Recipient.name)} — parens around bare key; no legitimate paren form; should suppress
        assertSuppressesWithMessage("parentheses") { eval("\${(Recipient.name)}", mapOf("Recipient.name" to "Alice")) }
    }

    @Test
    fun testBareParenDefaultInterpolationSuppresses() {
        // ${(Recipient.name)!"friend"} — bang outside parens, not the (key!"default") form; should suppress
        assertSuppressesWithMessage("parentheses") { eval("\${(Recipient.name)!\"friend\"}", mapOf("Recipient.name" to "Alice")) }
    }

    @Test
    fun testBareParenExistsCheckSuppresses() {
        // <#if (Recipient.name)??>  — parens around key in ?? check; should suppress
        assertSuppressesWithMessage("parentheses") { eval("<#if (Recipient.name)??>yes</#if>", mapOf("Recipient.name" to "Alice")) }
    }

    @Test
    fun testBareParenSwitchExpressionSuppresses() {
        // <#switch (Recipient.tier)> — paren-containing key in switch expression; should suppress
        assertSuppressesWithMessage("parentheses") {
            eval("<#switch (Recipient.tier)><#case \"gold\">Gold<#break><#default>Other</#switch>", mapOf("Recipient.tier" to "gold"))
        }
    }

    @Test
    fun testLegitimateParenDefaultStillWorks() {
        // ${(Recipient.name!"friend")} — canonical (key!"default") form; key absent → "friend"
        val result = eval("\${(Recipient.name!\"friend\")}", emptyMap())
        assertEquals("friend", result)
    }

    @Test
    fun testLegitimateParenDefaultKeyPresentStillWorks() {
        // ${(Recipient.name!"friend")} — canonical form; key present → key value
        val result = eval("\${(Recipient.name!\"friend\")}", mapOf("Recipient.name" to "Alice"))
        assertEquals("Alice", result)
    }

    @Test
    fun testParenInsideQuotedDefaultNotSuppressed() {
        // Parens are in the default *string*, not the key — validateBareKey must not fire
        val result = eval("\${Recipient.terms!\"(see terms)\"}", emptyMap())
        assertEquals("(see terms)", result)
    }

    @Test
    fun testParenInsideQuotedDefaultKeyPresentNotSuppressed() {
        val result = eval("\${Recipient.terms!\"(see terms)\"}", mapOf("Recipient.terms" to "ok"))
        assertEquals("ok", result)
    }

    @Test
    fun testParenInsideConditionDefaultNotSuppressed() {
        // Default value containing parens used in a condition — must not suppress
        val result = eval("<#if Recipient.label!\"(n/a)\" == \"(n/a)\">yes</#if>", emptyMap())
        assertEquals("yes", result)
    }
}
