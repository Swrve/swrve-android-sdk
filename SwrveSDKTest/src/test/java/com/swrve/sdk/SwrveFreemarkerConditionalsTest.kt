package com.swrve.sdk

import org.junit.Assert.assertEquals
import org.junit.Test

class SwrveFreemarkerConditionalsTest : SwrveFreemarkerTestBase() {

    // MARK: - <#if> / <#else> / <#elseif>

    @Test
    fun testIfElseTrueBranch() {
        assertEquals("Gold", eval("<#if Recipient.loyalty_points == \"1200\">Gold<#else>Standard</#if>", mapOf("Recipient.loyalty_points" to "1200")))
    }

    @Test
    fun testIfElseFalseBranch() {
        assertEquals("Standard", eval("<#if Recipient.loyalty_points == \"1200\">Gold<#else>Standard</#if>", mapOf("Recipient.loyalty_points" to "500")))
    }

    @Test
    fun testIfWithoutElse() {
        assertEquals("Shown", eval("<#if Recipient.flag == \"yes\">Shown</#if>", mapOf("Recipient.flag" to "yes")))
    }

    @Test
    fun testIfWithoutElseFalse() {
        assertEquals("", eval("<#if Recipient.flag == \"yes\">Shown</#if>", mapOf("Recipient.flag" to "no")))
    }

    @Test
    fun testIfMissingKeySuppresses() {
        assertSuppresses { eval("<#if Recipient.missing == \"x\">A<#else>B</#if>", emptyMap()) }
    }

    @Test
    fun testNotEquals() {
        assertEquals("International", eval("<#if Recipient.country != \"US\">International<#else>Domestic</#if>", mapOf("Recipient.country" to "UK")))
    }

    @Test
    fun testNotEqualsMissingKeySuppresses() {
        assertSuppresses { eval("<#if Recipient.country != \"US\">International<#else>Domestic</#if>", emptyMap()) }
    }

    @Test
    fun testElseIfFirstBranch() {
        assertEquals(
            "Gold",
            eval("<#if Recipient.loyalty_points?number gte 1000>Gold<#elseif Recipient.loyalty_points?number gte 500>Silver<#else>Bronze</#if>", mapOf("Recipient.loyalty_points" to "1200"))
        )
    }

    @Test
    fun testElseIfSecondBranch() {
        assertEquals(
            "Silver",
            eval("<#if Recipient.loyalty_points?number gte 1000>Gold<#elseif Recipient.loyalty_points?number gte 500>Silver<#else>Bronze</#if>", mapOf("Recipient.loyalty_points" to "700"))
        )
    }

    @Test
    fun testElseIfFallsToElse() {
        assertEquals(
            "Bronze",
            eval("<#if Recipient.loyalty_points?number gte 1000>Gold<#elseif Recipient.loyalty_points?number gte 500>Silver<#else>Bronze</#if>", mapOf("Recipient.loyalty_points" to "100"))
        )
    }

    @Test
    fun testElseIfBoundaryGte() {
        assertEquals(
            "Gold",
            eval("<#if Recipient.loyalty_points?number gte 1000>Gold<#elseif Recipient.loyalty_points?number gte 500>Silver<#else>Bronze</#if>", mapOf("Recipient.loyalty_points" to "1000"))
        )
    }

    // MARK: - ?boolean

    @Test
    fun testBooleanTrue() {
        assertEquals("Subscribed", eval("<#if Recipient.email_opt_in?boolean>Subscribed<#else>Not subscribed</#if>", mapOf("Recipient.email_opt_in" to "true")))
    }

    @Test
    fun testBooleanFalse() {
        assertEquals("Not subscribed", eval("<#if Recipient.email_opt_in?boolean>Subscribed<#else>Not subscribed</#if>", mapOf("Recipient.email_opt_in" to "false")))
    }

    @Test
    fun testBooleanTrueAsNativeType() {
        assertEquals("Subscribed", eval("<#if Recipient.email_opt_in?boolean>Subscribed<#else>Not subscribed</#if>", mapOf("Recipient.email_opt_in" to true)))
    }

