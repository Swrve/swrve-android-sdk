package com.swrve.sdk

import freemarker.template.Configuration
import freemarker.template.Template
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter

/**
 * Conformance tests: run identical templates through both SwrveFreemarkerEvaluator and the real
 * FreeMarker library (used by Accelerator) and assert they produce the same output.
 *
 * Tests that are KNOWN to diverge are marked @Ignore with a comment explaining why.
 * Those divergences are intentional SDK simplifications or unsupported edge cases.
 *
 * All property values are passed as strings (matching the SDK's flat Map<String,String> design),
 * mapped to a nested FreeMarker data model via dot-notation splitting:
 *   "Recipient.firstName" -> "Joe"  →  { Recipient: { firstName: "Joe" } }
 */
class SwrveFreemarkerConformanceTest {

    data class ConformanceCase(
        val name: String,
        val template: String,
        val props: Map<String, String> = emptyMap(),
        val expected: String
    )

    companion object {
        val TEST_CASES: List<ConformanceCase> = listOf(
            // Interpolation
            ConformanceCase(
                "testSimpleInterpolation",
                "Hello \${Recipient.firstName}!",
                mapOf("Recipient.firstName" to "Joe"),
                "Hello Joe!"
            ),
            ConformanceCase(
                "testMultipleInterpolations",
                "\${Recipient.firstName} \${Recipient.lastName}",
                mapOf("Recipient.firstName" to "Joe", "Recipient.lastName" to "Smith"),
                "Joe Smith"
            ),
            // Default value operator  !
            ConformanceCase(
                "testDefaultValueKeyPresent",
                """Hello ${'$'}{Recipient.firstName!"there"}""",
                mapOf("Recipient.firstName" to "Joe"),
                "Hello Joe"
            ),
            ConformanceCase(
                "testDefaultValueKeyMissing",
                """Hello ${'$'}{Recipient.firstName!"there"}""",
                emptyMap(),
                "Hello there"
            ),
            ConformanceCase(
                "testDefaultValueDoesNotTriggerOnEmptyString",
                """${'$'}{Recipient.name!"fallback"}""",
                mapOf("Recipient.name" to ""),
                ""
            ),
            ConformanceCase(
                "testDefaultValueSingleQuotedKeyMissing",
                "Hello \${Recipient.firstName!'there'}",
                emptyMap(),
                "Hello there"
            ),
            ConformanceCase(
                "testDefaultValueSingleQuotedKeyPresent",
                "Hello \${Recipient.firstName!'there'}",
                mapOf("Recipient.firstName" to "Joe"),
                "Hello Joe"
            ),
            // Numeric comparisons  ?number gt/gte/lt/lte
            ConformanceCase(
                "testNumericGt",
                "<#if Recipient.balance?number gt 100>Eligible<#else>Not eligible</#if>",
                mapOf("Recipient.balance" to "150"),
                "Eligible"
            ),
            ConformanceCase(
                "testNumericGte",
                "<#if Recipient.balance?number gte 100>Eligible<#else>Not eligible</#if>",
                mapOf("Recipient.balance" to "100"),
                "Eligible"
            ),
            ConformanceCase(
                "testNumericLt",
                "<#if Recipient.balance?number lt 100>Low<#else>OK</#if>",
                mapOf("Recipient.balance" to "50"),
                "Low"
            ),
            ConformanceCase(
                "testNumericLte",
                "<#if Recipient.balance?number lte 100>C</#if>",
                mapOf("Recipient.balance" to "100"),
                "C"
            ),
            ConformanceCase(
                "testNumericGtBoundaryExcludes",
                "<#if Recipient.balance?number gt 100>A</#if>",
                mapOf("Recipient.balance" to "100"),
                ""
            ),
            ConformanceCase(
                "testNumericLtBoundaryExcludes",
                "<#if Recipient.balance?number lt 100>B</#if>",
                mapOf("Recipient.balance" to "100"),
                ""
            ),
            ConformanceCase(
                "testNumericFloat",
                "<#if Recipient.score?number gt 3.0>High<#else>Low</#if>",
                mapOf("Recipient.score" to "3.14"),
                "High"
            ),
            // Plain text
            ConformanceCase("testPlainTextPassThrough", "Hello World", emptyMap(), "Hello World"),
            ConformanceCase("testEmptyTemplate", "", emptyMap(), ""),
            // if / else / elseif
            ConformanceCase(
                "testIfElseTrueBranch",
                "<#if Recipient.loyalty_points == \"1200\">Gold<#else>Standard</#if>",
                mapOf("Recipient.loyalty_points" to "1200"),
                "Gold"
            ),
            ConformanceCase(
                "testIfElseFalseBranch",
                "<#if Recipient.loyalty_points == \"1200\">Gold<#else>Standard</#if>",
                mapOf("Recipient.loyalty_points" to "500"),
                "Standard"
            ),
            ConformanceCase(
                "testIfWithoutElse",
                "<#if Recipient.flag == \"yes\">Shown</#if>",
                mapOf("Recipient.flag" to "yes"),
                "Shown"
            ),
            ConformanceCase(
                "testIfWithoutElseFalse",
                "<#if Recipient.flag == \"yes\">Shown</#if>",
                mapOf("Recipient.flag" to "no"),
                ""
            ),
            ConformanceCase(
                "testNotEquals",
                "<#if Recipient.country != \"US\">International<#else>Domestic</#if>",
                mapOf("Recipient.country" to "UK"),
                "International"
            ),
            ConformanceCase(
                "testElseIfFirstBranch",
                "<#if Recipient.loyalty_points?number gte 1000>Gold<#elseif Recipient.loyalty_points?number gte 500>Silver<#else>Bronze</#if>",
                mapOf("Recipient.loyalty_points" to "1200"),
                "Gold"
            ),
            ConformanceCase(
                "testElseIfSecondBranch",
                "<#if Recipient.loyalty_points?number gte 1000>Gold<#elseif Recipient.loyalty_points?number gte 500>Silver<#else>Bronze</#if>",
                mapOf("Recipient.loyalty_points" to "700"),
                "Silver"
            ),
            ConformanceCase(
                "testElseIfFallsToElse",
                "<#if Recipient.loyalty_points?number gte 1000>Gold<#elseif Recipient.loyalty_points?number gte 500>Silver<#else>Bronze</#if>",
                mapOf("Recipient.loyalty_points" to "100"),
                "Bronze"
            ),
            ConformanceCase(
                "testElseIfBoundaryGte",
                "<#if Recipient.loyalty_points?number gte 1000>Gold<#elseif Recipient.loyalty_points?number gte 500>Silver<#else>Bronze</#if>",
                mapOf("Recipient.loyalty_points" to "1000"),
                "Gold"
            ),
            // ?boolean
            ConformanceCase(
                "testBooleanTrue",
                "<#if Recipient.email_opt_in?boolean>Subscribed<#else>Not subscribed</#if>",
                mapOf("Recipient.email_opt_in" to "true"),
                "Subscribed"
            ),
            ConformanceCase(
                "testBooleanFalse",
                "<#if Recipient.email_opt_in?boolean>Subscribed<#else>Not subscribed</#if>",
                mapOf("Recipient.email_opt_in" to "false"),
                "Not subscribed"
            ),
            ConformanceCase(
                "testTrimBooleanChainTrue",
                "<#if Recipient.flag?trim?boolean>yes<#else>no</#if>",
                mapOf("Recipient.flag" to "  true  "),
                "yes"
            ),
            ConformanceCase(
                "testTrimBooleanChainFalse",
                "<#if Recipient.flag?trim?boolean>yes<#else>no</#if>",
                mapOf("Recipient.flag" to "  false  "),
                "no"
            ),
            // Negation  !
            ConformanceCase(
                "testNegationFalseBecomesTrue",
                "<#if !Recipient.email_opt_in?boolean>Not subscribed<#else>Subscribed</#if>",
                mapOf("Recipient.email_opt_in" to "false"),
                "Not subscribed"
            ),
            ConformanceCase(
                "testNegationTrueBecomesFalse",
                "<#if !Recipient.email_opt_in?boolean>Not subscribed<#else>Subscribed</#if>",
                mapOf("Recipient.email_opt_in" to "true"),
                "Subscribed"
            ),
            ConformanceCase(
                "testNegationOfExistsCheck",
                "<#if !Recipient.name??>No name<#else>Has name</#if>",
                emptyMap(),
                "No name"
            ),
            // Exists check  ??
            ConformanceCase(
                "testExistsCheckKeyPresent",
                "<#if Recipient.firstName??>Hello \${Recipient.firstName}<#else>Hello there</#if>",
                mapOf("Recipient.firstName" to "Joe"),
                "Hello Joe"
            ),
            ConformanceCase(
                "testExistsCheckKeyMissing",
                "<#if Recipient.firstName??>Hello \${Recipient.firstName}<#else>Hello there</#if>",
                emptyMap(),
                "Hello there"
            ),
            // Nested <#if>
            ConformanceCase(
                "testNestedIfBothTrue",
                "<#if Recipient.tier == \"gold\"><#if Recipient.vip == \"true\">VIP Gold<#else>Gold</#if><#else>Standard</#if>",
                mapOf("Recipient.tier" to "gold", "Recipient.vip" to "true"),
                "VIP Gold"
            ),
            ConformanceCase(
                "testNestedIfInnerFalseBranch",
                "<#if Recipient.tier == \"gold\"><#if Recipient.vip == \"true\">VIP Gold<#else>Gold</#if><#else>Standard</#if>",
                mapOf("Recipient.tier" to "gold", "Recipient.vip" to "false"),
                "Gold"
            ),
            ConformanceCase(
                "testNestedIfOuterFalseBranch",
                "<#if Recipient.tier == \"gold\"><#if Recipient.vip == \"true\">VIP Gold<#else>Gold</#if><#else>Standard</#if>",
                mapOf("Recipient.tier" to "bronze", "Recipient.vip" to "true"),
                "Standard"
            ),
            // Logical operators  && / ||
            ConformanceCase(
                "testAndBothTrue",
                "<#if Recipient.tier == \"gold\" && Recipient.active == \"true\">yes<#else>no</#if>",
                mapOf("Recipient.tier" to "gold", "Recipient.active" to "true"),
                "yes"
            ),
            ConformanceCase(
                "testAndLeftFalse",
                "<#if Recipient.tier == \"gold\" && Recipient.active == \"true\">yes<#else>no</#if>",
                mapOf("Recipient.tier" to "silver", "Recipient.active" to "true"),
                "no"
            ),
            ConformanceCase(
                "testAndRightFalse",
                "<#if Recipient.tier == \"gold\" && Recipient.active == \"true\">yes<#else>no</#if>",
                mapOf("Recipient.tier" to "gold", "Recipient.active" to "false"),
                "no"
            ),
            ConformanceCase(
                "testOrLeftTrue",
                "<#if Recipient.tier == \"gold\" || Recipient.tier == \"platinum\">premium<#else>standard</#if>",
                mapOf("Recipient.tier" to "gold"),
                "premium"
            ),
            ConformanceCase(
                "testOrRightTrue",
                "<#if Recipient.tier == \"gold\" || Recipient.tier == \"platinum\">premium<#else>standard</#if>",
                mapOf("Recipient.tier" to "platinum"),
                "premium"
            ),
            ConformanceCase(
                "testOrBothFalse",
                "<#if Recipient.tier == \"gold\" || Recipient.tier == \"platinum\">premium<#else>standard</#if>",
                mapOf("Recipient.tier" to "bronze"),
                "standard"
            ),
            ConformanceCase(
                "testAndPrecedenceOverOr",
                "<#if Recipient.a == \"1\" || Recipient.b == \"1\" && Recipient.c == \"1\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "1", "Recipient.b" to "0", "Recipient.c" to "0"),
                "yes"
            ),
            ConformanceCase(
                "testChainedAnd",
                "<#if Recipient.a == \"1\" && Recipient.b == \"1\" && Recipient.c == \"1\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "1", "Recipient.b" to "1", "Recipient.c" to "1"),
                "yes"
            ),
            ConformanceCase(
                "testNegationWithAnd",
                "<#if !Recipient.flag?boolean && Recipient.tier == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.flag" to "false", "Recipient.tier" to "gold"),
                "yes"
            ),
            ConformanceCase(
                "testAndShortCircuitMissingKey",
                "<#if Recipient.a?? && Recipient.b == \"x\">yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            ConformanceCase(
                "testOrShortCircuitMissingKey",
                "<#if Recipient.tier == \"gold\" || Recipient.missing == \"x\">yes<#else>no</#if>",
                mapOf("Recipient.tier" to "gold"),
                "yes"
            ),
            // Newline stripping after directives
            ConformanceCase(
                "testIfDirectiveNewlineStripped",
                "<#if Recipient.tier == \"gold\">\nGold\n</#if>",
                mapOf("Recipient.tier" to "gold"),
                "Gold\n"
            ),
            ConformanceCase(
                "testEndIfDirectiveNewlineStripped",
                "<#if Recipient.tier == \"gold\">\nGold\n</#if>\nafter",
                mapOf("Recipient.tier" to "gold"),
                "Gold\nafter"
            ),
            // ! default in <#if> conditions  (parenthesised form only)
            ConformanceCase(
                "testDefaultInConditionKeyMissing",
                "<#if (Recipient.tier!\"bronze\") == \"gold\">Gold<#else>Not gold</#if>",
                emptyMap(),
                "Not gold"
            ),
            ConformanceCase(
                "testDefaultInConditionKeyMissingDefaultMatches",
                "<#if (Recipient.tier!\"gold\") == \"gold\">Gold<#else>Not gold</#if>",
                emptyMap(),
                "Gold"
            ),
            ConformanceCase(
                "testDefaultInConditionKeyPresent",
                "<#if (Recipient.tier!\"bronze\") == \"gold\">Gold<#else>Not gold</#if>",
                mapOf("Recipient.tier" to "gold"),
                "Gold"
            ),
            ConformanceCase(
                "testDefaultInNotEqualsCondition",
                "<#if (Recipient.tier!\"bronze\") != \"gold\">Not gold<#else>Gold</#if>",
                emptyMap(),
                "Not gold"
            ),
            // ! bare default in <#if> conditions
            ConformanceCase(
                "testBareDefaultInConditionKeyMissing",
                "<#if Recipient.tier! == \"gold\">Gold<#else>Not gold</#if>",
                emptyMap(),
                "Not gold"
            ),
            ConformanceCase(
                "testBareDefaultInConditionEmptyStringMatchesEmpty",
                "<#if Recipient.tier! == \"\">Empty</#if>",
                emptyMap(),
                "Empty"
            ),
            ConformanceCase(
                "testBareDefaultInConditionKeyPresent",
                "<#if Recipient.tier! == \"gold\">Gold<#else>Not gold</#if>",
                mapOf("Recipient.tier" to "gold"),
                "Gold"
            ),
            ConformanceCase(
                "testBareDefaultNotEqualsKeyMissing",
                "<#if Recipient.tier! != \"gold\">Not gold</#if>",
                emptyMap(),
                "Not gold"
            ),
            // > inside quoted string literals
            ConformanceCase(
                "testGreaterThanInsideStringLiteralMatches",
                "<#if Recipient.label == \"a>b\">yes<#else>no</#if>",
                mapOf("Recipient.label" to "a>b"),
                "yes"
            ),
            ConformanceCase(
                "testGreaterThanInsideStringLiteralNoMatch",
                "<#if Recipient.label == \"a>b\">yes<#else>no</#if>",
                mapOf("Recipient.label" to "other"),
                "no"
            ),
            // Mixed content — interpolation inside if body
            ConformanceCase(
                "testIfWithInterpolationInBody",
                "<#if Recipient.tier == \"gold\">Welcome \${Recipient.name}, Gold member!<#else>Welcome \${Recipient.name}.</#if>",
                mapOf("Recipient.tier" to "gold", "Recipient.name" to "Alice"),
                "Welcome Alice, Gold member!"
            ),
            // <#switch>
            ConformanceCase(
                "testSwitchMatchesFirstCase",
                "<#switch Recipient.country><#case \"US\">US message<#break><#case \"UK\">UK message<#break><#default>Default message</#switch>",
                mapOf("Recipient.country" to "US"),
                "US message"
            ),
            ConformanceCase(
                "testSwitchMatchesMiddleCase",
                "<#switch Recipient.country><#case \"US\">US message<#break><#case \"UK\">UK message<#break><#default>Default message</#switch>",
                mapOf("Recipient.country" to "UK"),
                "UK message"
            ),
            ConformanceCase(
                "testSwitchNoMatchFallsToDefault",
                "<#switch Recipient.country><#case \"US\">US message<#break><#case \"UK\">UK message<#break><#default>Default message</#switch>",
                mapOf("Recipient.country" to "CA"),
                "Default message"
            ),
            ConformanceCase(
                "testSwitchNoCasesReturnsEmpty",
                "<#switch Recipient.country></#switch>",
                mapOf("Recipient.country" to "US"),
                ""
            ),
            ConformanceCase(
                "testSwitchNoMatchNoDefaultReturnsEmpty",
                "<#switch Recipient.country><#case \"US\">US message<#break></#switch>",
                mapOf("Recipient.country" to "CA"),
                ""
            ),
            ConformanceCase(
                "testSwitchFallThroughWithoutBreak",
                "<#switch Recipient.x><#case \"a\">A<#case \"b\">B<#break><#default>D</#switch>",
                mapOf("Recipient.x" to "a"),
                "AB"
            ),
            ConformanceCase(
                "testSwitchFallThroughIntoDefault",
                "<#switch Recipient.x><#case \"a\">A<#default>D</#switch>",
                mapOf("Recipient.x" to "a"),
                "AD"
            ),
            ConformanceCase(
                "testSwitchWhitespaceBetweenDirectivesAllowed",
                "<#switch Recipient.x>\n  <#case \"a\">A<#break>\n</#switch>",
                mapOf("Recipient.x" to "a"),
                "A"
            ),
            ConformanceCase(
                "testDefaultDirectiveNewlineStripped",
                "<#switch Recipient.tier>\n<#case \"gold\">\nGold<#break>\n<#default>\nOther</#switch>",
                mapOf("Recipient.tier" to "bronze"),
                "Other"
            ),
            // ?trim
            ConformanceCase(
                "testTrimStripsWhitespace",
                "\${Recipient.name?trim}",
                mapOf("Recipient.name" to "  Alice  "),
                "Alice"
            ),
            ConformanceCase(
                "testTrimLeadingOnly",
                "\${Recipient.name?trim}",
                mapOf("Recipient.name" to "  Bob"),
                "Bob"
            ),
            ConformanceCase(
                "testTrimTrailingOnly",
                "\${Recipient.name?trim}",
                mapOf("Recipient.name" to "Carol  "),
                "Carol"
            ),
            ConformanceCase(
                "testTrimNoWhitespaceUnchanged",
                "\${Recipient.name?trim}",
                mapOf("Recipient.name" to "Dave"),
                "Dave"
            ),
            ConformanceCase(
                "testTrimWhitespaceOnlyBecomesEmpty",
                "\${Recipient.name?trim}",
                mapOf("Recipient.name" to "   "),
                ""
            ),
            // ?has_content
            ConformanceCase(
                "testHasContentKeyPresentNonEmpty",
                "<#if Recipient.name?has_content>Hello \${Recipient.name}<#else>Hello there</#if>",
                mapOf("Recipient.name" to "Alice"),
                "Hello Alice"
            ),
            ConformanceCase(
                "testHasContentKeyPresentEmpty",
                "<#if Recipient.name?has_content>Hi<#else>No name</#if>",
                mapOf("Recipient.name" to ""),
                "No name"
            ),
            ConformanceCase(
                "testHasContentKeyMissing",
                "<#if Recipient.name?has_content>Hi<#else>No name</#if>",
                emptyMap(),
                "No name"
            ),
            ConformanceCase(
                "testHasContentNegated",
                "<#if !Recipient.name?has_content>No name<#else>Has name</#if>",
                emptyMap(),
                "No name"
            ),
            ConformanceCase(
                "testHasContentDistinctFromExistsOnEmptyString_exists",
                "<#if Recipient.name??>exists</#if>",
                mapOf("Recipient.name" to ""),
                "exists"
            ),
            ConformanceCase(
                "testHasContentDistinctFromExistsOnEmptyString_hasContent",
                "<#if Recipient.name?has_content>non-empty</#if>",
                mapOf("Recipient.name" to ""),
                ""
            ),
            // ?trim?has_content chain
            ConformanceCase(
                "testTrimHasContentWhitespaceOnlyIsFalse",
                "<#if Recipient.name?trim?has_content>Hi \${Recipient.name?trim}<#else>Hi there</#if>",
                mapOf("Recipient.name" to "   "),
                "Hi there"
            ),
            ConformanceCase(
                "testTrimHasContentNonEmptyAfterTrim",
                "<#if Recipient.name?trim?has_content>Hi \${Recipient.name?trim}<#else>Hi there</#if>",
                mapOf("Recipient.name" to "  Alice  "),
                "Hi Alice"
            ),
            // ?trim in equality conditions
            ConformanceCase(
                "testTrimEqualsMatchAfterTrim",
                "<#if Recipient.country?trim == \"US\">Domestic<#else>International</#if>",
                mapOf("Recipient.country" to "  US  "),
                "Domestic"
            ),
            ConformanceCase(
                "testTrimEqualsNoMatchAfterTrim",
                "<#if Recipient.country?trim == \"US\">Domestic<#else>International</#if>",
                mapOf("Recipient.country" to "  UK  "),
                "International"
            ),
            ConformanceCase(
                "testTrimEqualsNoWhitespaceBehavesNormally",
                "<#if Recipient.country?trim == \"US\">Domestic<#else>International</#if>",
                mapOf("Recipient.country" to "US"),
                "Domestic"
            ),
            ConformanceCase(
                "testTrimNotEqualsMatchAfterTrim",
                "<#if Recipient.country?trim != \"US\">International<#else>Domestic</#if>",
                mapOf("Recipient.country" to "  UK  "),
                "International"
            ),
            // ?trim?number chain
            ConformanceCase(
                "testTrimNumberStripsBeforeParsing",
                "<#if Recipient.balance?trim?number gt 100>Eligible<#else>Not eligible</#if>",
                mapOf("Recipient.balance" to "  150  "),
                "Eligible"
            ),
            ConformanceCase(
                "testTrimNumberNoWhitespaceBehavesNormally",
                "<#if Recipient.balance?trim?number gte 100>Eligible<#else>Not eligible</#if>",
                mapOf("Recipient.balance" to "100"),
                "Eligible"
            ),
            // ?trim!"default" in interpolation
            ConformanceCase(
                "testTrimDefaultKeyPresentTrimsValue",
                "Hello \${Recipient.name?trim!\"there\"}",
                mapOf("Recipient.name" to "  Alice  "),
                "Hello Alice"
            ),
            ConformanceCase(
                "testTrimDefaultDoesNotTriggerOnEmptyString",
                "\${Recipient.name?trim!\"fallback\"}",
                mapOf("Recipient.name" to ""),
                ""
            ),
            // Operator spacing (== and !=)
            ConformanceCase(
                "testNotEqualsNoSpaces",
                "<#if Recipient.country!=\"UK\">Not UK<#else>UK</#if>",
                mapOf("Recipient.country" to "US"),
                "Not UK"
            ),
            ConformanceCase(
                "testEqualsNoSpaces",
                "<#if Recipient.country==\"UK\">Match<#else>No match</#if>",
                mapOf("Recipient.country" to "UK"),
                "Match"
            ),
            ConformanceCase(
                "testEqualsNoSpacesNonMatch",
                "<#if Recipient.country==\"UK\">Match<#else>No match</#if>",
                mapOf("Recipient.country" to "US"),
                "No match"
            ),
            ConformanceCase(
                "testEqualsLeadingSpaceOnly",
                "<#if Recipient.country ==\"UK\">Match</#if>",
                mapOf("Recipient.country" to "UK"),
                "Match"
            ),
            ConformanceCase(
                "testEqualsTrailingSpaceOnly",
                "<#if Recipient.country== \"UK\">Match</#if>",
                mapOf("Recipient.country" to "UK"),
                "Match"
            ),
            ConformanceCase(
                "testEqualsNoSpacesWithTrim",
                "<#if Recipient.name?trim==\"Alice\">Match<#else>No match</#if>",
                mapOf("Recipient.name" to "  Alice  "),
                "Match"
            ),
            // Parenthesis grouping
            ConformanceCase(
                "testParenGroupingStripsOuterParens",
                "<#if (Recipient.tier == \"gold\")>Gold</#if>",
                mapOf("Recipient.tier" to "gold"),
                "Gold"
            ),
            ConformanceCase(
                "testNestedParensStripped",
                "<#if ((Recipient.tier == \"gold\"))>Gold</#if>",
                mapOf("Recipient.tier" to "gold"),
                "Gold"
            ),
            ConformanceCase(
                "testParenGroupingChangesOperatorPrecedence_noMatch",
                "<#if (Recipient.loyalty_points?number gt 1000 || Recipient.account_status == \"active\") && Recipient.region == \"US\">Match</#if>",
                mapOf("Recipient.loyalty_points" to "1200", "Recipient.account_status" to "inactive", "Recipient.region" to "CA"),
                ""
            ),
            ConformanceCase(
                "testParenGroupingChangesOperatorPrecedence_match",
                "<#if (Recipient.loyalty_points?number gt 1000 || Recipient.account_status == \"active\") && Recipient.region == \"US\">Match</#if>",
                mapOf("Recipient.loyalty_points" to "1200", "Recipient.account_status" to "inactive", "Recipient.region" to "US"),
                "Match"
            ),
            ConformanceCase(
                "testParenGroupingOrBeforeAnd_withParen",
                "<#if (Recipient.x == \"a\" || Recipient.x == \"b\") && Recipient.y == \"yes\">Hit</#if>",
                mapOf("Recipient.x" to "a", "Recipient.y" to "no"),
                ""
            ),
            ConformanceCase(
                "testParenGroupingOrBeforeAnd_withoutParen",
                "<#if Recipient.x == \"a\" || Recipient.x == \"b\" && Recipient.y == \"yes\">Hit</#if>",
                mapOf("Recipient.x" to "a", "Recipient.y" to "no"),
                "Hit"
            ),
            // String concatenation with +
            ConformanceCase(
                "testConcatLiteralsAndVariables",
                "\${\"Hi \" + Recipient.first_name + \", you have \" + Recipient.loyalty_points + \" points\"}",
                mapOf("Recipient.first_name" to "John", "Recipient.loyalty_points" to "1200"),
                "Hi John, you have 1200 points"
            ),
            ConformanceCase(
                "testConcatLiteralOnly",
                "\${\"Hello\" + \" World\"}",
                emptyMap(),
                "Hello World"
            ),
            ConformanceCase(
                "testConcatTrimLeadingPart",
                "\${Recipient.first_name?trim + \" world\"}",
                mapOf("Recipient.first_name" to "  Alice  "),
                "Alice world"
            ),
            ConformanceCase(
                "testConcatTrimMiddlePart",
                "\${\"Hello \" + Recipient.first_name?trim + \"!\"}",
                mapOf("Recipient.first_name" to "  Bob  "),
                "Hello Bob!"
            ),
            ConformanceCase(
                "testConcatTrimTrailingPart",
                "\${\"prefix: \" + Recipient.first_name?trim}",
                mapOf("Recipient.first_name" to "  Carol  "),
                "prefix: Carol"
            ),
            ConformanceCase(
                "testConcatTrimPreservesOtherWhitespace",
                "\${\"prefix: \" + Recipient.first_name?trim + Recipient.last_name}",
                mapOf("Recipient.first_name" to "  Alice  ", "Recipient.last_name" to " Smith"),
                "prefix: Alice Smith"
            ),
            // Regression: comparison keyword inside quoted string literal
            ConformanceCase(
                "testEqualityWithGtInStringLiteralNotMisinterpretedAsNumericCompare",
                "<#if Recipient.label == \"a gt b\">yes<#else>no</#if>",
                mapOf("Recipient.label" to "a gt b"),
                "yes"
            ),
            ConformanceCase(
                "testEqualityWithLtInStringLiteralNotMisinterpretedAsNumericCompare",
                "<#if Recipient.label == \"a lt b\">yes<#else>no</#if>",
                mapOf("Recipient.label" to "a lt b"),
                "yes"
            ),
            ConformanceCase(
                "testEqualityWithGteInStringLiteralNotMisinterpretedAsNumericCompare",
                "<#if Recipient.label == \"a gte b\">yes<#else>no</#if>",
                mapOf("Recipient.label" to "a gte b"),
                "yes"
            ),

            // ─────────────────────────────────────────────────────────────────
            // ?lower_case / ?upper_case — interpolation
            // ─────────────────────────────────────────────────────────────────
            ConformanceCase(
                "testLowerCaseInterpolation",
                "\${Recipient.name?lower_case}",
                mapOf("Recipient.name" to "ALICE"),
                "alice"
            ),
            ConformanceCase(
                "testUpperCaseInterpolation",
                "\${Recipient.name?upper_case}",
                mapOf("Recipient.name" to "alice"),
                "ALICE"
            ),
            ConformanceCase(
                "testTrimThenLowerCaseChain",
                "\${Recipient.name?trim?lower_case}",
                mapOf("Recipient.name" to "  ALICE  "),
                "alice"
            ),
            ConformanceCase(
                "testLowerCaseThenTrimChain",
                "\${Recipient.name?lower_case?trim}",
                mapOf("Recipient.name" to "  ALICE  "),
                "alice"
            ),
            ConformanceCase(
                "testLowerCaseWithDefaultKeyPresent",
                "\${Recipient.tier?lower_case!\"unknown\"}",
                mapOf("Recipient.tier" to "GOLD"),
                "gold"
            ),

            // ─────────────────────────────────────────────────────────────────
            // ?lower_case / ?upper_case — conditions
            // ─────────────────────────────────────────────────────────────────
            ConformanceCase(
                "testLowerCaseInConditionEquality",
                "<#if Recipient.tier?lower_case == \"gold\">Gold<#else>Other</#if>",
                mapOf("Recipient.tier" to "GOLD"),
                "Gold"
            ),
            ConformanceCase(
                "testUpperCaseInConditionEquality",
                "<#if Recipient.country?upper_case == \"US\">USA<#else>International</#if>",
                mapOf("Recipient.country" to "us"),
                "USA"
            ),
            ConformanceCase(
                "testLowerCaseBooleanChain",
                "<#if Recipient.flag?lower_case?boolean>yes<#else>no</#if>",
                mapOf("Recipient.flag" to "TRUE"),
                "yes"
            ),
            ConformanceCase(
                "testTrimLowerCaseBooleanChain",
                "<#if Recipient.flag?trim?lower_case?boolean>yes<#else>no</#if>",
                mapOf("Recipient.flag" to "  TRUE  "),
                "yes"
            ),
            ConformanceCase(
                "testLowerCaseHasContentChainKeyPresent",
                "<#if Recipient.name?lower_case?has_content>yes<#else>no</#if>",
                mapOf("Recipient.name" to "ALICE"),
                "yes"
            ),
            ConformanceCase(
                "testLowerCaseInElseIfChain",
                "<#if Recipient.tier?lower_case == \"gold\">Gold<#elseif Recipient.tier?lower_case == \"silver\">Silver<#else>Bronze</#if>",
                mapOf("Recipient.tier" to "SILVER"),
                "Silver"
            ),

            // ─────────────────────────────────────────────────────────────────
            // ?lower_case / ?upper_case — concat
            // ─────────────────────────────────────────────────────────────────
            ConformanceCase(
                "testLowerCaseInConcat",
                "\${\"prefix: \" + Recipient.name?lower_case}",
                mapOf("Recipient.name" to "ALICE"),
                "prefix: alice"
            ),
            ConformanceCase(
                "testUpperCaseInConcat",
                "\${Recipient.code?upper_case + \" suffix\"}",
                mapOf("Recipient.code" to "gold"),
                "GOLD suffix"
            ),

            // ─────────────────────────────────────────────────────────────────
            // ?lower_case / ?upper_case — switch
            // ─────────────────────────────────────────────────────────────────
            ConformanceCase(
                "testSwitchWithLowerCaseTransform",
                "<#switch Recipient.tier?lower_case><#case \"gold\">Gold<#break><#default>Other</#switch>",
                mapOf("Recipient.tier" to "GOLD"),
                "Gold"
            ),
            ConformanceCase(
                "testSwitchWithTrimLowerCaseChain",
                "<#switch Recipient.tier?trim?lower_case><#case \"gold\">Gold<#break><#default>Other</#switch>",
                mapOf("Recipient.tier" to "  GOLD  "),
                "Gold"
            ),

            // ─────────────────────────────────────────────────────────────────
            // ?number == / != (numeric equality)
            // ─────────────────────────────────────────────────────────────────
            ConformanceCase(
                "testNumericEqualsMatch",
                "<#if Recipient.points?number == 10>ten<#else>other</#if>",
                mapOf("Recipient.points" to "10"),
                "ten"
            ),
            ConformanceCase(
                "testNumericEqualsNoMatch",
                "<#if Recipient.points?number == 10>ten<#else>other</#if>",
                mapOf("Recipient.points" to "5"),
                "other"
            ),
            ConformanceCase(
                "testNumericNotEquals",
                "<#if Recipient.points?number != 0>nonzero<#else>zero</#if>",
                mapOf("Recipient.points" to "5"),
                "nonzero"
            ),
            ConformanceCase(
                "testNumericEqualsFloat",
                "<#if Recipient.score?number == 9.5>yes<#else>no</#if>",
                mapOf("Recipient.score" to "9.5"),
                "yes"
            ),
            ConformanceCase(
                "testNumericEqualsWithTrimChain",
                "<#if Recipient.points?trim?number == 10>ten<#else>other</#if>",
                mapOf("Recipient.points" to "  10  "),
                "ten"
            ),
            ConformanceCase(
                "testNumericEqualsScientificNotation",
                "<#if Recipient.amount?number == 1000>match<#else>no</#if>",
                mapOf("Recipient.amount" to "1e3"),
                "match"
            ),

            // ─────────────────────────────────────────────────────────────────
            // Bare ! default (empty string fallback)
            // ─────────────────────────────────────────────────────────────────
            ConformanceCase(
                "testBareDefaultKeyMissing",
                "\${Recipient.name!}",
                emptyMap(),
                ""
            ),
            ConformanceCase(
                "testBareDefaultKeyPresent",
                "\${Recipient.name!}",
                mapOf("Recipient.name" to "Alice"),
                "Alice"
            ),

            // ─────────────────────────────────────────────────────────────────
            // Adversarial combinations
            // ─────────────────────────────────────────────────────────────────
            ConformanceCase(
                "testLowerCaseAndNumericCondition",
                "<#if Recipient.tier?lower_case == \"gold\" && Recipient.points?number gte 100>Premium<#else>Standard</#if>",
                mapOf("Recipient.tier" to "GOLD", "Recipient.points" to "150"),
                "Premium"
            ),
            ConformanceCase(
                "testUpperCaseOrCondition",
                "<#if Recipient.country?upper_case == \"US\" || Recipient.country?upper_case == \"CA\">North America<#else>Other</#if>",
                mapOf("Recipient.country" to "ca"),
                "North America"
            ),
            ConformanceCase(
                "testNestedIfWithTransforms",
                "<#if Recipient.tier?lower_case == \"gold\"><#if Recipient.points?number gte 500>VIP<#else>Standard Gold</#if><#else>Other</#if>",
                mapOf("Recipient.tier" to "GOLD", "Recipient.points" to "600"),
                "VIP"
            ),
            ConformanceCase(
                "testNumericEqualsAndTransformCondition",
                "<#if Recipient.tier?lower_case == \"gold\" && Recipient.points?number == 1000>Exact Gold<#else>Not exact</#if>",
                mapOf("Recipient.tier" to "GOLD", "Recipient.points" to "1000"),
                "Exact Gold"
            ),
            ConformanceCase(
                "testSwitchWithNestedIfAndTransforms",
                "<#switch Recipient.country?upper_case><#case \"US\"><#if Recipient.tier?lower_case == \"gold\">US Gold<#else>US Standard</#if><#break><#default>International</#switch>",
                mapOf("Recipient.country" to "us", "Recipient.tier" to "GOLD"),
                "US Gold"
            ),
            ConformanceCase(
                "testMultipleTransformsInCondition",
                "<#if Recipient.name?trim?lower_case?trim == \"alice\">yes<#else>no</#if>",
                mapOf("Recipient.name" to "  ALICE  "),
                "yes"
            ),
            ConformanceCase(
                "testLongAndChainWithTransforms",
                "<#if Recipient.a?boolean && Recipient.b?lower_case == \"active\" && Recipient.c?number gt 0>all<#else>not all</#if>",
                mapOf("Recipient.a" to "true", "Recipient.b" to "ACTIVE", "Recipient.c" to "1"),
                "all"
            ),
            ConformanceCase(
                "testNumericEqualityInElseIf",
                "<#if Recipient.level?number == 1>One<#elseif Recipient.level?number == 2>Two<#elseif Recipient.level?number == 3>Three<#else>Other</#if>",
                mapOf("Recipient.level" to "2"),
                "Two"
            ),
            ConformanceCase(
                "testLowerCaseInterpolationInIfBody",
                "<#if Recipient.active?boolean>Hello \${Recipient.name?lower_case}!<#else>Inactive</#if>",
                mapOf("Recipient.active" to "true", "Recipient.name" to "ALICE"),
                "Hello alice!"
            ),
            ConformanceCase(
                "testConcatWithMultipleTransforms",
                "\${Recipient.greeting?upper_case + \", \" + Recipient.name?lower_case + \"!\"}",
                mapOf("Recipient.greeting" to "hello", "Recipient.name" to "WORLD"),
                "HELLO, world!"
            ),
        )