    @Test
    fun testBooleanFalseAsNativeType() {
        assertEquals("Not subscribed", eval("<#if Recipient.email_opt_in?boolean>Subscribed<#else>Not subscribed</#if>", mapOf("Recipient.email_opt_in" to false)))
    }

    @Test
    fun testBooleanCaseInsensitive() {
        assertEquals("Subscribed", eval("<#if Recipient.email_opt_in?boolean>Subscribed</#if>", mapOf("Recipient.email_opt_in" to "TRUE")))
    }

    @Test
    fun testBooleanInvalidValueSuppresses() {
        assertSuppresses { eval("<#if Recipient.email_opt_in?boolean>Subscribed</#if>", mapOf("Recipient.email_opt_in" to "yes")) }
    }

    @Test
    fun testBooleanMissingKeySuppresses() {
        assertSuppresses { eval("<#if Recipient.email_opt_in?boolean>Subscribed</#if>", emptyMap()) }
    }

    @Test
    fun testTrimBooleanChainTrue() {
        assertEquals("yes", eval("<#if Recipient.flag?trim?boolean>yes<#else>no</#if>", mapOf("Recipient.flag" to "  true  ")))
    }

    @Test
    fun testTrimBooleanChainFalse() {
        assertEquals("no", eval("<#if Recipient.flag?trim?boolean>yes<#else>no</#if>", mapOf("Recipient.flag" to "  false  ")))
    }

    // MARK: - Negation

    @Test
    fun testNegationFalseBecomesTrue() {
        assertEquals("Not subscribed", eval("<#if !Recipient.email_opt_in?boolean>Not subscribed<#else>Subscribed</#if>", mapOf("Recipient.email_opt_in" to "false")))
    }

    @Test
    fun testNegationTrueBecomesFalse() {
        assertEquals("Subscribed", eval("<#if !Recipient.email_opt_in?boolean>Not subscribed<#else>Subscribed</#if>", mapOf("Recipient.email_opt_in" to "true")))
    }

    @Test
    fun testNegationOfExistsCheck() {
        assertEquals("No name", eval("<#if !Recipient.name??>No name<#else>Has name</#if>", emptyMap()))
    }

    // MARK: - Exists check ??

    @Test
    fun testExistsCheckKeyPresent() {
        assertEquals("Hello Joe", eval("<#if Recipient.firstName??>Hello \${Recipient.firstName}<#else>Hello there</#if>", mapOf("Recipient.firstName" to "Joe")))
    }

    @Test
    fun testExistsCheckKeyMissing() {
        assertEquals("Hello there", eval("<#if Recipient.firstName??>Hello \${Recipient.firstName}<#else>Hello there</#if>", emptyMap()))
    }

    @Test
    fun testExistsWithBuiltinBeforeSuppresses() {
        assertSuppresses { eval("<#if Recipient.name?trim??>yes</#if>", mapOf("Recipient.name" to "Joe")) }
    }

    // MARK: - Nested <#if>

    @Test
    fun testNestedIf() {
        assertEquals(
            "VIP Gold",
            eval("<#if Recipient.tier == \"gold\"><#if Recipient.vip == \"true\">VIP Gold<#else>Gold</#if><#else>Standard</#if>", mapOf("Recipient.tier" to "gold", "Recipient.vip" to "true"))
        )
    }

    @Test
    fun testNestedIfInnerFalseBranch() {
        assertEquals(
            "Gold",
            eval("<#if Recipient.tier == \"gold\"><#if Recipient.vip == \"true\">VIP Gold<#else>Gold</#if><#else>Standard</#if>", mapOf("Recipient.tier" to "gold", "Recipient.vip" to "false"))
        )
    }

    @Test
    fun testNestedIfOuterFalseBranch() {
        assertEquals(
            "Standard",
            eval("<#if Recipient.tier == \"gold\"><#if Recipient.vip == \"true\">VIP Gold<#else>Gold</#if><#else>Standard</#if>", mapOf("Recipient.tier" to "bronze", "Recipient.vip" to "true"))
        )
    }

    // MARK: - Logical operators && / ||