        internal fun buildDataModel(templateStr: String, flatProps: Map<String, String>): Map<String, Any> {
            val result = mutableMapOf<String, Any>()
            // Scan template for PascalCase "Word." patterns (e.g. "Recipient.") to pre-create top-level
            // objects as empty maps. Uppercase-only is intentional: IAM templates always use Recipient.*.
            val topLevelRef = Regex("""\b([A-Z][A-Za-z0-9_]*)\.(?!now\b)""")
            topLevelRef.findAll(templateStr).forEach { m ->
                result.getOrPut(m.groupValues[1]) { mutableMapOf<String, Any>() }
            }
            for ((key, value) in flatProps) {
                val dot = key.indexOf('.')
                if (dot != -1) {
                    val parent = key.substring(0, dot)
                    val child = key.substring(dot + 1)

                    @Suppress("UNCHECKED_CAST")
                    val nested = result.getOrPut(parent) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
                    nested[child] = value
                } else {
                    result[key] = value
                }
            }
            return result
        }

        internal fun fmEval(templateStr: String, props: Map<String, String>, config: Configuration): String {
            val tmpl = Template("test", StringReader(templateStr), config)
            val out = StringWriter()
            tmpl.process(buildDataModel(templateStr, props), out)
            return out.toString()
        }
    }

    private lateinit var fmConfig: Configuration

    @Before
    fun setUp() {
        fmConfig = Configuration(Configuration.VERSION_2_3_23) // using same version as Accelerator
        fmConfig.defaultEncoding = "UTF-8"
    }

    /**
     * Converts flat "Recipient.firstName" -> "Joe" into { "Recipient": { "firstName": "Joe" } }.
     *
     * Also pre-populates any top-level object names found in the template as empty maps,
     * so that `Recipient.name??` returns false (rather than throwing) when no Recipient keys
     * are present in flatProps. In real FreeMarker usage the Recipient object always exists —
     * properties within it may be absent, but the top-level key is always present.
     */
    private fun buildDataModel(templateStr: String, flatProps: Map<String, String>): Map<String, Any> =
        Companion.buildDataModel(templateStr, flatProps)

    private fun fmEval(templateStr: String, props: Map<String, String> = emptyMap()): String =
        Companion.fmEval(templateStr, props, fmConfig)

    private fun sdkEval(template: String, props: Map<String, String> = emptyMap()): String =
        SwrveFreemarkerEvaluator.evaluate(template, props)

    private fun case(name: String): ConformanceCase =
        TEST_CASES.firstOrNull { it.name == name }
            ?: error("No ConformanceCase named '$name' in TEST_CASES")

    /**
     * Assert SDK and FreeMarker agree on the output for [template] + [props].
     * Optionally pass [expected] to also pin down the correct value.
     */
    private fun assertConforms(template: String, props: Map<String, String> = emptyMap(), expected: String? = null) {
        val sdkResult = sdkEval(template, props)
        val fmResult = fmEval(template, props)
        if (expected != null) {
            assertEquals("SDK output mismatch (template: $template)", expected, sdkResult)
            assertEquals("FreeMarker output mismatch (template: $template)", expected, fmResult)
        }
        assertEquals(
            "SDK and FreeMarker diverge\n  template: $template\n  props: $props\n  sdk=$sdkResult  fm=$fmResult",
            fmResult, sdkResult
        )
    }

    private fun assertConforms(c: ConformanceCase) = assertConforms(c.template, c.props, c.expected)

    // -------------------------------------------------------------------------
    // Interpolation
    // -------------------------------------------------------------------------

    @Test
    fun testSimpleInterpolation() = assertConforms(case("testSimpleInterpolation"))

    @Test
    fun testMultipleInterpolations() = assertConforms(case("testMultipleInterpolations"))

    // -------------------------------------------------------------------------
    // Default value operator  !
    // -------------------------------------------------------------------------

    @Test
    fun testDefaultValueKeyPresent() = assertConforms(case("testDefaultValueKeyPresent"))

    @Test
    fun testDefaultValueKeyMissing() = assertConforms(case("testDefaultValueKeyMissing"))

    @Test
    fun testDefaultValueDoesNotTriggerOnEmptyString() = assertConforms(case("testDefaultValueDoesNotTriggerOnEmptyString"))

    @Test
    fun testDefaultValueSingleQuotedKeyMissing() = assertConforms(case("testDefaultValueSingleQuotedKeyMissing"))

    @Test
    fun testDefaultValueSingleQuotedKeyPresent() = assertConforms(case("testDefaultValueSingleQuotedKeyPresent"))

    // -------------------------------------------------------------------------
    // Numeric comparisons  ?number gt/gte/lt/lte
    // -------------------------------------------------------------------------

    @Test
    fun testNumericGt() = assertConforms(case("testNumericGt"))

    @Test
    fun testNumericGte() = assertConforms(case("testNumericGte"))

    @Test
    fun testNumericLt() = assertConforms(case("testNumericLt"))

    @Test
    fun testNumericLte() = assertConforms(case("testNumericLte"))

    @Test
    fun testNumericGtBoundaryExcludes() = assertConforms(case("testNumericGtBoundaryExcludes"))

    @Test
    fun testNumericLtBoundaryExcludes() = assertConforms(case("testNumericLtBoundaryExcludes"))

    @Test
    fun testNumericFloat() = assertConforms(case("testNumericFloat"))

    // -------------------------------------------------------------------------
    // Plain text
    // -------------------------------------------------------------------------

    @Test
    fun testPlainTextPassThrough() = assertConforms(case("testPlainTextPassThrough"))

    @Test
    fun testEmptyTemplate() = assertConforms(case("testEmptyTemplate"))

    // -------------------------------------------------------------------------
    // if / else / elseif
    // -------------------------------------------------------------------------

    @Test
    fun testIfElseTrueBranch() = assertConforms(case("testIfElseTrueBranch"))

    @Test
    fun testIfElseFalseBranch() = assertConforms(case("testIfElseFalseBranch"))

    @Test
    fun testIfWithoutElse() = assertConforms(case("testIfWithoutElse"))

    @Test
    fun testIfWithoutElseFalse() = assertConforms(case("testIfWithoutElseFalse"))

    @Test
    fun testNotEquals() = assertConforms(case("testNotEquals"))

    @Test
    fun testElseIfFirstBranch() = assertConforms(case("testElseIfFirstBranch"))

    @Test
    fun testElseIfSecondBranch() = assertConforms(case("testElseIfSecondBranch"))

    @Test
    fun testElseIfFallsToElse() = assertConforms(case("testElseIfFallsToElse"))

    @Test
    fun testElseIfBoundaryGte() = assertConforms(case("testElseIfBoundaryGte"))

    // -------------------------------------------------------------------------
    // ?boolean
    // -------------------------------------------------------------------------

    @Test
    fun testBooleanTrue() = assertConforms(case("testBooleanTrue"))

    @Test
    fun testBooleanFalse() = assertConforms(case("testBooleanFalse"))

    @Test
    fun testTrimBooleanChainTrue() = assertConforms(case("testTrimBooleanChainTrue"))

    @Test
    fun testTrimBooleanChainFalse() = assertConforms(case("testTrimBooleanChainFalse"))

    // -------------------------------------------------------------------------
    // Negation  !
    // -------------------------------------------------------------------------

    @Test
    fun testNegationFalseBecomesTrue() = assertConforms(case("testNegationFalseBecomesTrue"))

    @Test
    fun testNegationTrueBecomesFalse() = assertConforms(case("testNegationTrueBecomesFalse"))

    @Test
    fun testNegationOfExistsCheck() = assertConforms(case("testNegationOfExistsCheck"))

    // -------------------------------------------------------------------------
    // Exists check  ??
    // -------------------------------------------------------------------------

    @Test
    fun testExistsCheckKeyPresent() = assertConforms(case("testExistsCheckKeyPresent"))

    @Test
    fun testExistsCheckKeyMissing() = assertConforms(case("testExistsCheckKeyMissing"))

    // -------------------------------------------------------------------------
    // Nested <#if>
    // -------------------------------------------------------------------------

    @Test
    fun testNestedIfBothTrue() = assertConforms(case("testNestedIfBothTrue"))

    @Test
    fun testNestedIfInnerFalseBranch() = assertConforms(case("testNestedIfInnerFalseBranch"))

    @Test
    fun testNestedIfOuterFalseBranch() = assertConforms(case("testNestedIfOuterFalseBranch"))

    // -------------------------------------------------------------------------
    // Logical operators  && / ||
    // -------------------------------------------------------------------------

    @Test
    fun testAndBothTrue() = assertConforms(case("testAndBothTrue"))

    @Test
    fun testAndLeftFalse() = assertConforms(case("testAndLeftFalse"))

    @Test
    fun testAndRightFalse() = assertConforms(case("testAndRightFalse"))

    @Test
    fun testOrLeftTrue() = assertConforms(case("testOrLeftTrue"))

    @Test
    fun testOrRightTrue() = assertConforms(case("testOrRightTrue"))

    @Test
    fun testOrBothFalse() = assertConforms(case("testOrBothFalse"))

    @Test
    fun testAndPrecedenceOverOr() = assertConforms(case("testAndPrecedenceOverOr"))

    @Test
    fun testChainedAnd() = assertConforms(case("testChainedAnd"))

    @Test
    fun testNegationWithAnd() = assertConforms(case("testNegationWithAnd"))

    @Test
    fun testAndShortCircuitMissingKey() = assertConforms(case("testAndShortCircuitMissingKey"))

    @Test
    fun testOrShortCircuitMissingKey() = assertConforms(case("testOrShortCircuitMissingKey"))

    // -------------------------------------------------------------------------
    // Newline stripping after directives
    // -------------------------------------------------------------------------

    @Test
    fun testIfDirectiveNewlineStripped() = assertConforms(case("testIfDirectiveNewlineStripped"))

    @Test
    fun testEndIfDirectiveNewlineStripped() = assertConforms(case("testEndIfDirectiveNewlineStripped"))

    @Test
    fun testElseDirectiveNewlineStripped() =
        assertConforms(
            "<#if Recipient.tier == \"gold\">\nGold<#else>\nSilver</#if>",
            mapOf("Recipient.tier" to "silver"),
            "\nSilver"
        )

    @Test
    fun testElseOnOwnLineNewlineStripped() =
        // <#else> starts a new line — \n after it is stripped, matching FreeMarker behaviour
        assertConforms(
            "<#if Recipient.tier == \"gold\">\nGold\n<#else>\nSilver\n</#if>",
            mapOf("Recipient.tier" to "silver"),
            "Silver\n"
        )

    @Test
    fun testElseIfDirectiveNewlineStripped() =
        assertConforms(
            "<#if Recipient.tier == \"gold\">\nGold<#elseif Recipient.tier == \"silver\">\nSilver<#else>\nBronze</#if>",
            mapOf("Recipient.tier" to "silver"),
            "\nSilver"
        )

    @Test
    fun testElseIfOnOwnLineNewlineStripped() =
        // <#elseif> starts a new line — \n after it is stripped, matching FreeMarker behaviour
        assertConforms(
            "<#if Recipient.tier == \"gold\">\nGold\n<#elseif Recipient.tier == \"silver\">\nSilver\n<#else>\nBronze\n</#if>",
            mapOf("Recipient.tier" to "silver"),
            "Silver\n"
        )

    @Test
    fun testInlineDirectiveNewlineNotStripped() =
        // <#if> is mid-line (non-whitespace before it) — \n after </#if> is kept
        assertConforms(
            "Hello<#if Recipient.name??>!</#if>\nWorld",
            mapOf("Recipient.name" to "Alice"),
            "Hello!\nWorld"
        )

    @Test
    fun testCrlfAfterDirectiveStripped() =
        // CRLF line endings: \r\n after an on-own-line directive is stripped
        // Note: cannot be a conformance fixture — the fixture generator normalises CRLF
        assertConforms(
            "<#if Recipient.tier == \"gold\">\r\nGold\r\n</#if>",
            mapOf("Recipient.tier" to "gold"),
            "Gold\r\n"
        )

    @Test
    fun testCrAfterDirectiveStripped() =
        // Bare \r (classic Mac) after an on-own-line directive is stripped
        assertConforms(
            "<#if Recipient.tier == \"gold\">\rGold\r</#if>",
            mapOf("Recipient.tier" to "gold"),
            "Gold\r"
        )

    @Test
    fun testCrlfInlineDirectiveNotStripped() =
        // </#if> is mid-line — \r\n after it must NOT be stripped
        assertConforms(
            "Hello<#if Recipient.name??>!</#if>\r\nWorld",
            mapOf("Recipient.name" to "Alice"),
            "Hello!\r\nWorld"
        )

    // -------------------------------------------------------------------------
    // ! default in <#if> conditions  (parenthesised form only — see @Ignore below)
    // -------------------------------------------------------------------------

    @Test
    fun testDefaultInConditionKeyMissing() = assertConforms(case("testDefaultInConditionKeyMissing"))

    @Test
    fun testDefaultInConditionKeyMissingDefaultMatches() = assertConforms(case("testDefaultInConditionKeyMissingDefaultMatches"))

    @Test
    fun testDefaultInConditionKeyPresent() = assertConforms(case("testDefaultInConditionKeyPresent"))

    @Test
    fun testDefaultInNotEqualsCondition() = assertConforms(case("testDefaultInNotEqualsCondition"))

    @Test
    fun testBareDefaultInConditionKeyMissing() = assertConforms(case("testBareDefaultInConditionKeyMissing"))

    @Test
    fun testBareDefaultInConditionEmptyStringMatchesEmpty() = assertConforms(case("testBareDefaultInConditionEmptyStringMatchesEmpty"))

    @Test
    fun testBareDefaultInConditionKeyPresent() = assertConforms(case("testBareDefaultInConditionKeyPresent"))

    @Test
    fun testBareDefaultNotEqualsKeyMissing() = assertConforms(case("testBareDefaultNotEqualsKeyMissing"))

    // -------------------------------------------------------------------------
    // > inside quoted string literals
    // -------------------------------------------------------------------------

    @Test
    fun testGreaterThanInsideStringLiteralMatches() = assertConforms(case("testGreaterThanInsideStringLiteralMatches"))

    @Test
    fun testGreaterThanInsideStringLiteralNoMatch() = assertConforms(case("testGreaterThanInsideStringLiteralNoMatch"))

    // -------------------------------------------------------------------------
    // Mixed content — interpolation inside if body
    // -------------------------------------------------------------------------

    @Test
    fun testIfWithInterpolationInBody() = assertConforms(case("testIfWithInterpolationInBody"))

    // -------------------------------------------------------------------------
    // <#switch>
    // -------------------------------------------------------------------------

    @Test
    fun testSwitchMatchesFirstCase() = assertConforms(case("testSwitchMatchesFirstCase"))

    @Test
    fun testSwitchMatchesMiddleCase() = assertConforms(case("testSwitchMatchesMiddleCase"))

    @Test
    fun testSwitchNoMatchFallsToDefault() = assertConforms(case("testSwitchNoMatchFallsToDefault"))

    @Test
    fun testSwitchNoCasesReturnsEmpty() = assertConforms(case("testSwitchNoCasesReturnsEmpty"))

    @Test
    fun testSwitchNoMatchNoDefaultReturnsEmpty() = assertConforms(case("testSwitchNoMatchNoDefaultReturnsEmpty"))

    @Test
    fun testSwitchFallThroughWithoutBreak() = assertConforms(case("testSwitchFallThroughWithoutBreak"))

    @Test
    fun testSwitchFallThroughIntoDefault() = assertConforms(case("testSwitchFallThroughIntoDefault"))

    @Test
    fun testSwitchWhitespaceBetweenDirectivesAllowed() = assertConforms(case("testSwitchWhitespaceBetweenDirectivesAllowed"))

    @Test
    fun testSwitchEndTagNewlineNotStrippedWhenInline() =
        assertConforms(
            "<#switch Recipient.tier>\n<#case \"gold\">\nGold<#break>\n<#default>\nOther</#switch>\n",
            mapOf("Recipient.tier" to "gold"),
            "Gold\n"
        )

    @Test
    fun testDefaultDirectiveNewlineStripped() = assertConforms(case("testDefaultDirectiveNewlineStripped"))

    // -------------------------------------------------------------------------
    // ?trim
    // -------------------------------------------------------------------------

    @Test
    fun testTrimStripsWhitespace() = assertConforms(case("testTrimStripsWhitespace"))

    @Test
    fun testTrimLeadingOnly() = assertConforms(case("testTrimLeadingOnly"))

    @Test
    fun testTrimTrailingOnly() = assertConforms(case("testTrimTrailingOnly"))

    @Test
    fun testTrimNoWhitespaceUnchanged() = assertConforms(case("testTrimNoWhitespaceUnchanged"))

    @Test
    fun testTrimWhitespaceOnlyBecomesEmpty() = assertConforms(case("testTrimWhitespaceOnlyBecomesEmpty"))

    // -------------------------------------------------------------------------
    // ?has_content
    // -------------------------------------------------------------------------

    @Test
    fun testHasContentKeyPresentNonEmpty() = assertConforms(case("testHasContentKeyPresentNonEmpty"))

    @Test
    fun testHasContentKeyPresentEmpty() = assertConforms(case("testHasContentKeyPresentEmpty"))

    @Test
    fun testHasContentKeyMissing() = assertConforms(case("testHasContentKeyMissing"))

    @Test
    fun testHasContentNegated() = assertConforms(case("testHasContentNegated"))

    @Test
    fun testHasContentDistinctFromExistsOnEmptyString() {
        assertConforms(case("testHasContentDistinctFromExistsOnEmptyString_exists"))
        assertConforms(case("testHasContentDistinctFromExistsOnEmptyString_hasContent"))
    }

    // -------------------------------------------------------------------------
    // ?trim?has_content chain
    // -------------------------------------------------------------------------

    @Test
    fun testTrimHasContentWhitespaceOnlyIsFalse() = assertConforms(case("testTrimHasContentWhitespaceOnlyIsFalse"))

    @Test
    fun testTrimHasContentNonEmptyAfterTrim() = assertConforms(case("testTrimHasContentNonEmptyAfterTrim"))

    // -------------------------------------------------------------------------
    // ?trim in equality conditions
    // -------------------------------------------------------------------------

    @Test
    fun testTrimEqualsMatchAfterTrim() = assertConforms(case("testTrimEqualsMatchAfterTrim"))

    @Test
    fun testTrimEqualsNoMatchAfterTrim() = assertConforms(case("testTrimEqualsNoMatchAfterTrim"))

    @Test
    fun testTrimEqualsNoWhitespaceBehavesNormally() = assertConforms(case("testTrimEqualsNoWhitespaceBehavesNormally"))

    @Test
    fun testTrimNotEqualsMatchAfterTrim() = assertConforms(case("testTrimNotEqualsMatchAfterTrim"))

    // -------------------------------------------------------------------------
    // ?trim?number chain
    // -------------------------------------------------------------------------

    @Test
    fun testTrimNumberStripsBeforeParsing() = assertConforms(case("testTrimNumberStripsBeforeParsing"))

    @Test
    fun testTrimNumberNoWhitespaceBehavesNormally() = assertConforms(case("testTrimNumberNoWhitespaceBehavesNormally"))

    // -------------------------------------------------------------------------
    // ?trim!"default" in interpolation
    // -------------------------------------------------------------------------

    @Test
    fun testTrimDefaultKeyPresentTrimsValue() = assertConforms(case("testTrimDefaultKeyPresentTrimsValue"))

    /**
     * DIVERGENCE: SDK propagates "missing key" state through the ?trim built-in chain,
     * so ?trim!"default" on a missing key returns the default.
     * FreeMarker throws InvalidReferenceException on ?trim when the variable is missing,
     * because ?trim is not null-safe. In FreeMarker, use (Recipient.name!"default")?trim
     * or (Recipient.name?trim)!"default" with a null-safe expression.
     */
    @Ignore("FreeMarker throws on ?trim of missing variable; SDK propagates missing through chain")
    @Test
    fun testTrimDefaultKeyMissingReturnsDefault() =
        assertConforms("Hello \${Recipient.name?trim!\"there\"}", emptyMap(), "Hello there")

    @Test
    fun testTrimDefaultDoesNotTriggerOnEmptyString() = assertConforms(case("testTrimDefaultDoesNotTriggerOnEmptyString"))

    /** DIVERGENCE: same root cause as testTrimDefaultKeyMissingReturnsDefault above. */
    @Ignore("FreeMarker throws on ?trim of missing variable; SDK propagates missing through chain")
    @Test
    fun testTrimDefaultSingleQuoted() =
        assertConforms("Hello \${Recipient.name?trim!'there'}", emptyMap(), "Hello there")

    // -------------------------------------------------------------------------
    // Operator spacing (== and !=)
    // -------------------------------------------------------------------------

    @Test
    fun testNotEqualsNoSpaces() = assertConforms(case("testNotEqualsNoSpaces"))

    @Test
    fun testEqualsNoSpaces() = assertConforms(case("testEqualsNoSpaces"))

    @Test
    fun testEqualsNoSpacesNonMatch() = assertConforms(case("testEqualsNoSpacesNonMatch"))

    @Test
    fun testEqualsLeadingSpaceOnly() = assertConforms(case("testEqualsLeadingSpaceOnly"))

    @Test
    fun testEqualsTrailingSpaceOnly() = assertConforms(case("testEqualsTrailingSpaceOnly"))

    @Test
    fun testEqualsNoSpacesWithTrim() = assertConforms(case("testEqualsNoSpacesWithTrim"))

    // -------------------------------------------------------------------------
    // Parenthesis grouping
    // -------------------------------------------------------------------------

    @Test
    fun testParenGroupingStripsOuterParens() = assertConforms(case("testParenGroupingStripsOuterParens"))

    @Test
    fun testNestedParensStripped() = assertConforms(case("testNestedParensStripped"))

    @Test
    fun testParenGroupingChangesOperatorPrecedence() {
        assertConforms(case("testParenGroupingChangesOperatorPrecedence_noMatch"))
        assertConforms(case("testParenGroupingChangesOperatorPrecedence_match"))
    }

    @Test
    fun testParenGroupingOrBeforeAnd() {
        assertConforms(case("testParenGroupingOrBeforeAnd_withParen"))
        assertConforms(case("testParenGroupingOrBeforeAnd_withoutParen"))
    }

    // -------------------------------------------------------------------------
    // String concatenation with +
    // -------------------------------------------------------------------------

    @Test
    fun testConcatLiteralsAndVariables() = assertConforms(case("testConcatLiteralsAndVariables"))

    @Test
    fun testConcatLiteralOnly() = assertConforms(case("testConcatLiteralOnly"))

    @Test
    fun testConcatTrimLeadingPart() = assertConforms(case("testConcatTrimLeadingPart"))

    @Test
    fun testConcatTrimMiddlePart() = assertConforms(case("testConcatTrimMiddlePart"))

    @Test
    fun testConcatTrimTrailingPart() = assertConforms(case("testConcatTrimTrailingPart"))

    @Test
    fun testConcatTrimPreservesOtherWhitespace() = assertConforms(case("testConcatTrimPreservesOtherWhitespace"))

    // -------------------------------------------------------------------------
    // Regression: comparison keyword inside quoted string literal
    // -------------------------------------------------------------------------

    @Test
    fun testEqualityWithGtInStringLiteralNotMisinterpretedAsNumericCompare() = assertConforms(case("testEqualityWithGtInStringLiteralNotMisinterpretedAsNumericCompare"))

    @Test
    fun testEqualityWithLtInStringLiteralNotMisinterpretedAsNumericCompare() = assertConforms(case("testEqualityWithLtInStringLiteralNotMisinterpretedAsNumericCompare"))

    @Test
    fun testEqualityWithGteInStringLiteralNotMisinterpretedAsNumericCompare() = assertConforms(case("testEqualityWithGteInStringLiteralNotMisinterpretedAsNumericCompare"))

    // ─────────────────────────────────────────────────────────────────────────
    // ?lower_case / ?upper_case — interpolation
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun testLowerCaseInterpolation() = assertConforms(case("testLowerCaseInterpolation"))
    @Test fun testUpperCaseInterpolation() = assertConforms(case("testUpperCaseInterpolation"))
    @Test fun testTrimThenLowerCaseChain() = assertConforms(case("testTrimThenLowerCaseChain"))
    @Test fun testLowerCaseThenTrimChain() = assertConforms(case("testLowerCaseThenTrimChain"))
    @Test fun testLowerCaseWithDefaultKeyPresent() = assertConforms(case("testLowerCaseWithDefaultKeyPresent"))

    // ─────────────────────────────────────────────────────────────────────────
    // ?lower_case / ?upper_case — conditions
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun testLowerCaseInConditionEquality() = assertConforms(case("testLowerCaseInConditionEquality"))
    @Test fun testUpperCaseInConditionEquality() = assertConforms(case("testUpperCaseInConditionEquality"))
    @Test fun testLowerCaseBooleanChain() = assertConforms(case("testLowerCaseBooleanChain"))
    @Test fun testTrimLowerCaseBooleanChain() = assertConforms(case("testTrimLowerCaseBooleanChain"))
    @Test fun testLowerCaseHasContentChainKeyPresent() = assertConforms(case("testLowerCaseHasContentChainKeyPresent"))
    @Test fun testLowerCaseInElseIfChain() = assertConforms(case("testLowerCaseInElseIfChain"))

    // ─────────────────────────────────────────────────────────────────────────
    // ?lower_case / ?upper_case — concat and switch
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun testLowerCaseInConcat() = assertConforms(case("testLowerCaseInConcat"))
    @Test fun testUpperCaseInConcat() = assertConforms(case("testUpperCaseInConcat"))
    @Test fun testSwitchWithLowerCaseTransform() = assertConforms(case("testSwitchWithLowerCaseTransform"))
    @Test fun testSwitchWithTrimLowerCaseChain() = assertConforms(case("testSwitchWithTrimLowerCaseChain"))

    // ─────────────────────────────────────────────────────────────────────────
    // ?number == / != (numeric equality)
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun testNumericEqualsMatch() = assertConforms(case("testNumericEqualsMatch"))
    @Test fun testNumericEqualsNoMatch() = assertConforms(case("testNumericEqualsNoMatch"))
    @Test fun testNumericNotEquals() = assertConforms(case("testNumericNotEquals"))
    @Test fun testNumericEqualsFloat() = assertConforms(case("testNumericEqualsFloat"))
    @Test fun testNumericEqualsWithTrimChain() = assertConforms(case("testNumericEqualsWithTrimChain"))
    @Test fun testNumericEqualsScientificNotation() = assertConforms(case("testNumericEqualsScientificNotation"))

    // ─────────────────────────────────────────────────────────────────────────
    // Bare ! default
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun testBareDefaultKeyMissing() = assertConforms(case("testBareDefaultKeyMissing"))
    @Test fun testBareDefaultKeyPresent() = assertConforms(case("testBareDefaultKeyPresent"))

    // ─────────────────────────────────────────────────────────────────────────
    // Adversarial combinations
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun testLowerCaseAndNumericCondition() = assertConforms(case("testLowerCaseAndNumericCondition"))
    @Test fun testUpperCaseOrCondition() = assertConforms(case("testUpperCaseOrCondition"))
    @Test fun testNestedIfWithTransforms() = assertConforms(case("testNestedIfWithTransforms"))
    @Test fun testNumericEqualsAndTransformCondition() = assertConforms(case("testNumericEqualsAndTransformCondition"))
    @Test fun testSwitchWithNestedIfAndTransforms() = assertConforms(case("testSwitchWithNestedIfAndTransforms"))
    @Test fun testMultipleTransformsInCondition() = assertConforms(case("testMultipleTransformsInCondition"))
    @Test fun testLongAndChainWithTransforms() = assertConforms(case("testLongAndChainWithTransforms"))
    @Test fun testNumericEqualityInElseIf() = assertConforms(case("testNumericEqualityInElseIf"))
    @Test fun testLowerCaseInterpolationInIfBody() = assertConforms(case("testLowerCaseInterpolationInIfBody"))
    @Test fun testConcatWithMultipleTransforms() = assertConforms(case("testConcatWithMultipleTransforms"))

    // =========================================================================
    // KNOWN DIVERGENCES — marked @Ignore with explanation
    // =========================================================================

    /**
     * DIVERGENCE: Date & datetime built-ins (?date, ?datetime, .now) are an intentional
     * SDK-specific subset that is deliberately NOT conformance-tested against stock FreeMarker:
     *
     *  - `.now` in stock FreeMarker is the real wall-clock time at evaluation, so its output can
     *    never be captured as a stable golden fixture (it would freeze to fixture-generation time
     *    and then never match at test time). The SDK instead exposes an injectable nowProvider.
     *  - The SDK parses ISO 8601 strings and compares calendar days / instants directly, whereas
     *    stock FreeMarker's ?date/?datetime depend on the Configuration's date_format and have no
     *    notion of the campaign GLOBAL/LOCAL timezone the SDK layers on top.
     *
     * These built-ins are covered instead by SwrveFreemarkerDatetimeTest (behaviour, with a fixed
     * nowProvider) and by the timezone_type wiring tests in SwrveBaseCampaignUnitTest
     * (testDynamicImageUrlDateConditionUsesCampaignTimezone).
     */
    @Ignore("Date built-ins are an SDK-specific subset; .now is non-deterministic wall-clock in stock FreeMarker and the GLOBAL/LOCAL timezone semantics have no FreeMarker equivalent")
    @Test
    fun testDateBuiltinsAreSdkSpecificNotConformanceTested() =
        assertConforms("<#if Recipient.expiry?date gt .now?date>Valid<#else>Expired</#if>", mapOf("Recipient.expiry" to "2099-01-01"))

    /**
     * DIVERGENCE: ?trim?has_content on a missing variable.
     * SDK propagates "missing key" state through the built-in chain and returns false for
     * ?has_content, producing "No name".
     * FreeMarker throws InvalidReferenceException when ?trim is applied to an undefined variable
     * because ?trim is not null-safe.
     * Workaround in real FreeMarker: use (Recipient.name!"")?trim?has_content.
     */
    @Ignore("SDK returns 'No name'; FreeMarker throws on ?trim of undefined variable")
    @Test
    fun testTrimHasContentMissingKeyIsFalse() =
        assertConforms("<#if Recipient.name?trim?has_content>Hi<#else>No name</#if>", emptyMap(), "No name")

    /**
     * DIVERGENCE: unparenthesised ! default operator vs == precedence.
     * SDK parses  Recipient.tier!"bronze" == "bronze"  as  (Recipient.tier!"bronze") == "bronze".
     * FreeMarker gives ! lower precedence than ==, so it parses as  Recipient.tier!("bronze" == "bronze")
     * = Recipient.tier!false — evaluating the condition as a boolean false, which causes a
     * "Boolean expected, but got String" error when the key is present.
     * Use explicit parentheses to get portable behaviour: (Recipient.tier!"bronze") == "bronze".
     */
    @Ignore("SDK has higher ! precedence than FreeMarker in unparenthesised conditions")
    @Test
    fun testDefaultInConditionWithoutParens() =
        assertConforms("<#if Recipient.tier!\"bronze\" == \"bronze\">Bronze<#else>Other</#if>", emptyMap(), "Bronze")

    /**
     * DIVERGENCE: ?trim!default precedence inside <#if> (same root cause as above).
     * Additionally, FreeMarker throws on ?trim of an undefined variable (missing key case).
     */
    @Ignore("SDK has higher ! precedence; FreeMarker also throws on ?trim of undefined key")
    @Test
    fun testTrimWithDefaultInConditionKeyMissing() =
        assertConforms("<#if Recipient.tier?trim!\"bronze\" == \"bronze\">Bronze<#else>Other</#if>", emptyMap(), "Bronze")

    /**
     * DIVERGENCE: FreeMarker 2.3.23 ?boolean only accepts exactly "true" or "false"
     * (the docs say "case-insensitive since 2.3.20" but the implementation rejects "TRUE").
     * Our SDK converts to lowercase before comparison so "TRUE" → true.
     */
    @Ignore("FreeMarker rejects 'TRUE' for ?boolean; SDK accepts it case-insensitively")
    @Test
    fun testBooleanCaseInsensitive() =
        assertConforms(
            "<#if Recipient.email_opt_in?boolean>Subscribed</#if>",
            mapOf("Recipient.email_opt_in" to "TRUE"),
            "Subscribed"
        )

    /**
     * DIVERGENCE: trailing whitespace line after a directive tag.
     * Template: <#if condition>  \nGold</#if>  (two spaces before \n)
     * SDK preserves the "  \n" because it only strips a bare \n immediately after a directive.
     * FreeMarker's whitespace-stripping rule strips any line whose content is ONLY directives
     * and whitespace, so the "  " + "\n" is stripped and the output is just "Gold".
     */
    @Ignore("SDK preserves trailing spaces+newline after directive; FreeMarker strips them")
    @Test
    fun testNewlineNotStrippedWhenSpacesBefore() =
        assertConforms("<#if Recipient.tier == \"gold\">  \nGold</#if>", mapOf("Recipient.tier" to "gold"), "  \nGold")

    /**
     * DIVERGENCE: ?lower_case!"default" on a missing variable.
     * SDK: missing key → transforms not applied → returns "fallback".
     * FreeMarker: ?lower_case is not null-safe; throws InvalidReferenceException on missing
     * variable even when followed by !. Same root cause as the ?trim!"fallback" divergence.
     * Portable workaround: ${(Recipient.name!"")?lower_case}
     */
    @Ignore("SDK returns fallback; FreeMarker throws on ?lower_case of undefined variable")
    @Test
    fun testLowerCaseWithDefaultKeyMissing() =
        assertConforms("\${Recipient.name?lower_case!\"fallback\"}", emptyMap(), "fallback")

    /**
     * DIVERGENCE: ?upper_case!"default" on a missing variable.
     * Same root cause as ?lower_case!"default" above.
     */
    @Ignore("SDK returns fallback; FreeMarker throws on ?upper_case of undefined variable")
    @Test
    fun testUpperCaseWithDefaultKeyMissing() =
        assertConforms("\${Recipient.name?upper_case!\"fallback\"}", emptyMap(), "fallback")

    /**
     * DIVERGENCE: ?lower_case?has_content on a missing variable.
     * SDK: ?has_content is null-safe in our evaluator — missing key returns false gracefully.
     * FreeMarker: ?lower_case is applied first, throws InvalidReferenceException for missing key;
     * ?has_content cannot save it since it's chained after a non-null-safe built-in.
     */
    @Ignore("SDK returns false; FreeMarker throws on ?lower_case?has_content of undefined variable")
    @Test
    fun testLowerCaseHasContentMissingKey() =
        assertConforms(
            "<#if Recipient.name?lower_case?has_content>yes<#else>no</#if>",
            emptyMap(),
            "no"
        )

    /**
     * DIVERGENCE: ?upper_case?boolean — SDK evaluates "true" → upper_case → "TRUE" →
     * boolean (SDK lowercases before comparing, so "TRUE" → true).
     * FreeMarker 2.3.23: "true" → upper_case → "TRUE" → ?boolean rejects "TRUE"
     * (same as the bare ?boolean "TRUE" case above).
     */
    @Ignore("SDK accepts ?upper_case?boolean on 'true'; FreeMarker 2.3.23 rejects 'TRUE' for ?boolean")
    @Test
    fun testUpperCaseBooleanDivergence() =
        assertConforms(
            "<#if Recipient.flag?upper_case?boolean>yes<#else>no</#if>",
            mapOf("Recipient.flag" to "true"),
            "yes"
        )

    /**
     * DIVERGENCE: bare ! after ?lower_case on a missing variable.
     * SDK: missing key → returns "" (bare ! default).
     * FreeMarker: ?lower_case is not null-safe; throws before bare ! can provide a default.
     */
    @Ignore("SDK returns empty string; FreeMarker throws on ?lower_case! of undefined variable")
    @Test
    fun testLowerCaseBareDefaultKeyMissing() =
        assertConforms("\${Recipient.name?lower_case!}", emptyMap(), "")
}