    @Test
    fun testAndBothTrue() {
        assertEquals("yes", eval("<#if Recipient.tier == \"gold\" && Recipient.active == \"true\">yes<#else>no</#if>", mapOf("Recipient.tier" to "gold", "Recipient.active" to "true")))
    }

    @Test
    fun testAndLeftFalse() {
        assertEquals("no", eval("<#if Recipient.tier == \"gold\" && Recipient.active == \"true\">yes<#else>no</#if>", mapOf("Recipient.tier" to "silver", "Recipient.active" to "true")))
    }

    @Test
    fun testAndRightFalse() {
        assertEquals("no", eval("<#if Recipient.tier == \"gold\" && Recipient.active == \"true\">yes<#else>no</#if>", mapOf("Recipient.tier" to "gold", "Recipient.active" to "false")))
    }

    @Test
    fun testOrLeftTrue() {
        assertEquals("premium", eval("<#if Recipient.tier == \"gold\" || Recipient.tier == \"platinum\">premium<#else>standard</#if>", mapOf("Recipient.tier" to "gold")))
    }

    @Test
    fun testOrRightTrue() {
        assertEquals("premium", eval("<#if Recipient.tier == \"gold\" || Recipient.tier == \"platinum\">premium<#else>standard</#if>", mapOf("Recipient.tier" to "platinum")))
    }

    @Test
    fun testOrBothFalse() {
        assertEquals("standard", eval("<#if Recipient.tier == \"gold\" || Recipient.tier == \"platinum\">premium<#else>standard</#if>", mapOf("Recipient.tier" to "bronze")))
    }

    @Test
    fun testAndPrecedenceOverOr() {
        // a || b && c should parse as a || (b && c)
        assertEquals(
            "yes", eval(
                "<#if Recipient.a == \"1\" || Recipient.b == \"1\" && Recipient.c == \"1\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "1", "Recipient.b" to "0", "Recipient.c" to "0")
            )
        )
    }

    @Test
    fun testAndShortCircuitMissingKey() {
        // Left side (exists check) returns false, so right side (which would throw) is not evaluated
        assertEquals("no", eval("<#if Recipient.a?? && Recipient.b == \"x\">yes<#else>no</#if>", emptyMap()))
    }

    @Test
    fun testOrShortCircuitMissingKey() {
        // Left side true, right side has missing key but short-circuit prevents throw
        assertEquals("yes", eval("<#if Recipient.tier == \"gold\" || Recipient.missing == \"x\">yes<#else>no</#if>", mapOf("Recipient.tier" to "gold")))
    }

    @Test
    fun testChainedAnd() {
        assertEquals(
            "yes",
            eval("<#if Recipient.a == \"1\" && Recipient.b == \"1\" && Recipient.c == \"1\">yes<#else>no</#if>", mapOf("Recipient.a" to "1", "Recipient.b" to "1", "Recipient.c" to "1"))
        )
    }

    @Test
    fun testNegationWithAnd() {
        assertEquals("yes", eval("<#if !Recipient.flag?boolean && Recipient.tier == \"gold\">yes<#else>no</#if>", mapOf("Recipient.flag" to "false", "Recipient.tier" to "gold")))
    }

    // MARK: - Newline stripping after directives

    @Test
    fun testIfDirectiveNewlineStripped() {
        assertEquals("Gold\n", eval("<#if Recipient.tier == \"gold\">\nGold\n</#if>", mapOf("Recipient.tier" to "gold")))
    }

    @Test
    fun testEndIfDirectiveNewlineStripped() {
        assertEquals("Gold\nafter", eval("<#if Recipient.tier == \"gold\">\nGold\n</#if>\nafter", mapOf("Recipient.tier" to "gold")))
    }

    @Test
    fun testElseDirectiveNewlineNotStrippedWhenInline() {
        // <#else> is mid-line (after "Gold") — newline is kept, matching FreeMarker behaviour
        assertEquals("\nSilver", eval("<#if Recipient.tier == \"gold\">\nGold<#else>\nSilver</#if>", mapOf("Recipient.tier" to "silver")))
    }

    @Test
    fun testElseIfDirectiveNewlineNotStrippedWhenInline() {
        // <#elseif> is mid-line — newline before the matching branch body is kept
        assertEquals("\nSilver", eval("<#if Recipient.tier == \"gold\">\nGold<#elseif Recipient.tier == \"silver\">\nSilver<#else>\nBronze</#if>", mapOf("Recipient.tier" to "silver")))
    }

    @Test
    fun testNewlineNotStrippedWhenSpacesBefore() {
        assertEquals("  \nGold", eval("<#if Recipient.tier == \"gold\">  \nGold</#if>", mapOf("Recipient.tier" to "gold")))
    }

    // MARK: - ! default in <#if> condition expressions

    @Test
    fun testDefaultInConditionKeyMissing() {
        assertEquals("Not gold", eval("<#if (Recipient.tier!\"bronze\") == \"gold\">Gold<#else>Not gold</#if>", emptyMap()))
    }

    @Test
    fun testDefaultInConditionKeyMissingDefaultMatches() {
        assertEquals("Gold", eval("<#if (Recipient.tier!\"gold\") == \"gold\">Gold<#else>Not gold</#if>", emptyMap()))
    }

    @Test
    fun testDefaultInConditionKeyPresent() {
        assertEquals("Gold", eval("<#if (Recipient.tier!\"bronze\") == \"gold\">Gold<#else>Not gold</#if>", mapOf("Recipient.tier" to "gold")))
    }

    @Test
    fun testDefaultInConditionWithoutParens() {
        assertEquals("Bronze", eval("<#if Recipient.tier!\"bronze\" == \"bronze\">Bronze<#else>Other</#if>", emptyMap()))
    }

    @Test
    fun testDefaultInNotEqualsCondition() {
        assertEquals("Not gold", eval("<#if (Recipient.tier!\"bronze\") != \"gold\">Not gold<#else>Gold</#if>", emptyMap()))
    }

    @Test
    fun testTrimWithDefaultInCondition() {
        assertEquals("Gold", eval("<#if Recipient.tier?trim!\"bronze\" == \"gold\">Gold<#else>Not gold</#if>", mapOf("Recipient.tier" to "  gold  ")))
    }

    @Test
    fun testTrimWithDefaultInConditionKeyMissingSuppresses() {
        assertSuppresses { eval("<#if Recipient.tier?trim!\"bronze\" == \"bronze\">Bronze<#else>Other</#if>", emptyMap()) }
    }

    // MARK: - Bare ! default in <#if> conditions

    @Test
    fun testBareDefaultInConditionKeyMissing() {
        assertEquals("Not gold", eval("<#if Recipient.tier! == \"gold\">Gold<#else>Not gold</#if>", emptyMap()))
    }

    @Test
    fun testBareDefaultInConditionEmptyStringMatchesEmpty() {
        assertEquals("Empty", eval("<#if Recipient.tier! == \"\">Empty</#if>", emptyMap()))
    }

    @Test
    fun testBareDefaultInConditionKeyPresent() {
        assertEquals("Gold", eval("<#if Recipient.tier! == \"gold\">Gold<#else>Not gold</#if>", mapOf("Recipient.tier" to "gold")))
    }

    @Test
    fun testBareDefaultNotEqualsKeyMissing() {
        assertEquals("Not gold", eval("<#if Recipient.tier! != \"gold\">Not gold</#if>", emptyMap()))
    }

    @Test
    fun testBareDefaultWithTrimKeyMissingSuppresses() {
        assertSuppresses { eval("<#if Recipient.tier?trim! == \"gold\">Gold<#else>Not gold</#if>", emptyMap()) }
    }

    // MARK: - > inside quoted string literals

    @Test
    fun testGreaterThanInsideStringLiteralInCondition() {
        assertEquals("yes", eval("<#if Recipient.label == \"a>b\">yes<#else>no</#if>", mapOf("Recipient.label" to "a>b")))
    }

    @Test
    fun testGreaterThanInsideStringLiteralNoMatch() {
        assertEquals("no", eval("<#if Recipient.label == \"a>b\">yes<#else>no</#if>", mapOf("Recipient.label" to "other")))
    }

    // MARK: - Mixed content

    // MARK: - Quoted LHS rejected in == / !=

    @Test
    fun testQuotedLHSInEqualitySuppresses() {
        assertSuppressesWithMessage("left-hand side") {
            eval("<#if \"gold\" == Recipient.tier>yes</#if>", mapOf("Recipient.tier" to "gold"))
        }
    }

    // MARK: - Paren-aware operator search

    @Test
    fun testParenWrappedVariableInNotEquals() {
        assertEquals("gold", eval("<#if (Recipient.tier) != \"gold\">other<#else>gold</#if>", mapOf("Recipient.tier" to "gold")))
    }

    @Test
    fun testEqualityInsideParensDoesNotConfuseOuterNotEquals() {
        // Without paren-awareness findOperator("==") finds == inside the parens → confusing parse error.
        // With fix the outer != is found → eval-time error instead.
        assertSuppresses {
            eval("<#if (Recipient.a == \"b\") != \"c\">yes</#if>", mapOf("Recipient.a" to "b"))
        }
    }

    // MARK: - Quote-aware numeric operator search

    @Test
    fun testOperatorInsideQuotedDefaultNotMisParsed() {
        // " gt " inside the string literal must not be treated as a numeric comparison operator.
        // Before fix: indexOf(" gt ") splits inside the literal → throws "Numeric comparison requires ?number".
        // After fix: indexOutsideQuotes skips the quoted span → parseNumericCompare returns null → parseBooleanBuiltin handles it.
        assertEquals("yes", eval("<#if (Recipient.flag!\"a gt b\")?boolean>yes<#else>no</#if>", mapOf("Recipient.flag" to "true")))
    }

    @Test
    fun testOperatorInsideQuotedDefaultDefaultPathNotMisParsed() {
        // Same but key is absent — default "a gt b" is used, which is not a valid boolean → suppresses.
        assertSuppresses {
            eval("<#if (Recipient.flag!\"a gt b\")?boolean>yes<#else>no</#if>", emptyMap())
        }
    }

    // MARK: - Terminal built-ins rejected in == / != LHS

    @Test
    fun testBooleanBuiltinInEqualityLHSSuppresses() {
        assertSuppressesWithMessage("?boolean") {
            eval("<#if Recipient.flag?boolean == \"true\">yes</#if>", mapOf("Recipient.flag" to "true"))
        }
    }

    @Test
    fun testNumberBuiltinInEqualityLHSSuppresses() {
        assertSuppressesWithMessage("?number") {
            eval("<#if Recipient.points?number == \"10\">yes</#if>", mapOf("Recipient.points" to "10"))
        }
    }

    @Test
    fun testHasContentBuiltinInEqualityLHSSuppresses() {
        assertSuppressesWithMessage("?has_content") {
            eval("<#if Recipient.name?has_content == \"true\">yes</#if>", mapOf("Recipient.name" to "Alice"))
        }
    }

    @Test
    fun testIfWithInterpolationInBody() {
        assertEquals(
            "Welcome Alice, Gold member!", eval(
                "<#if Recipient.tier == \"gold\">Welcome \${Recipient.name}, Gold member!<#else>Welcome \${Recipient.name}.</#if>",
                mapOf("Recipient.tier" to "gold", "Recipient.name" to "Alice")
            )
        )
    }

    // MARK: - String literal interior quote validation

    @Test
    fun testStringLiteralInteriorDoubleQuoteSuppresses() {
        assertSuppresses {
            eval("<#if Recipient.x == \"y\" != \"z\">ok</#if>", mapOf("Recipient.x" to "y"))
        }
    }

    @Test
    fun testStringLiteralInteriorSingleQuoteSuppresses() {
        assertSuppresses {
            eval("<#if Recipient.x == 'a' 'b'>ok</#if>", mapOf("Recipient.x" to "a"))
        }
    }

    @Test
    fun testStringLiteralValidDoubleQuotedAccepted() {
        assertEquals("ok", eval("<#if Recipient.x == \"hello\">ok</#if>", mapOf("Recipient.x" to "hello")))
    }

    @Test
    fun testStringLiteralValidSingleQuotedAccepted() {
        assertEquals("ok", eval("<#if Recipient.x == 'hello'>ok</#if>", mapOf("Recipient.x" to "hello")))
    }
}
