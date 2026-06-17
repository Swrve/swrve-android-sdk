package com.swrve.sdk

import freemarker.template.Configuration
import freemarker.template.TemplateException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

/**
 * Adversarial conformance tests: combinatorial cases that stress the interactions between
 * built-ins, operators, contexts, and edge-case values.
 *
 * Every assertConforms test asserts SDK output == real FreeMarker 2.3.23 output.
 * Known divergences use assertDiverges instead of @Ignore — they run and pass while the
 * divergence exists, and fail if SDK and FreeMarker ever agree (signalling the case should
 * be promoted to assertConforms). See the bottom of this file for details.
 *
 * Structure mirrors SwrveFreemarkerConformanceTest: TEST_CASES holds the shared data read
 * by SwrveFreemarkerAdversarialFixtureGenerator to produce the iOS fixture JSON.
 */
class SwrveFreemarkerAdversarialTest {

    data class ConformanceCase(
        val name: String,
        val template: String,
        val props: Map<String, String> = emptyMap(),
        val expected: String
    )

    companion object {
        val TEST_CASES: List<ConformanceCase> = listOf(

            // ═════════════════════════════════════════════════════════════════
            // TRANSFORM MATRIX — every built-in × every context
            // ═════════════════════════════════════════════════════════════════

            // ── ?trim — interpolation ─────────────────────────────────────────
            ConformanceCase(
                "trimInterpolationLeadingTrailing",
                "\${Recipient.v?trim}",
                mapOf("Recipient.v" to "  hello  "),
                "hello"
            ),
            ConformanceCase(
                "trimInterpolationEmptyString",
                "\${Recipient.v?trim}",
                mapOf("Recipient.v" to ""),
                ""
            ),
            ConformanceCase(
                "trimInterpolationWhitespaceOnly",
                "\${Recipient.v?trim}",
                mapOf("Recipient.v" to "   \t  "),
                ""
            ),
            ConformanceCase(
                "trimInterpolationNoWhitespace",
                "\${Recipient.v?trim}",
                mapOf("Recipient.v" to "hello"),
                "hello"
            ),

            // ── ?lower_case — interpolation ───────────────────────────────────
            ConformanceCase(
                "lowerCaseInterpolationAllCaps",
                "\${Recipient.v?lower_case}",
                mapOf("Recipient.v" to "HELLO WORLD"),
                "hello world"
            ),
            ConformanceCase(
                "lowerCaseInterpolationMixed",
                "\${Recipient.v?lower_case}",
                mapOf("Recipient.v" to "HeLLo WoRLd"),
                "hello world"
            ),
            ConformanceCase(
                "lowerCaseInterpolationAlreadyLower",
                "\${Recipient.v?lower_case}",
                mapOf("Recipient.v" to "hello"),
                "hello"
            ),
            ConformanceCase(
                "lowerCaseInterpolationEmpty",
                "\${Recipient.v?lower_case}",
                mapOf("Recipient.v" to ""),
                ""
            ),
            ConformanceCase(
                "lowerCaseInterpolationWithDigits",
                "\${Recipient.v?lower_case}",
                mapOf("Recipient.v" to "ABC123"),
                "abc123"
            ),

            // ── ?upper_case — interpolation ───────────────────────────────────
            ConformanceCase(
                "upperCaseInterpolationAllLower",
                "\${Recipient.v?upper_case}",
                mapOf("Recipient.v" to "hello world"),
                "HELLO WORLD"
            ),
            ConformanceCase(
                "upperCaseInterpolationMixed",
                "\${Recipient.v?upper_case}",
                mapOf("Recipient.v" to "HeLLo WoRLd"),
                "HELLO WORLD"
            ),
            ConformanceCase(
                "upperCaseInterpolationAlreadyUpper",
                "\${Recipient.v?upper_case}",
                mapOf("Recipient.v" to "HELLO"),
                "HELLO"
            ),
            ConformanceCase(
                "upperCaseInterpolationEmpty",
                "\${Recipient.v?upper_case}",
                mapOf("Recipient.v" to ""),
                ""
            ),

            // ── ?trim — condition (LHS) ───────────────────────────────────────
            ConformanceCase(
                "trimConditionLhsEqualsMatch",
                "<#if Recipient.v?trim == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "  gold  "),
                "yes"
            ),
            ConformanceCase(
                "trimConditionLhsEqualsNoMatch",
                "<#if Recipient.v?trim == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "  silver  "),
                "no"
            ),
            ConformanceCase(
                "trimConditionLhsNotEqualsMatch",
                "<#if Recipient.v?trim != \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "  silver  "),
                "yes"
            ),
            ConformanceCase(
                "trimConditionLhsEmptyStringVsEmpty",
                "<#if Recipient.v?trim == \"\">empty<#else>nonempty</#if>",
                mapOf("Recipient.v" to "   "),
                "empty"
            ),

            // ── ?lower_case — condition (LHS) ─────────────────────────────────
            ConformanceCase(
                "lowerCaseConditionLhsEqualsMatch",
                "<#if Recipient.v?lower_case == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "GOLD"),
                "yes"
            ),
            ConformanceCase(
                "lowerCaseConditionLhsEqualsNoMatch",
                "<#if Recipient.v?lower_case == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "SILVER"),
                "no"
            ),
            ConformanceCase(
                "lowerCaseConditionLhsNotEqualsMatch",
                "<#if Recipient.v?lower_case != \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "SILVER"),
                "yes"
            ),
            ConformanceCase(
                "lowerCaseConditionLhsNotEqualsNoMatch",
                "<#if Recipient.v?lower_case != \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "GOLD"),
                "no"
            ),

            // ── ?upper_case — condition (LHS) ─────────────────────────────────
            ConformanceCase(
                "upperCaseConditionLhsEqualsMatch",
                "<#if Recipient.v?upper_case == \"GOLD\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "gold"),
                "yes"
            ),
            ConformanceCase(
                "upperCaseConditionLhsEqualsNoMatch",
                "<#if Recipient.v?upper_case == \"GOLD\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "silver"),
                "no"
            ),
            ConformanceCase(
                "upperCaseConditionLhsNotEqualsMatch",
                "<#if Recipient.v?upper_case != \"GOLD\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "silver"),
                "yes"
            ),

            // ── ?trim — switch expression ──────────────────────────────────────
            ConformanceCase(
                "trimSwitchLeadingSpaces",
                "<#switch Recipient.v?trim><#case \"a\">A<#break><#default>D</#switch>",
                mapOf("Recipient.v" to "  a  "),
                "A"
            ),
            ConformanceCase(
                "trimSwitchNoMatch",
                "<#switch Recipient.v?trim><#case \"a\">A<#break><#default>D</#switch>",
                mapOf("Recipient.v" to "  b  "),
                "D"
            ),

            // ── ?lower_case — switch expression ───────────────────────────────
            ConformanceCase(
                "lowerCaseSwitchMatch",
                "<#switch Recipient.v?lower_case><#case \"gold\">Gold<#break><#default>Other</#switch>",
                mapOf("Recipient.v" to "GOLD"),
                "Gold"
            ),
            ConformanceCase(
                "lowerCaseSwitchNoMatch",
                "<#switch Recipient.v?lower_case><#case \"gold\">Gold<#break><#default>Other</#switch>",
                mapOf("Recipient.v" to "SILVER"),
                "Other"
            ),
            ConformanceCase(
                "lowerCaseSwitchMixedCaseInput",
                "<#switch Recipient.v?lower_case><#case \"gold\">Gold<#break><#case \"silver\">Silver<#break><#default>Other</#switch>",
                mapOf("Recipient.v" to "GoLd"),
                "Gold"
            ),

            // ── ?upper_case — switch expression ───────────────────────────────
            ConformanceCase(
                "upperCaseSwitchMatch",
                "<#switch Recipient.v?upper_case><#case \"GOLD\">Gold<#break><#default>Other</#switch>",
                mapOf("Recipient.v" to "gold"),
                "Gold"
            ),
            ConformanceCase(
                "upperCaseSwitchNoMatch",
                "<#switch Recipient.v?upper_case><#case \"GOLD\">Gold<#break><#default>Other</#switch>",
                mapOf("Recipient.v" to "silver"),
                "Other"
            ),

            // ── ?trim — concat operand ────────────────────────────────────────
            ConformanceCase(
                "trimConcatLeadingPart",
                "\${Recipient.a?trim + \" world\"}",
                mapOf("Recipient.a" to "  hello  "),
                "hello world"
            ),
            ConformanceCase(
                "trimConcatTrailingPart",
                "\${\"hello \" + Recipient.a?trim}",
                mapOf("Recipient.a" to "  world  "),
                "hello world"
            ),
            ConformanceCase(
                "trimConcatMiddlePart",
                "\${\"a \" + Recipient.v?trim + \" b\"}",
                mapOf("Recipient.v" to "  mid  "),
                "a mid b"
            ),

            // ── ?lower_case — concat operand ──────────────────────────────────
            ConformanceCase(
                "lowerCaseConcatLeadingPart",
                "\${Recipient.a?lower_case + \" world\"}",
                mapOf("Recipient.a" to "HELLO"),
                "hello world"
            ),
            ConformanceCase(
                "lowerCaseConcatTrailingPart",
                "\${\"hello \" + Recipient.a?lower_case}",
                mapOf("Recipient.a" to "WORLD"),
                "hello world"
            ),
            ConformanceCase(
                "lowerCaseConcatMiddlePart",
                "\${\"A \" + Recipient.v?lower_case + \" B\"}",
                mapOf("Recipient.v" to "MID"),
                "A mid B"
            ),

            // ── ?upper_case — concat operand ──────────────────────────────────
            ConformanceCase(
                "upperCaseConcatLeadingPart",
                "\${Recipient.a?upper_case + \" world\"}",
                mapOf("Recipient.a" to "hello"),
                "HELLO world"
            ),
            ConformanceCase(
                "upperCaseConcatTrailingPart",
                "\${\"hello \" + Recipient.a?upper_case}",
                mapOf("Recipient.a" to "world"),
                "hello WORLD"
            ),
            ConformanceCase(
                "upperCaseConcatMiddlePart",
                "\${\"a \" + Recipient.v?upper_case + \" b\"}",
                mapOf("Recipient.v" to "mid"),
                "a MID b"
            ),

            // ═════════════════════════════════════════════════════════════════
            // CHAINED TRANSFORM COMBOS
            // ═════════════════════════════════════════════════════════════════

            ConformanceCase(
                "trimThenLowerInterpolation",
                "\${Recipient.v?trim?lower_case}",
                mapOf("Recipient.v" to "  HELLO  "),
                "hello"
            ),
            ConformanceCase(
                "trimThenUpperInterpolation",
                "\${Recipient.v?trim?upper_case}",
                mapOf("Recipient.v" to "  hello  "),
                "HELLO"
            ),
            ConformanceCase(
                "lowerThenTrimInterpolation",
                "\${Recipient.v?lower_case?trim}",
                mapOf("Recipient.v" to "  HELLO  "),
                "hello"
            ),
            ConformanceCase(
                "upperThenTrimInterpolation",
                "\${Recipient.v?upper_case?trim}",
                mapOf("Recipient.v" to "  hello  "),
                "HELLO"
            ),
            ConformanceCase(
                "trimThenLowerInCondition",
                "<#if Recipient.v?trim?lower_case == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "  GOLD  "),
                "yes"
            ),
            ConformanceCase(
                "trimThenUpperInCondition",
                "<#if Recipient.v?trim?upper_case == \"GOLD\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "  gold  "),
                "yes"
            ),
            ConformanceCase(
                "lowerThenTrimInCondition",
                "<#if Recipient.v?lower_case?trim == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "  GOLD  "),
                "yes"
            ),
            ConformanceCase(
                "trimThenLowerInSwitch",
                "<#switch Recipient.v?trim?lower_case><#case \"gold\">Gold<#break><#default>Other</#switch>",
                mapOf("Recipient.v" to "  GOLD  "),
                "Gold"
            ),
            ConformanceCase(
                "trimThenUpperInSwitch",
                "<#switch Recipient.v?trim?upper_case><#case \"GOLD\">Gold<#break><#default>Other</#switch>",
                mapOf("Recipient.v" to "  gold  "),
                "Gold"
            ),
            ConformanceCase(
                "trimThenLowerInConcat",
                "\${Recipient.v?trim?lower_case + \"!\"}",
                mapOf("Recipient.v" to "  HELLO  "),
                "hello!"
            ),
            ConformanceCase(
                "trimThenUpperInConcat",
                "\${Recipient.v?trim?upper_case + \"!\"}",
                mapOf("Recipient.v" to "  hello  "),
                "HELLO!"
            ),
            ConformanceCase(
                "lowerThenBooleanChain",
                "<#if Recipient.v?lower_case?boolean>yes<#else>no</#if>",
                mapOf("Recipient.v" to "TRUE"),
                "yes"
            ),
            ConformanceCase(
                "trimThenLowerThenBooleanChain",
                "<#if Recipient.v?trim?lower_case?boolean>yes<#else>no</#if>",
                mapOf("Recipient.v" to "  TRUE  "),
                "yes"
            ),
            ConformanceCase(
                "lowerThenHasContentKeyPresent",
                "<#if Recipient.v?lower_case?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to "HELLO"),
                "yes"
            ),
            ConformanceCase(
                "lowerThenHasContentEmptyString",
                "<#if Recipient.v?lower_case?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to ""),
                "no"
            ),
            ConformanceCase(
                "upperThenHasContentKeyPresent",
                "<#if Recipient.v?upper_case?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to "hello"),
                "yes"
            ),
            ConformanceCase(
                "trimThenLowerTrimAgain",
                "<#if Recipient.v?trim?lower_case?trim == \"alice\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "  ALICE  "),
                "yes"
            ),

            // ── cross-chains: ?lower_case?upper_case and ?upper_case?lower_case ─
            ConformanceCase(
                "lowerThenUpperInterpolation",
                "\${Recipient.v?lower_case?upper_case}",
                mapOf("Recipient.v" to "HeLLo"),
                "HELLO"
            ),
            ConformanceCase(
                "upperThenLowerInterpolation",
                "\${Recipient.v?upper_case?lower_case}",
                mapOf("Recipient.v" to "HeLLo"),
                "hello"
            ),
            ConformanceCase(
                "lowerThenUpperInCondition",
                "<#if Recipient.v?lower_case?upper_case == \"GOLD\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "GoLd"),
                "yes"
            ),
            ConformanceCase(
                "upperThenLowerInCondition",
                "<#if Recipient.v?upper_case?lower_case == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "GoLd"),
                "yes"
            ),

            // ── ?lower_case?trim — missing contexts ───────────────────────────
            ConformanceCase(
                "lowerThenTrimSwitch",
                "<#switch Recipient.v?lower_case?trim><#case \"gold\">Gold<#break><#default>Other</#switch>",
                mapOf("Recipient.v" to "  GOLD  "),
                "Gold"
            ),
            ConformanceCase(
                "lowerThenTrimConcat",
                "\${Recipient.v?lower_case?trim + \"!\"}",
                mapOf("Recipient.v" to "  HELLO  "),
                "hello!"
            ),

            // ── ?upper_case?trim — missing contexts ───────────────────────────
            ConformanceCase(
                "upperThenTrimCondition",
                "<#if Recipient.v?upper_case?trim == \"GOLD\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "  gold  "),
                "yes"
            ),
            ConformanceCase(
                "upperThenTrimSwitch",
                "<#switch Recipient.v?upper_case?trim><#case \"GOLD\">Gold<#break><#default>Other</#switch>",
                mapOf("Recipient.v" to "  gold  "),
                "Gold"
            ),
            ConformanceCase(
                "upperThenTrimConcat",
                "\${Recipient.v?upper_case?trim + \"!\"}",
                mapOf("Recipient.v" to "  hello  "),
                "HELLO!"
            ),

            // ── ?trim?boolean and related terminal chains ─────────────────────
            ConformanceCase(
                "trimBooleanTrue",
                "<#if Recipient.v?trim?boolean>yes<#else>no</#if>",
                mapOf("Recipient.v" to "  true  "),
                "yes"
            ),
            ConformanceCase(
                "trimBooleanFalse",
                "<#if Recipient.v?trim?boolean>yes<#else>no</#if>",
                mapOf("Recipient.v" to "  false  "),
                "no"
            ),
            ConformanceCase(
                "lowerTrimBooleanChain",
                "<#if Recipient.v?lower_case?trim?boolean>yes<#else>no</#if>",
                mapOf("Recipient.v" to "  TRUE  "),
                "yes"
            ),

            // ── ?lower_case?number and ?upper_case?number ─────────────────────
            // Useful for normalising scientific notation: "1E3"?lower_case → "1e3" → 1000
            ConformanceCase(
                "lowerThenNumberInteger",
                "<#if Recipient.v?lower_case?number gt 100>yes<#else>no</#if>",
                mapOf("Recipient.v" to "150"),
                "yes"
            ),
            ConformanceCase(
                "lowerThenNumberScientificUpperE",
                "<#if Recipient.v?lower_case?number == 1000>yes<#else>no</#if>",
                mapOf("Recipient.v" to "1E3"),
                "yes"
            ),
            ConformanceCase(
                "upperThenNumberInteger",
                "<#if Recipient.v?upper_case?number gt 100>yes<#else>no</#if>",
                mapOf("Recipient.v" to "150"),
                "yes"
            ),
            ConformanceCase(
                "upperThenNumberScientificLowerE",
                "<#if Recipient.v?upper_case?number == 1000>yes<#else>no</#if>",
                mapOf("Recipient.v" to "1e3"),
                "yes"
            ),

            // ── 3-way chains ending in ?has_content ───────────────────────────
            ConformanceCase(
                "trimLowerHasContentPresent",
                "<#if Recipient.v?trim?lower_case?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to "  HELLO  "),
                "yes"
            ),
            ConformanceCase(
                "trimLowerHasContentEmpty",
                "<#if Recipient.v?trim?lower_case?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to ""),
                "no"
            ),
            ConformanceCase(
                "trimUpperHasContentPresent",
                "<#if Recipient.v?trim?upper_case?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to "  hello  "),
                "yes"
            ),
            ConformanceCase(
                "trimUpperHasContentWhitespaceOnly",
                "<#if Recipient.v?trim?upper_case?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to "   "),
                "no"
            ),
            ConformanceCase(
                "lowerTrimHasContentPresent",
                "<#if Recipient.v?lower_case?trim?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to "  HELLO  "),
                "yes"
            ),
            ConformanceCase(
                "upperTrimHasContentPresent",
                "<#if Recipient.v?upper_case?trim?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to "  hello  "),
                "yes"
            ),

            // ── ?lower_case with default on present key ───────────────────────
            ConformanceCase(
                "lowerCaseDefaultKeyPresent",
                "\${Recipient.v?lower_case!\"fallback\"}",
                mapOf("Recipient.v" to "GOLD"),
                "gold"
            ),
            ConformanceCase(
                "upperCaseDefaultKeyPresent",
                "\${Recipient.v?upper_case!\"fallback\"}",
                mapOf("Recipient.v" to "gold"),
                "GOLD"
            ),

            // ═════════════════════════════════════════════════════════════════
            // NUMERIC OPERATOR MATRIX — all 6 operators × value types
            // ═════════════════════════════════════════════════════════════════

            // ── gt ────────────────────────────────────────────────────────────
            ConformanceCase(
                "numericGtIntegerTrue",
                "<#if Recipient.v?number gt 10>yes<#else>no</#if>",
                mapOf("Recipient.v" to "11"),
                "yes"
            ),
            ConformanceCase(
                "numericGtIntegerFalseEqual",
                "<#if Recipient.v?number gt 10>yes<#else>no</#if>",
                mapOf("Recipient.v" to "10"),
                "no"
            ),
            ConformanceCase(
                "numericGtIntegerFalseBelow",
                "<#if Recipient.v?number gt 10>yes<#else>no</#if>",
                mapOf("Recipient.v" to "9"),
                "no"
            ),
            ConformanceCase(
                "numericGtFloat",
                "<#if Recipient.v?number gt 1.5>yes<#else>no</#if>",
                mapOf("Recipient.v" to "1.51"),
                "yes"
            ),
            ConformanceCase(
                "numericGtNegativeValues",
                "<#if Recipient.v?number gt -5>yes<#else>no</#if>",
                mapOf("Recipient.v" to "-3"),
                "yes"
            ),

            // ── gte ───────────────────────────────────────────────────────────
            ConformanceCase(
                "numericGteExact",
                "<#if Recipient.v?number gte 10>yes<#else>no</#if>",
                mapOf("Recipient.v" to "10"),
                "yes"
            ),
            ConformanceCase(
                "numericGteAbove",
                "<#if Recipient.v?number gte 10>yes<#else>no</#if>",
                mapOf("Recipient.v" to "11"),
                "yes"
            ),
            ConformanceCase(
                "numericGteBelow",
                "<#if Recipient.v?number gte 10>yes<#else>no</#if>",
                mapOf("Recipient.v" to "9"),
                "no"
            ),

            // ── lt ────────────────────────────────────────────────────────────
            ConformanceCase(
                "numericLtIntegerTrue",
                "<#if Recipient.v?number lt 10>yes<#else>no</#if>",
                mapOf("Recipient.v" to "9"),
                "yes"
            ),
            ConformanceCase(
                "numericLtIntegerFalseEqual",
                "<#if Recipient.v?number lt 10>yes<#else>no</#if>",
                mapOf("Recipient.v" to "10"),
                "no"
            ),
            ConformanceCase(
                "numericLtFloat",
                "<#if Recipient.v?number lt 1.5>yes<#else>no</#if>",
                mapOf("Recipient.v" to "1.49"),
                "yes"
            ),

            // ── lte ───────────────────────────────────────────────────────────
            ConformanceCase(
                "numericLteExact",
                "<#if Recipient.v?number lte 10>yes<#else>no</#if>",
                mapOf("Recipient.v" to "10"),
                "yes"
            ),
            ConformanceCase(
                "numericLteBelow",
                "<#if Recipient.v?number lte 10>yes<#else>no</#if>",
                mapOf("Recipient.v" to "9"),
                "yes"
            ),
            ConformanceCase(
                "numericLteAbove",
                "<#if Recipient.v?number lte 10>yes<#else>no</#if>",
                mapOf("Recipient.v" to "11"),
                "no"
            ),

            // ── == (numeric equality) ─────────────────────────────────────────
            ConformanceCase(
                "numericEqIntegerMatch",
                "<#if Recipient.v?number == 42>yes<#else>no</#if>",
                mapOf("Recipient.v" to "42"),
                "yes"
            ),
            ConformanceCase(
                "numericEqIntegerNoMatch",
                "<#if Recipient.v?number == 42>yes<#else>no</#if>",
                mapOf("Recipient.v" to "43"),
                "no"
            ),
            ConformanceCase(
                "numericEqFloatMatch",
                "<#if Recipient.v?number == 3.14>yes<#else>no</#if>",
                mapOf("Recipient.v" to "3.14"),
                "yes"
            ),
            ConformanceCase(
                "numericEqFloatNoMatch",
                "<#if Recipient.v?number == 3.14>yes<#else>no</#if>",
                mapOf("Recipient.v" to "3.15"),
                "no"
            ),
            ConformanceCase(
                "numericEqZero",
                "<#if Recipient.v?number == 0>yes<#else>no</#if>",
                mapOf("Recipient.v" to "0"),
                "yes"
            ),
            ConformanceCase(
                "numericEqNegative",
                "<#if Recipient.v?number == -1>yes<#else>no</#if>",
                mapOf("Recipient.v" to "-1"),
                "yes"
            ),
            ConformanceCase(
                "numericEqScientificNotation",
                "<#if Recipient.v?number == 1000>yes<#else>no</#if>",
                mapOf("Recipient.v" to "1e3"),
                "yes"
            ),

            // ── != (numeric not-equals) ───────────────────────────────────────
            ConformanceCase(
                "numericNeqMatch",
                "<#if Recipient.v?number != 42>yes<#else>no</#if>",
                mapOf("Recipient.v" to "43"),
                "yes"
            ),
            ConformanceCase(
                "numericNeqNoMatch",
                "<#if Recipient.v?number != 42>yes<#else>no</#if>",
                mapOf("Recipient.v" to "42"),
                "no"
            ),
            ConformanceCase(
                "numericNeqFloat",
                "<#if Recipient.v?number != 3.14>yes<#else>no</#if>",
                mapOf("Recipient.v" to "2.72"),
                "yes"
            ),
            ConformanceCase(
                "numericNeqZero",
                "<#if Recipient.v?number != 0>yes<#else>no</#if>",
                mapOf("Recipient.v" to "1"),
                "yes"
            ),

            // ── ?trim?number chains ───────────────────────────────────────────
            ConformanceCase(
                "trimNumberGtWhitespacePadded",
                "<#if Recipient.v?trim?number gt 100>yes<#else>no</#if>",
                mapOf("Recipient.v" to "  150  "),
                "yes"
            ),
            ConformanceCase(
                "trimNumberEqWhitespacePadded",
                "<#if Recipient.v?trim?number == 100>yes<#else>no</#if>",
                mapOf("Recipient.v" to "  100  "),
                "yes"
            ),
            ConformanceCase(
                "trimNumberLteWhitespacePadded",
                "<#if Recipient.v?trim?number lte 100>yes<#else>no</#if>",
                mapOf("Recipient.v" to "  99  "),
                "yes"
            ),

            // ═════════════════════════════════════════════════════════════════
            // LOGICAL OPERATOR COMBINATIONS
            // ═════════════════════════════════════════════════════════════════

            ConformanceCase(
                "andBothStringEqualities",
                "<#if Recipient.a == \"x\" && Recipient.b == \"y\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "x", "Recipient.b" to "y"),
                "yes"
            ),
            ConformanceCase(
                "andLeftFalseShortCircuits",
                "<#if Recipient.a == \"x\" && Recipient.b == \"y\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "z", "Recipient.b" to "y"),
                "no"
            ),
            ConformanceCase(
                "orBothFalse",
                "<#if Recipient.a == \"x\" || Recipient.b == \"y\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "z", "Recipient.b" to "z"),
                "no"
            ),
            ConformanceCase(
                "orLeftTrueShortCircuits",
                "<#if Recipient.a == \"x\" || Recipient.b == \"y\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "x", "Recipient.b" to "z"),
                "yes"
            ),
            ConformanceCase(
                "andWithNumericGt",
                "<#if Recipient.a == \"gold\" && Recipient.b?number gt 100>yes<#else>no</#if>",
                mapOf("Recipient.a" to "gold", "Recipient.b" to "200"),
                "yes"
            ),
            ConformanceCase(
                "andWithNumericGtFalse",
                "<#if Recipient.a == \"gold\" && Recipient.b?number gt 100>yes<#else>no</#if>",
                mapOf("Recipient.a" to "gold", "Recipient.b" to "50"),
                "no"
            ),
            ConformanceCase(
                "orWithNumericAndString",
                "<#if Recipient.a == \"vip\" || Recipient.b?number gte 1000>yes<#else>no</#if>",
                mapOf("Recipient.a" to "basic", "Recipient.b" to "1500"),
                "yes"
            ),
            ConformanceCase(
                "andWithTransformAndNumeric",
                "<#if Recipient.a?lower_case == \"gold\" && Recipient.b?number gt 0>yes<#else>no</#if>",
                mapOf("Recipient.a" to "GOLD", "Recipient.b" to "1"),
                "yes"
            ),
            ConformanceCase(
                "orWithTwoTransformConditions",
                "<#if Recipient.a?lower_case == \"gold\" || Recipient.a?lower_case == \"platinum\">premium<#else>standard</#if>",
                mapOf("Recipient.a" to "PLATINUM"),
                "premium"
            ),
            ConformanceCase(
                "andPrecedenceOverOr",
                "<#if Recipient.a == \"1\" || Recipient.b == \"1\" && Recipient.c == \"1\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "0", "Recipient.b" to "0", "Recipient.c" to "0"),
                "no"
            ),
            ConformanceCase(
                "parenOverridesAndPrecedence",
                "<#if (Recipient.a == \"1\" || Recipient.b == \"1\") && Recipient.c == \"1\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "1", "Recipient.b" to "0", "Recipient.c" to "0"),
                "no"
            ),
            ConformanceCase(
                "parenOverridesAndPrecedenceMatch",
                "<#if (Recipient.a == \"1\" || Recipient.b == \"1\") && Recipient.c == \"1\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "1", "Recipient.b" to "0", "Recipient.c" to "1"),
                "yes"
            ),
            ConformanceCase(
                "tripleAndAllTrue",
                "<#if Recipient.a == \"x\" && Recipient.b == \"y\" && Recipient.c == \"z\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "x", "Recipient.b" to "y", "Recipient.c" to "z"),
                "yes"
            ),
            ConformanceCase(
                "tripleAndMiddleFalse",
                "<#if Recipient.a == \"x\" && Recipient.b == \"y\" && Recipient.c == \"z\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "x", "Recipient.b" to "BAD", "Recipient.c" to "z"),
                "no"
            ),
            ConformanceCase(
                "negationInAnd",
                "<#if !Recipient.a?boolean && Recipient.b == \"x\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "false", "Recipient.b" to "x"),
                "yes"
            ),
            ConformanceCase(
                "negationInOr",
                "<#if !Recipient.a?boolean || Recipient.b == \"y\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "true", "Recipient.b" to "y"),
                "yes"
            ),
            ConformanceCase(
                "andWithExistsCheck",
                "<#if Recipient.a?? && Recipient.b == \"x\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "present", "Recipient.b" to "x"),
                "yes"
            ),
            ConformanceCase(
                "andWithExistsCheckMissing",
                "<#if Recipient.a?? && Recipient.b == \"x\">yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            ConformanceCase(
                "orWithHasContent",
                "<#if Recipient.a?has_content || Recipient.b == \"y\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "", "Recipient.b" to "y"),
                "yes"
            ),

            // ═════════════════════════════════════════════════════════════════
            // DEFAULT OPERATOR COMBINATIONS
            // ═════════════════════════════════════════════════════════════════

            ConformanceCase(
                "defaultKeyPresent",
                "\${Recipient.v!\"fallback\"}",
                mapOf("Recipient.v" to "hello"),
                "hello"
            ),
            ConformanceCase(
                "defaultKeyMissing",
                "\${Recipient.v!\"fallback\"}",
                emptyMap(),
                "fallback"
            ),
            ConformanceCase(
                "defaultEmptyStringDoesNotTrigger",
                "\${Recipient.v!\"fallback\"}",
                mapOf("Recipient.v" to ""),
                ""
            ),
            ConformanceCase(
                "defaultSingleQuoted",
                "\${Recipient.v!'fallback'}",
                emptyMap(),
                "fallback"
            ),
            ConformanceCase(
                "defaultInConditionParenKeyMissing",
                "<#if (Recipient.v!\"default\") == \"default\">yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "defaultInConditionParenKeyPresent",
                "<#if (Recipient.v!\"default\") == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "gold"),
                "yes"
            ),
            ConformanceCase(
                "bareDefaultKeyMissing",
                "\${Recipient.v!}",
                emptyMap(),
                ""
            ),
            ConformanceCase(
                "bareDefaultKeyPresent",
                "\${Recipient.v!}",
                mapOf("Recipient.v" to "hello"),
                "hello"
            ),
            ConformanceCase(
                "bareDefaultInConditionKeyMissing",
                "<#if Recipient.v! == \"\">yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "bareDefaultInConditionKeyPresent",
                "<#if Recipient.v! == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "gold"),
                "yes"
            ),
            ConformanceCase(
                "trimDefaultKeyPresent",
                "\${Recipient.v?trim!\"fallback\"}",
                mapOf("Recipient.v" to "  hello  "),
                "hello"
            ),
            ConformanceCase(
                "trimDefaultEmptyStringDoesNotTrigger",
                "\${Recipient.v?trim!\"fallback\"}",
                mapOf("Recipient.v" to ""),
                ""
            ),

            // ═════════════════════════════════════════════════════════════════
            // NESTING AND STRUCTURAL COMBINATIONS
            // ═════════════════════════════════════════════════════════════════

            ConformanceCase(
                "nestedIfBothTrue",
                "<#if Recipient.a == \"x\"><#if Recipient.b == \"y\">both<#else>aOnly</#if><#else>neither</#if>",
                mapOf("Recipient.a" to "x", "Recipient.b" to "y"),
                "both"
            ),
            ConformanceCase(
                "nestedIfInnerFalse",
                "<#if Recipient.a == \"x\"><#if Recipient.b == \"y\">both<#else>aOnly</#if><#else>neither</#if>",
                mapOf("Recipient.a" to "x", "Recipient.b" to "z"),
                "aOnly"
            ),
            ConformanceCase(
                "nestedIfOuterFalse",
                "<#if Recipient.a == \"x\"><#if Recipient.b == \"y\">both<#else>aOnly</#if><#else>neither</#if>",
                mapOf("Recipient.a" to "z", "Recipient.b" to "y"),
                "neither"
            ),
            ConformanceCase(
                "nestedIfWithTransformOuter",
                "<#if Recipient.a?lower_case == \"gold\"><#if Recipient.b?number gt 100>VIP<#else>Standard</#if><#else>Other</#if>",
                mapOf("Recipient.a" to "GOLD", "Recipient.b" to "200"),
                "VIP"
            ),
            ConformanceCase(
                "nestedIfWithTransformOuterBelowThreshold",
                "<#if Recipient.a?lower_case == \"gold\"><#if Recipient.b?number gt 100>VIP<#else>Standard</#if><#else>Other</#if>",
                mapOf("Recipient.a" to "GOLD", "Recipient.b" to "50"),
                "Standard"
            ),
            ConformanceCase(
                "switchWithNestedIf",
                "<#switch Recipient.a?upper_case><#case \"US\"><#if Recipient.b?number gt 0>US+<#else>US0</#if><#break><#default>Other</#switch>",
                mapOf("Recipient.a" to "us", "Recipient.b" to "1"),
                "US+"
            ),
            ConformanceCase(
                "switchWithNestedIfFalse",
                "<#switch Recipient.a?upper_case><#case \"US\"><#if Recipient.b?number gt 0>US+<#else>US0</#if><#break><#default>Other</#switch>",
                mapOf("Recipient.a" to "us", "Recipient.b" to "0"),
                "US0"
            ),
            ConformanceCase(
                "ifWithInterpolationAndTransformInBody",
                "<#if Recipient.active?boolean>Hello \${Recipient.name?trim?lower_case}!<#else>Inactive</#if>",
                mapOf("Recipient.active" to "true", "Recipient.name" to "  ALICE  "),
                "Hello alice!"
            ),
            ConformanceCase(
                "elseIfChainWithTransforms",
                "<#if Recipient.tier?lower_case == \"gold\">Gold<#elseif Recipient.tier?lower_case == \"silver\">Silver<#elseif Recipient.tier?lower_case == \"bronze\">Bronze<#else>Other</#if>",
                mapOf("Recipient.tier" to "BRONZE"),
                "Bronze"
            ),
            ConformanceCase(
                "elseIfChainFallsToElse",
                "<#if Recipient.tier?lower_case == \"gold\">Gold<#elseif Recipient.tier?lower_case == \"silver\">Silver<#else>Other</#if>",
                mapOf("Recipient.tier" to "PLATINUM"),
                "Other"
            ),
            ConformanceCase(
                "numericElseIfChain",
                "<#if Recipient.v?number gt 1000>High<#elseif Recipient.v?number gt 500>Mid<#elseif Recipient.v?number gt 0>Low<#else>Zero</#if>",
                mapOf("Recipient.v" to "750"),
                "Mid"
            ),
            ConformanceCase(
                "numericElseIfChainHigh",
                "<#if Recipient.v?number gt 1000>High<#elseif Recipient.v?number gt 500>Mid<#elseif Recipient.v?number gt 0>Low<#else>Zero</#if>",
                mapOf("Recipient.v" to "2000"),
                "High"
            ),
            ConformanceCase(
                "numericElseIfChainZero",
                "<#if Recipient.v?number gt 1000>High<#elseif Recipient.v?number gt 500>Mid<#elseif Recipient.v?number gt 0>Low<#else>Zero</#if>",
                mapOf("Recipient.v" to "0"),
                "Zero"
            ),

            // ═════════════════════════════════════════════════════════════════
            // EDGE CASE VALUES
            // ═════════════════════════════════════════════════════════════════

            ConformanceCase(
                "emptyStringEquality",
                "<#if Recipient.v == \"\">empty<#else>nonempty</#if>",
                mapOf("Recipient.v" to ""),
                "empty"
            ),
            ConformanceCase(
                "singleSpaceStringEquality",
                "<#if Recipient.v == \" \">space<#else>other</#if>",
                mapOf("Recipient.v" to " "),
                "space"
            ),
            ConformanceCase(
                "stringWithSpecialCharsEquality",
                "<#if Recipient.v == \"a&b\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "a&b"),
                "yes"
            ),
            ConformanceCase(
                "numericIntegerZero",
                "<#if Recipient.v?number == 0>zero<#else>nonzero</#if>",
                mapOf("Recipient.v" to "0"),
                "zero"
            ),
            ConformanceCase(
                "numericNegativeGtNegative",
                "<#if Recipient.v?number gt -10>yes<#else>no</#if>",
                mapOf("Recipient.v" to "-5"),
                "yes"
            ),
            ConformanceCase(
                "numericLargeInt",
                "<#if Recipient.v?number gt 999999>big<#else>small</#if>",
                mapOf("Recipient.v" to "1000000"),
                "big"
            ),
            ConformanceCase(
                "stringWithGtCharInValue",
                "<#if Recipient.v == \"a>b\">yes<#else>no</#if>",
                mapOf("Recipient.v" to "a>b"),
                "yes"
            ),
            ConformanceCase(
                "lowerCaseWithDigitsAndSymbols",
                "\${Recipient.v?lower_case}",
                mapOf("Recipient.v" to "Hello-World_123"),
                "hello-world_123"
            ),
            ConformanceCase(
                "upperCaseWithDigitsAndSymbols",
                "\${Recipient.v?upper_case}",
                mapOf("Recipient.v" to "Hello-World_123"),
                "HELLO-WORLD_123"
            ),
            ConformanceCase(
                "booleanLowerTrue",
                "<#if Recipient.v?boolean>yes<#else>no</#if>",
                mapOf("Recipient.v" to "true"),
                "yes"
            ),
            ConformanceCase(
                "booleanLowerFalse",
                "<#if Recipient.v?boolean>yes<#else>no</#if>",
                mapOf("Recipient.v" to "false"),
                "no"
            ),
            ConformanceCase(
                "existsCheckOnPresentEmptyString",
                "<#if Recipient.v??>yes<#else>no</#if>",
                mapOf("Recipient.v" to ""),
                "yes"
            ),
            ConformanceCase(
                "existsCheckOnMissing",
                "<#if Recipient.v??>yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            ConformanceCase(
                "hasContentEmptyString",
                "<#if Recipient.v?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to ""),
                "no"
            ),
            ConformanceCase(
                "hasContentWhitespaceOnly",
                "<#if Recipient.v?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to "   "),
                "yes"
            ),
            ConformanceCase(
                "trimHasContentWhitespaceOnlyFalse",
                "<#if Recipient.v?trim?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to "   "),
                "no"
            ),
            ConformanceCase(
                "trimHasContentNonEmpty",
                "<#if Recipient.v?trim?has_content>yes<#else>no</#if>",
                mapOf("Recipient.v" to "  hello  "),
                "yes"
            ),

            // ═════════════════════════════════════════════════════════════════
            // CONCAT EDGE CASES
            // ═════════════════════════════════════════════════════════════════

            ConformanceCase(
                "concatThreeParts",
                "\${Recipient.a + \" \" + Recipient.b}",
                mapOf("Recipient.a" to "hello", "Recipient.b" to "world"),
                "hello world"
            ),
            ConformanceCase(
                "concatFourParts",
                "\${Recipient.a + \", \" + Recipient.b + \"!\"}",
                mapOf("Recipient.a" to "hello", "Recipient.b" to "world"),
                "hello, world!"
            ),
            ConformanceCase(
                "concatLiteralOnly",
                "\${\"hello\" + \" \" + \"world\"}",
                emptyMap(),
                "hello world"
            ),
            ConformanceCase(
                "concatWithLowerAndUpper",
                "\${Recipient.a?lower_case + \"-\" + Recipient.b?upper_case}",
                mapOf("Recipient.a" to "HELLO", "Recipient.b" to "world"),
                "hello-WORLD"
            ),
            ConformanceCase(
                "concatWithTrimBothSides",
                "\${Recipient.a?trim + \"-\" + Recipient.b?trim}",
                mapOf("Recipient.a" to "  hello  ", "Recipient.b" to "  world  "),
                "hello-world"
            ),
            ConformanceCase(
                "concatWithTransformAndLiteral",
                "\${\"greeting: \" + Recipient.name?trim?lower_case + \"!\"}",
                mapOf("Recipient.name" to "  ALICE  "),
                "greeting: alice!"
            ),

            // ═════════════════════════════════════════════════════════════════
            // MIXED REAL-WORLD STYLE TEMPLATES
            // ═════════════════════════════════════════════════════════════════

            ConformanceCase(
                "realWorldTierAndPoints",
                "<#if Recipient.tier?lower_case == \"gold\" && Recipient.points?number gte 500>Premium Gold<#elseif Recipient.tier?lower_case == \"gold\">Gold<#else>Standard</#if>",
                mapOf("Recipient.tier" to "GOLD", "Recipient.points" to "600"),
                "Premium Gold"
            ),
            ConformanceCase(
                "realWorldTierAndPointsGoldLowPoints",
                "<#if Recipient.tier?lower_case == \"gold\" && Recipient.points?number gte 500>Premium Gold<#elseif Recipient.tier?lower_case == \"gold\">Gold<#else>Standard</#if>",
                mapOf("Recipient.tier" to "GOLD", "Recipient.points" to "100"),
                "Gold"
            ),
            ConformanceCase(
                "realWorldTierAndPointsStandard",
                "<#if Recipient.tier?lower_case == \"gold\" && Recipient.points?number gte 500>Premium Gold<#elseif Recipient.tier?lower_case == \"gold\">Gold<#else>Standard</#if>",
                mapOf("Recipient.tier" to "BRONZE", "Recipient.points" to "1000"),
                "Standard"
            ),
            ConformanceCase(
                "realWorldPersonalisedGreeting",
                "<#if Recipient.name?has_content>Hello \${Recipient.name?trim}<#else>Hello there</#if>",
                mapOf("Recipient.name" to "  Alice  "),
                "Hello Alice"
            ),
            ConformanceCase(
                "realWorldPersonalisedGreetingMissing",
                "<#if Recipient.name?has_content>Hello \${Recipient.name?trim}<#else>Hello there</#if>",
                mapOf("Recipient.name" to ""),
                "Hello there"
            ),
            ConformanceCase(
                "realWorldCountrySwitch",
                "<#switch Recipient.country?upper_case><#case \"US\">United States<#break><#case \"UK\">United Kingdom<#break><#case \"CA\">Canada<#break><#default>International</#switch>",
                mapOf("Recipient.country" to "ca"),
                "Canada"
            ),
            ConformanceCase(
                "realWorldCountrySwitchDefault",
                "<#switch Recipient.country?upper_case><#case \"US\">United States<#break><#case \"UK\">United Kingdom<#break><#case \"CA\">Canada<#break><#default>International</#switch>",
                mapOf("Recipient.country" to "de"),
                "International"
            ),
            ConformanceCase(
                "realWorldOptInStatus",
                "<#if Recipient.opt_in?trim?lower_case?boolean>Subscribed<#else>Not subscribed</#if>",
                mapOf("Recipient.opt_in" to "  TRUE  "),
                "Subscribed"
            ),

            // ═════════════════════════════════════════════════════════════════
            // PARENTHESIZED DEFAULT × LOGICAL OPERATORS
            // ═════════════════════════════════════════════════════════════════

            // ── (key!"default")?boolean in && / || / ! ────────────────────────
            ConformanceCase(
                "parenDefaultBooleanAndKeyAbsent",
                "<#if (Recipient.flag!\"false\")?boolean && Recipient.tier == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.tier" to "gold"),
                "no"
            ),
            ConformanceCase(
                "parenDefaultBooleanAndKeyPresent",
                "<#if (Recipient.flag!\"false\")?boolean && Recipient.tier == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.flag" to "true", "Recipient.tier" to "gold"),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultBooleanOrKeyAbsentTrueDefault",
                "<#if (Recipient.flag!\"true\")?boolean || Recipient.tier == \"gold\">yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultBooleanNegateKeyAbsentFalseDefault",
                "<#if !(Recipient.flag!\"false\")?boolean>yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultBooleanNegateKeyPresentTrue",
                "<#if !(Recipient.flag!\"false\")?boolean>yes<#else>no</#if>",
                mapOf("Recipient.flag" to "true"),
                "no"
            ),

            // ── (key!"default")?number in && / || ────────────────────────────
            ConformanceCase(
                "parenDefaultNumberGtKeyAbsentFalse",
                "<#if (Recipient.points!\"0\")?number gt 100>yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            ConformanceCase(
                "parenDefaultNumberGteKeyAbsentTrue",
                "<#if (Recipient.points!\"100\")?number gte 100>yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultNumberGtKeyPresentOverrides",
                "<#if (Recipient.points!\"0\")?number gt 100>yes<#else>no</#if>",
                mapOf("Recipient.points" to "200"),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultNumberGteAndStringKeyAbsent",
                "<#if (Recipient.points!\"0\")?number gte 500 && Recipient.tier == \"gold\">VIP<#else>other</#if>",
                mapOf("Recipient.tier" to "gold"),
                "other"
            ),

            // ── (key!"default")?has_content in && / || ───────────────────────
            ConformanceCase(
                "parenDefaultHasContentKeyAbsentEmptyDefault",
                "<#if (Recipient.promo!\"\")?has_content>yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            ConformanceCase(
                "parenDefaultHasContentKeyAbsentNonEmptyDefault",
                "<#if (Recipient.promo!\"SAVE10\")?has_content>yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultHasContentKeyPresentEmpty",
                "<#if (Recipient.promo!\"SAVE10\")?has_content>yes<#else>no</#if>",
                mapOf("Recipient.promo" to ""),
                "no"
            ),
            ConformanceCase(
                "parenDefaultHasContentAndConditionBothTrue",
                "<#if (Recipient.promo!\"\")?has_content && Recipient.tier == \"gold\">show<#else>hide</#if>",
                mapOf("Recipient.promo" to "SAVE20", "Recipient.tier" to "gold"),
                "show"
            ),
            ConformanceCase(
                "parenDefaultHasContentOrFallsToOther",
                "<#if (Recipient.promo!\"\")?has_content || Recipient.vip == \"true\">show<#else>hide</#if>",
                mapOf("Recipient.vip" to "true"),
                "show"
            ),

            // ═════════════════════════════════════════════════════════════════
            // SWITCH WITH DEFAULT OPERATOR
            // ═════════════════════════════════════════════════════════════════

            ConformanceCase(
                "switchDefaultKeyAbsentMatchesCase",
                "<#switch Recipient.tier!\"free\"><#case \"free\">Free<#break><#case \"pro\">Pro<#break><#default>Other</#switch>",
                emptyMap(),
                "Free"
            ),
            ConformanceCase(
                "switchDefaultKeyAbsentNoMatchHitsDefault",
                "<#switch Recipient.tier!\"basic\"><#case \"free\">Free<#break><#case \"pro\">Pro<#break><#default>Other</#switch>",
                emptyMap(),
                "Other"
            ),
            ConformanceCase(
                "switchDefaultKeyAbsentNoMatchNoDefault",
                "<#switch Recipient.tier!\"vip\"><#case \"free\">Free<#break><#case \"pro\">Pro<#break></#switch>",
                emptyMap(),
                ""
            ),
            ConformanceCase(
                "switchDefaultKeyPresentOverridesDefault",
                "<#switch Recipient.tier!\"free\"><#case \"free\">Free<#break><#case \"pro\">Pro<#break><#default>Other</#switch>",
                mapOf("Recipient.tier" to "pro"),
                "Pro"
            ),
            ConformanceCase(
                "switchDefaultKeyPresentNoMatch",
                "<#switch Recipient.tier!\"free\"><#case \"free\">Free<#break><#case \"pro\">Pro<#break><#default>Other</#switch>",
                mapOf("Recipient.tier" to "enterprise"),
                "Other"
            ),
            ConformanceCase(
                "switchDefaultWithInterpolationInBody",
                "<#switch Recipient.tier!\"guest\"><#case \"guest\">Hello Guest<#break><#case \"member\">Hello \${Recipient.name}<#break></#switch>",
                mapOf("Recipient.name" to "Alice"),
                "Hello Guest"
            ),

            // ═════════════════════════════════════════════════════════════════
            // EXISTS CHECK (??) IN <#ELSEIF>
            // ═════════════════════════════════════════════════════════════════

            ConformanceCase(
                "elseIfExistsCheckPresent",
                "<#if Recipient.a == \"x\">A<#elseif Recipient.b??>B<#else>C</#if>",
                mapOf("Recipient.a" to "y", "Recipient.b" to "anything"),
                "B"
            ),
            ConformanceCase(
                "elseIfExistsCheckMissing",
                "<#if Recipient.a == \"x\">A<#elseif Recipient.b??>B<#else>C</#if>",
                mapOf("Recipient.a" to "y"),
                "C"
            ),
            ConformanceCase(
                "elseIfNegatedExistsCheckMissing",
                "<#if Recipient.a == \"x\">A<#elseif !Recipient.b??>B<#else>C</#if>",
                mapOf("Recipient.a" to "y"),
                "B"
            ),
            ConformanceCase(
                "elseIfExistsWithAnd",
                "<#if Recipient.a == \"x\">A<#elseif Recipient.b?? && Recipient.c == \"z\">BC<#else>C</#if>",
                mapOf("Recipient.a" to "y", "Recipient.b" to "present", "Recipient.c" to "z"),
                "BC"
            ),

            // ═════════════════════════════════════════════════════════════════
            // CONCATENATION WITHOUT SPACES AROUND +
            // ═════════════════════════════════════════════════════════════════

            ConformanceCase(
                "concatNoSpacesTwoParts",
                "\${Recipient.a+Recipient.b}",
                mapOf("Recipient.a" to "foo", "Recipient.b" to "bar"),
                "foobar"
            ),
            ConformanceCase(
                "concatNoSpacesPropertyAndLiteral",
                "\${Recipient.a+\"!\"}",
                mapOf("Recipient.a" to "hello"),
                "hello!"
            ),
            ConformanceCase(
                "concatMixedSpacing",
                "\${Recipient.a+Recipient.b + Recipient.c}",
                mapOf("Recipient.a" to "a", "Recipient.b" to "b", "Recipient.c" to "c"),
                "abc"
            ),
            ConformanceCase(
                "concatNoSpacesWithTransform",
                "\${Recipient.a?upper_case+Recipient.b?lower_case}",
                mapOf("Recipient.a" to "hello", "Recipient.b" to "WORLD"),
                "HELLOworld"
            ),

            // ═════════════════════════════════════════════════════════════════
            // REAL-WORLD TEMPLATES USING NEW FEATURES
            // ═════════════════════════════════════════════════════════════════

            ConformanceCase(
                "realWorldSwitchWithDefault",
                "<#switch Recipient.plan!\"starter\"><#case \"starter\">Free plan<#break><#case \"pro\">Pro plan<#break><#default>Unknown plan</#switch>",
                emptyMap(),
                "Free plan"
            ),
            ConformanceCase(
                "realWorldParenDefaultBooleanFlag",
                "<#if (Recipient.opted_in!\"false\")?boolean>Exclusive offer inside!<#else>Subscribe for exclusive offers.</#if>",
                emptyMap(),
                "Subscribe for exclusive offers."
            ),
            ConformanceCase(
                "realWorldParenDefaultNumberDiscount",
                "<#if (Recipient.discount!\"0\")?number gt 0>Save \${Recipient.discount}%!<#else>No discount available.</#if>",
                emptyMap(),
                "No discount available."
            ),
            ConformanceCase(
                "realWorldParenDefaultHasContentGreeting",
                "<#if (Recipient.first_name!\"\")?has_content>Dear \${Recipient.first_name}<#else>Dear Customer</#if>",
                emptyMap(),
                "Dear Customer"
            ),
            ConformanceCase(
                "realWorldCombinedDefaultsEliteGold",
                "<#if (Recipient.tier!\"bronze\") == \"gold\" && (Recipient.points!\"0\")?number gte 500>Elite Gold<#elseif (Recipient.tier!\"bronze\") == \"gold\">Gold<#else>Standard</#if>",
                mapOf("Recipient.tier" to "gold", "Recipient.points" to "600"),
                "Elite Gold"
            ),
            ConformanceCase(
                "realWorldCombinedDefaultsGold",
                "<#if (Recipient.tier!\"bronze\") == \"gold\" && (Recipient.points!\"0\")?number gte 500>Elite Gold<#elseif (Recipient.tier!\"bronze\") == \"gold\">Gold<#else>Standard</#if>",
                mapOf("Recipient.tier" to "gold", "Recipient.points" to "200"),
                "Gold"
            ),
            ConformanceCase(
                "realWorldCombinedDefaultsStandard",
                "<#if (Recipient.tier!\"bronze\") == \"gold\" && (Recipient.points!\"0\")?number gte 500>Elite Gold<#elseif (Recipient.tier!\"bronze\") == \"gold\">Gold<#else>Standard</#if>",
                emptyMap(),
                "Standard"
            ),
            // ── Parenthesized default with trailing transform in equality ─────────
            ConformanceCase(
                "parenDefaultTransformLowerCaseMatchKeyAbsent",
                "<#if (Recipient.tier!\"bronze\")?lower_case == \"gold\">yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            ConformanceCase(
                "parenDefaultTransformLowerCaseMatchKeyPresent",
                "<#if (Recipient.tier!\"bronze\")?lower_case == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.tier" to "GOLD"),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultTransformLowerCaseNoMatchKeyPresent",
                "<#if (Recipient.tier!\"bronze\")?lower_case == \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.tier" to "SILVER"),
                "no"
            ),
            ConformanceCase(
                "parenDefaultTransformUpperCaseMatchKeyAbsent",
                "<#if (Recipient.status!\"active\")?upper_case == \"ACTIVE\">yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultTransformTrimMatchKeyAbsent",
                "<#if (Recipient.code!\"  vip  \")?trim == \"vip\">yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultTransformLowerCaseWithAndKeyAbsent",
                "<#if (Recipient.tier!\"bronze\")?lower_case == \"gold\" && Recipient.points == \"500\">VIP<#else>other</#if>",
                emptyMap(),
                "other"
            ),
            ConformanceCase(
                "parenDefaultTransformLowerCaseWithAndBothMatch",
                "<#if (Recipient.tier!\"bronze\")?lower_case == \"gold\" && Recipient.points == \"500\">VIP<#else>other</#if>",
                mapOf("Recipient.tier" to "GOLD", "Recipient.points" to "500"),
                "VIP"
            ),
            ConformanceCase(
                "parenDefaultTransformInElseIf",
                "<#if Recipient.a == \"x\">A<#elseif (Recipient.tier!\"bronze\")?lower_case == \"gold\">Gold<#else>other</#if>",
                mapOf("Recipient.a" to "y", "Recipient.tier" to "GOLD"),
                "Gold"
            ),
            // ── (key!"default")?transform in ${} interpolation ──────────────────
            ConformanceCase(
                "parenDefaultInterpolationUpperCaseKeyAbsent",
                "\${(Recipient.name!\"guest\")?upper_case}",
                emptyMap(),
                "GUEST"
            ),
            ConformanceCase(
                "parenDefaultInterpolationUpperCaseKeyPresent",
                "\${(Recipient.name!\"guest\")?upper_case}",
                mapOf("Recipient.name" to "alice"),
                "ALICE"
            ),
            ConformanceCase(
                "parenDefaultInterpolationLowerCaseKeyAbsent",
                "\${(Recipient.status!\"ACTIVE\")?lower_case}",
                emptyMap(),
                "active"
            ),
            ConformanceCase(
                "parenDefaultInterpolationTrimKeyAbsent",
                "\${(Recipient.code!\"  vip  \")?trim}",
                emptyMap(),
                "vip"
            ),
            ConformanceCase(
                "parenDefaultInterpolationTrimKeyPresent",
                "\${(Recipient.code!\"  vip  \")?trim}",
                mapOf("Recipient.code" to "  gold  "),
                "gold"
            ),
            ConformanceCase(
                "parenDefaultInterpolationNoTransformKeyAbsent",
                "\${(Recipient.name!\"Guest\")}",
                emptyMap(),
                "Guest"
            ),
            ConformanceCase(
                "parenDefaultInterpolationChainedTransformKeyAbsent",
                "\${(Recipient.name!\"  hello  \")?trim?upper_case}",
                emptyMap(),
                "HELLO"
            ),
            // ── bare ?boolean without transforms ─────────────────────────────────
            ConformanceCase(
                "bareBooleanTrueKeyPresent",
                "<#if Recipient.flag?boolean>yes<#else>no</#if>",
                mapOf("Recipient.flag" to "true"),
                "yes"
            ),
            ConformanceCase(
                "bareBooleanFalseKeyPresent",
                "<#if Recipient.flag?boolean>yes<#else>no</#if>",
                mapOf("Recipient.flag" to "false"),
                "no"
            ),
            ConformanceCase(
                "bareBooleanWithDefaultTrueKeyAbsent",
                "<#if (Recipient.flag!\"true\")?boolean>yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "bareBooleanWithDefaultFalseKeyAbsent",
                "<#if (Recipient.flag!\"false\")?boolean>yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            // ── (key!"default") with != operator ─────────────────────────────────
            ConformanceCase(
                "parenDefaultNotEqualsKeyAbsentMatchesDefault",
                "<#if (Recipient.tier!\"bronze\") != \"gold\">yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultNotEqualsKeyAbsentDefaultIsValue",
                "<#if (Recipient.tier!\"gold\") != \"gold\">yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            ConformanceCase(
                "parenDefaultNotEqualsKeyPresent",
                "<#if (Recipient.tier!\"bronze\") != \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.tier" to "silver"),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultTransformNotEqualsKeyAbsent",
                "<#if (Recipient.tier!\"bronze\")?lower_case != \"gold\">yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultTransformNotEqualsKeyPresentMatchesAfterTransform",
                "<#if (Recipient.tier!\"bronze\")?lower_case != \"gold\">yes<#else>no</#if>",
                mapOf("Recipient.tier" to "GOLD"),
                "no"
            ),

            // ── (key!"default")?number with lt / lte / != ─────────────────────
            ConformanceCase(
                "parenDefaultNumberLtKeyAbsentTrue",
                "<#if (Recipient.points!\"50\")?number lt 100>yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultNumberLtKeyAbsentFalse",
                "<#if (Recipient.points!\"150\")?number lt 100>yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            ConformanceCase(
                "parenDefaultNumberLtKeyPresentOverrides",
                "<#if (Recipient.points!\"200\")?number lt 100>yes<#else>no</#if>",
                mapOf("Recipient.points" to "50"),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultNumberLteKeyAbsentTrue",
                "<#if (Recipient.points!\"100\")?number lte 100>yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultNumberLteKeyAbsentFalse",
                "<#if (Recipient.points!\"101\")?number lte 100>yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            ConformanceCase(
                "parenDefaultNumberNotEqualsKeyAbsentTrue",
                "<#if (Recipient.points!\"0\")?number != 5>yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultNumberNotEqualsKeyAbsentDefaultMatchesRhs",
                "<#if (Recipient.points!\"5\")?number != 5>yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            ConformanceCase(
                "parenDefaultNumberNotEqualsKeyPresent",
                "<#if (Recipient.points!\"0\")?number != 5>yes<#else>no</#if>",
                mapOf("Recipient.points" to "10"),
                "yes"
            ),

            // ── ?? with || ────────────────────────────────────────────────────
            ConformanceCase(
                "existsCheckOrFalse",
                "<#if Recipient.a?? || Recipient.b??>yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            ConformanceCase(
                "existsCheckOrLeftTrue",
                "<#if Recipient.a?? || Recipient.b??>yes<#else>no</#if>",
                mapOf("Recipient.a" to "x"),
                "yes"
            ),
            ConformanceCase(
                "existsCheckOrRightTrue",
                "<#if Recipient.a?? || Recipient.b??>yes<#else>no</#if>",
                mapOf("Recipient.b" to "x"),
                "yes"
            ),
            ConformanceCase(
                "existsCheckOrBothTrue",
                "<#if Recipient.a?? || Recipient.b??>yes<#else>no</#if>",
                mapOf("Recipient.a" to "x", "Recipient.b" to "y"),
                "yes"
            ),

            // ── multiple ?? in same && condition ──────────────────────────────
            ConformanceCase(
                "doubleExistsCheckBothPresent",
                "<#if Recipient.a?? && Recipient.b??>yes<#else>no</#if>",
                mapOf("Recipient.a" to "x", "Recipient.b" to "y"),
                "yes"
            ),
            ConformanceCase(
                "doubleExistsCheckFirstMissing",
                "<#if Recipient.a?? && Recipient.b??>yes<#else>no</#if>",
                mapOf("Recipient.b" to "y"),
                "no"
            ),
            ConformanceCase(
                "doubleExistsCheckSecondMissing",
                "<#if Recipient.a?? && Recipient.b??>yes<#else>no</#if>",
                mapOf("Recipient.a" to "x"),
                "no"
            ),
            ConformanceCase(
                "doubleExistsCheckBothMissing",
                "<#if Recipient.a?? && Recipient.b??>yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),

            // ── switch + default + transform combined ─────────────────────────
            ConformanceCase(
                "switchDefaultWithTransformKeyAbsent",
                "<#switch Recipient.tier!\"BRONZE\"?lower_case><#case \"bronze\">Bronze<#break><#case \"gold\">Gold<#break><#default>Other</#switch>",
                emptyMap(),
                "Bronze"
            ),
            ConformanceCase(
                "switchDefaultWithTransformKeyPresentAlreadyNormalized",
                "<#switch Recipient.tier!\"BRONZE\"?lower_case><#case \"bronze\">Bronze<#break><#case \"gold\">Gold<#break><#default>Other</#switch>",
                mapOf("Recipient.tier" to "gold"),
                "Gold"
            ),
            ConformanceCase(
                "switchDefaultWithTransformKeyAbsentNoCase",
                "<#switch Recipient.tier!\"VIP\"?lower_case><#case \"bronze\">Bronze<#break><#case \"gold\">Gold<#break><#default>Other</#switch>",
                emptyMap(),
                "Other"
            ),

            // ── concat with paren-default operands ────────────────────────────
            ConformanceCase(
                "concatParenDefaultBothAbsent",
                "\${(Recipient.first!\"J\") + (Recipient.last!\"Doe\")}",
                emptyMap(),
                "JDoe"
            ),
            ConformanceCase(
                "concatParenDefaultFirstPresent",
                "\${(Recipient.first!\"J\") + (Recipient.last!\"Doe\")}",
                mapOf("Recipient.first" to "Alice"),
                "AliceDoe"
            ),
            ConformanceCase(
                "concatParenDefaultBothPresent",
                "\${(Recipient.first!\"J\") + (Recipient.last!\"Doe\")}",
                mapOf("Recipient.first" to "Alice", "Recipient.last" to "Smith"),
                "AliceSmith"
            ),
            ConformanceCase(
                "concatParenDefaultWithTransform",
                "\${(Recipient.first!\"hello\")?upper_case + \" \" + (Recipient.last!\"world\")?upper_case}",
                emptyMap(),
                "HELLO WORLD"
            ),

            // ── two paren-defaults combined with || ───────────────────────────
            ConformanceCase(
                "parenDefaultOrBothAbsentBothMatch",
                "<#if (Recipient.a!\"x\") == \"x\" || (Recipient.b!\"y\") == \"y\">yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultOrBothAbsentNeitherMatch",
                "<#if (Recipient.a!\"z\") == \"x\" || (Recipient.b!\"w\") == \"y\">yes<#else>no</#if>",
                emptyMap(),
                "no"
            ),
            ConformanceCase(
                "parenDefaultOrBothAbsentSecondMatch",
                "<#if (Recipient.a!\"z\") == \"x\" || (Recipient.b!\"y\") == \"y\">yes<#else>no</#if>",
                emptyMap(),
                "yes"
            ),
            ConformanceCase(
                "parenDefaultOrFirstPresentMatchesRhs",
                "<#if (Recipient.a!\"z\") == \"x\" || (Recipient.b!\"w\") == \"y\">yes<#else>no</#if>",
                mapOf("Recipient.a" to "x"),
                "yes"
            ),
        )
    }

    private lateinit var fmConfig: Configuration

    @Before
    fun setUp() {
        fmConfig = Configuration(Configuration.VERSION_2_3_23)
        fmConfig.defaultEncoding = "UTF-8"
    }

    private fun fmEval(template: String, props: Map<String, String>): String =
        SwrveFreemarkerConformanceTest.fmEval(template, props, fmConfig)

    private fun sdkEval(template: String, props: Map<String, String>): String =
        SwrveFreemarkerEvaluator.evaluate(template, props)

    private fun assertBothSuppress(template: String, props: Map<String, String> = emptyMap()) {
        try { sdkEval(template, props); error("Expected SDK to throw for: $template") } catch (_: FreemarkerException) {}
        try { fmEval(template, props) } catch (_: Exception) { return }
        error("Expected FreeMarker to throw for: $template")
    }

    private fun assertConforms(c: ConformanceCase) {
        val sdkResult = sdkEval(c.template, c.props)
        val fmResult = fmEval(c.template, c.props)
        assertEquals("SDK output mismatch for '${c.name}'", c.expected, sdkResult)
        assertEquals("FreeMarker output mismatch for '${c.name}'", c.expected, fmResult)
        assertEquals(
            "SDK and FreeMarker diverge for '${c.name}'\n  template: ${c.template}\n  props: ${c.props}\n  sdk=$sdkResult  fm=$fmResult",
            fmResult, sdkResult
        )
    }

    /**
     * JUnit 4 has no built-in xfail/@ExpectedToFail annotation. This helper acts as one:
     * the test PASSES when SDK and FreeMarker diverge (either FreeMarker throws or returns a
     * different value), and FAILS if they agree — which would mean the divergence has been
     * fixed and the test should be promoted to a normal assertConforms call.
     *
     * [sdkExpected] pins down the SDK's specific expected output so regressions in SDK
     * behaviour are still caught even while the FreeMarker divergence exists.
     */
    private fun assertDiverges(sdkExpected: String, template: String, props: Map<String, String> = emptyMap()) {
        val sdkResult = sdkEval(template, props)
        assertEquals("SDK output changed for divergence case (template: $template)", sdkExpected, sdkResult)

        val fmResult = try {
            fmEval(template, props)
        } catch (e: TemplateException) {
            return // FreeMarker evaluation error — divergence confirmed, test passes
        }

        assertNotEquals(
            "Divergence no longer exists — SDK and FreeMarker now agree on '$template' (result: '$fmResult'). Promote to assertConforms.",
            sdkResult, fmResult
        )
    }

    private fun case(name: String): ConformanceCase =
        TEST_CASES.firstOrNull { it.name == name } ?: error("No ConformanceCase named '$name'")

    // ── Transform matrix: ?trim ───────────────────────────────────────────────

    @Test fun testTrimInterpolationLeadingTrailing() = assertConforms(case("trimInterpolationLeadingTrailing"))
    @Test fun testTrimInterpolationEmptyString() = assertConforms(case("trimInterpolationEmptyString"))
    @Test fun testTrimInterpolationWhitespaceOnly() = assertConforms(case("trimInterpolationWhitespaceOnly"))
    @Test fun testTrimInterpolationNoWhitespace() = assertConforms(case("trimInterpolationNoWhitespace"))

    // ── Transform matrix: ?lower_case ────────────────────────────────────────

    @Test fun testLowerCaseInterpolationAllCaps() = assertConforms(case("lowerCaseInterpolationAllCaps"))
    @Test fun testLowerCaseInterpolationMixed() = assertConforms(case("lowerCaseInterpolationMixed"))
    @Test fun testLowerCaseInterpolationAlreadyLower() = assertConforms(case("lowerCaseInterpolationAlreadyLower"))
    @Test fun testLowerCaseInterpolationEmpty() = assertConforms(case("lowerCaseInterpolationEmpty"))
    @Test fun testLowerCaseInterpolationWithDigits() = assertConforms(case("lowerCaseInterpolationWithDigits"))

    // ── Transform matrix: ?upper_case ────────────────────────────────────────

    @Test fun testUpperCaseInterpolationAllLower() = assertConforms(case("upperCaseInterpolationAllLower"))
    @Test fun testUpperCaseInterpolationMixed() = assertConforms(case("upperCaseInterpolationMixed"))
    @Test fun testUpperCaseInterpolationAlreadyUpper() = assertConforms(case("upperCaseInterpolationAlreadyUpper"))
    @Test fun testUpperCaseInterpolationEmpty() = assertConforms(case("upperCaseInterpolationEmpty"))

    // ── ?trim in conditions ───────────────────────────────────────────────────

    @Test fun testTrimConditionLhsEqualsMatch() = assertConforms(case("trimConditionLhsEqualsMatch"))
    @Test fun testTrimConditionLhsEqualsNoMatch() = assertConforms(case("trimConditionLhsEqualsNoMatch"))
    @Test fun testTrimConditionLhsNotEqualsMatch() = assertConforms(case("trimConditionLhsNotEqualsMatch"))
    @Test fun testTrimConditionLhsEmptyStringVsEmpty() = assertConforms(case("trimConditionLhsEmptyStringVsEmpty"))

    // ── ?lower_case in conditions ─────────────────────────────────────────────

    @Test fun testLowerCaseConditionLhsEqualsMatch() = assertConforms(case("lowerCaseConditionLhsEqualsMatch"))
    @Test fun testLowerCaseConditionLhsEqualsNoMatch() = assertConforms(case("lowerCaseConditionLhsEqualsNoMatch"))
    @Test fun testLowerCaseConditionLhsNotEqualsMatch() = assertConforms(case("lowerCaseConditionLhsNotEqualsMatch"))
    @Test fun testLowerCaseConditionLhsNotEqualsNoMatch() = assertConforms(case("lowerCaseConditionLhsNotEqualsNoMatch"))

    // ── ?upper_case in conditions ─────────────────────────────────────────────

    @Test fun testUpperCaseConditionLhsEqualsMatch() = assertConforms(case("upperCaseConditionLhsEqualsMatch"))
    @Test fun testUpperCaseConditionLhsEqualsNoMatch() = assertConforms(case("upperCaseConditionLhsEqualsNoMatch"))
    @Test fun testUpperCaseConditionLhsNotEqualsMatch() = assertConforms(case("upperCaseConditionLhsNotEqualsMatch"))

    // ── Transforms in switch ──────────────────────────────────────────────────

    @Test fun testTrimSwitchLeadingSpaces() = assertConforms(case("trimSwitchLeadingSpaces"))
    @Test fun testTrimSwitchNoMatch() = assertConforms(case("trimSwitchNoMatch"))
    @Test fun testLowerCaseSwitchMatch() = assertConforms(case("lowerCaseSwitchMatch"))
    @Test fun testLowerCaseSwitchNoMatch() = assertConforms(case("lowerCaseSwitchNoMatch"))
    @Test fun testLowerCaseSwitchMixedCaseInput() = assertConforms(case("lowerCaseSwitchMixedCaseInput"))
    @Test fun testUpperCaseSwitchMatch() = assertConforms(case("upperCaseSwitchMatch"))
    @Test fun testUpperCaseSwitchNoMatch() = assertConforms(case("upperCaseSwitchNoMatch"))

    // ── Transforms in concat ──────────────────────────────────────────────────

    @Test fun testTrimConcatLeadingPart() = assertConforms(case("trimConcatLeadingPart"))
    @Test fun testTrimConcatTrailingPart() = assertConforms(case("trimConcatTrailingPart"))
    @Test fun testTrimConcatMiddlePart() = assertConforms(case("trimConcatMiddlePart"))
    @Test fun testLowerCaseConcatLeadingPart() = assertConforms(case("lowerCaseConcatLeadingPart"))
    @Test fun testLowerCaseConcatTrailingPart() = assertConforms(case("lowerCaseConcatTrailingPart"))
    @Test fun testLowerCaseConcatMiddlePart() = assertConforms(case("lowerCaseConcatMiddlePart"))
    @Test fun testUpperCaseConcatLeadingPart() = assertConforms(case("upperCaseConcatLeadingPart"))
    @Test fun testUpperCaseConcatTrailingPart() = assertConforms(case("upperCaseConcatTrailingPart"))
    @Test fun testUpperCaseConcatMiddlePart() = assertConforms(case("upperCaseConcatMiddlePart"))

    // ── Chained transforms ────────────────────────────────────────────────────

    @Test fun testTrimThenLowerInterpolation() = assertConforms(case("trimThenLowerInterpolation"))
    @Test fun testTrimThenUpperInterpolation() = assertConforms(case("trimThenUpperInterpolation"))
    @Test fun testLowerThenTrimInterpolation() = assertConforms(case("lowerThenTrimInterpolation"))
    @Test fun testUpperThenTrimInterpolation() = assertConforms(case("upperThenTrimInterpolation"))
    @Test fun testTrimThenLowerInCondition() = assertConforms(case("trimThenLowerInCondition"))
    @Test fun testTrimThenUpperInCondition() = assertConforms(case("trimThenUpperInCondition"))
    @Test fun testLowerThenTrimInCondition() = assertConforms(case("lowerThenTrimInCondition"))
    @Test fun testTrimThenLowerInSwitch() = assertConforms(case("trimThenLowerInSwitch"))
    @Test fun testTrimThenUpperInSwitch() = assertConforms(case("trimThenUpperInSwitch"))
    @Test fun testTrimThenLowerInConcat() = assertConforms(case("trimThenLowerInConcat"))
    @Test fun testTrimThenUpperInConcat() = assertConforms(case("trimThenUpperInConcat"))
    @Test fun testLowerThenBooleanChain() = assertConforms(case("lowerThenBooleanChain"))
    @Test fun testTrimThenLowerThenBooleanChain() = assertConforms(case("trimThenLowerThenBooleanChain"))
    @Test fun testLowerThenHasContentKeyPresent() = assertConforms(case("lowerThenHasContentKeyPresent"))
    @Test fun testLowerThenHasContentEmptyString() = assertConforms(case("lowerThenHasContentEmptyString"))
    @Test fun testUpperThenHasContentKeyPresent() = assertConforms(case("upperThenHasContentKeyPresent"))
    @Test fun testTrimThenLowerTrimAgain() = assertConforms(case("trimThenLowerTrimAgain"))
    @Test fun testLowerThenUpperInterpolation() = assertConforms(case("lowerThenUpperInterpolation"))
    @Test fun testUpperThenLowerInterpolation() = assertConforms(case("upperThenLowerInterpolation"))
    @Test fun testLowerThenUpperInCondition() = assertConforms(case("lowerThenUpperInCondition"))
    @Test fun testUpperThenLowerInCondition() = assertConforms(case("upperThenLowerInCondition"))
    @Test fun testLowerThenTrimSwitch() = assertConforms(case("lowerThenTrimSwitch"))
    @Test fun testLowerThenTrimConcat() = assertConforms(case("lowerThenTrimConcat"))
    @Test fun testUpperThenTrimCondition() = assertConforms(case("upperThenTrimCondition"))
    @Test fun testUpperThenTrimSwitch() = assertConforms(case("upperThenTrimSwitch"))
    @Test fun testUpperThenTrimConcat() = assertConforms(case("upperThenTrimConcat"))
    @Test fun testTrimBooleanTrue() = assertConforms(case("trimBooleanTrue"))
    @Test fun testTrimBooleanFalse() = assertConforms(case("trimBooleanFalse"))
    @Test fun testLowerTrimBooleanChain() = assertConforms(case("lowerTrimBooleanChain"))
    @Test fun testLowerThenNumberInteger() = assertConforms(case("lowerThenNumberInteger"))
    @Test fun testLowerThenNumberScientificUpperE() = assertConforms(case("lowerThenNumberScientificUpperE"))
    @Test fun testUpperThenNumberInteger() = assertConforms(case("upperThenNumberInteger"))
    @Test fun testUpperThenNumberScientificLowerE() = assertConforms(case("upperThenNumberScientificLowerE"))
    @Test fun testTrimLowerHasContentPresent() = assertConforms(case("trimLowerHasContentPresent"))
    @Test fun testTrimLowerHasContentEmpty() = assertConforms(case("trimLowerHasContentEmpty"))
    @Test fun testTrimUpperHasContentPresent() = assertConforms(case("trimUpperHasContentPresent"))
    @Test fun testTrimUpperHasContentWhitespaceOnly() = assertConforms(case("trimUpperHasContentWhitespaceOnly"))
    @Test fun testLowerTrimHasContentPresent() = assertConforms(case("lowerTrimHasContentPresent"))
    @Test fun testUpperTrimHasContentPresent() = assertConforms(case("upperTrimHasContentPresent"))
    @Test fun testLowerCaseDefaultKeyPresent() = assertConforms(case("lowerCaseDefaultKeyPresent"))
    @Test fun testUpperCaseDefaultKeyPresent() = assertConforms(case("upperCaseDefaultKeyPresent"))

    // ── Numeric operators ─────────────────────────────────────────────────────

    @Test fun testNumericGtIntegerTrue() = assertConforms(case("numericGtIntegerTrue"))
    @Test fun testNumericGtIntegerFalseEqual() = assertConforms(case("numericGtIntegerFalseEqual"))
    @Test fun testNumericGtIntegerFalseBelow() = assertConforms(case("numericGtIntegerFalseBelow"))
    @Test fun testNumericGtFloat() = assertConforms(case("numericGtFloat"))
    @Test fun testNumericGtNegativeValues() = assertConforms(case("numericGtNegativeValues"))
    @Test fun testNumericGteExact() = assertConforms(case("numericGteExact"))
    @Test fun testNumericGteAbove() = assertConforms(case("numericGteAbove"))
    @Test fun testNumericGteBelow() = assertConforms(case("numericGteBelow"))
    @Test fun testNumericLtIntegerTrue() = assertConforms(case("numericLtIntegerTrue"))
    @Test fun testNumericLtIntegerFalseEqual() = assertConforms(case("numericLtIntegerFalseEqual"))
    @Test fun testNumericLtFloat() = assertConforms(case("numericLtFloat"))
    @Test fun testNumericLteExact() = assertConforms(case("numericLteExact"))
    @Test fun testNumericLteBelow() = assertConforms(case("numericLteBelow"))
    @Test fun testNumericLteAbove() = assertConforms(case("numericLteAbove"))
    @Test fun testNumericEqIntegerMatch() = assertConforms(case("numericEqIntegerMatch"))
    @Test fun testNumericEqIntegerNoMatch() = assertConforms(case("numericEqIntegerNoMatch"))
    @Test fun testNumericEqFloatMatch() = assertConforms(case("numericEqFloatMatch"))
    @Test fun testNumericEqFloatNoMatch() = assertConforms(case("numericEqFloatNoMatch"))
    @Test fun testNumericEqZero() = assertConforms(case("numericEqZero"))
    @Test fun testNumericEqNegative() = assertConforms(case("numericEqNegative"))
    @Test fun testNumericEqScientificNotation() = assertConforms(case("numericEqScientificNotation"))
    @Test fun testNumericNeqMatch() = assertConforms(case("numericNeqMatch"))
    @Test fun testNumericNeqNoMatch() = assertConforms(case("numericNeqNoMatch"))
    @Test fun testNumericNeqFloat() = assertConforms(case("numericNeqFloat"))
    @Test fun testNumericNeqZero() = assertConforms(case("numericNeqZero"))
    @Test fun testTrimNumberGtWhitespacePadded() = assertConforms(case("trimNumberGtWhitespacePadded"))
    @Test fun testTrimNumberEqWhitespacePadded() = assertConforms(case("trimNumberEqWhitespacePadded"))
    @Test fun testTrimNumberLteWhitespacePadded() = assertConforms(case("trimNumberLteWhitespacePadded"))

    // ── Logical operators ─────────────────────────────────────────────────────

    @Test fun testAndBothStringEqualities() = assertConforms(case("andBothStringEqualities"))
    @Test fun testAndLeftFalseShortCircuits() = assertConforms(case("andLeftFalseShortCircuits"))
    @Test fun testOrBothFalse() = assertConforms(case("orBothFalse"))
    @Test fun testOrLeftTrueShortCircuits() = assertConforms(case("orLeftTrueShortCircuits"))
    @Test fun testAndWithNumericGt() = assertConforms(case("andWithNumericGt"))
    @Test fun testAndWithNumericGtFalse() = assertConforms(case("andWithNumericGtFalse"))
    @Test fun testOrWithNumericAndString() = assertConforms(case("orWithNumericAndString"))
    @Test fun testAndWithTransformAndNumeric() = assertConforms(case("andWithTransformAndNumeric"))
    @Test fun testOrWithTwoTransformConditions() = assertConforms(case("orWithTwoTransformConditions"))
    @Test fun testAndPrecedenceOverOr() = assertConforms(case("andPrecedenceOverOr"))
    @Test fun testParenOverridesAndPrecedence() = assertConforms(case("parenOverridesAndPrecedence"))
    @Test fun testParenOverridesAndPrecedenceMatch() = assertConforms(case("parenOverridesAndPrecedenceMatch"))
    @Test fun testTripleAndAllTrue() = assertConforms(case("tripleAndAllTrue"))
    @Test fun testTripleAndMiddleFalse() = assertConforms(case("tripleAndMiddleFalse"))
    @Test fun testNegationInAnd() = assertConforms(case("negationInAnd"))
    @Test fun testNegationInOr() = assertConforms(case("negationInOr"))
    @Test fun testAndWithExistsCheck() = assertConforms(case("andWithExistsCheck"))
    @Test fun testAndWithExistsCheckMissing() = assertConforms(case("andWithExistsCheckMissing"))
    @Test fun testOrWithHasContent() = assertConforms(case("orWithHasContent"))

    // ── Default operator ──────────────────────────────────────────────────────

    @Test fun testDefaultKeyPresent() = assertConforms(case("defaultKeyPresent"))
    @Test fun testDefaultKeyMissing() = assertConforms(case("defaultKeyMissing"))
    @Test fun testDefaultEmptyStringDoesNotTrigger() = assertConforms(case("defaultEmptyStringDoesNotTrigger"))
    @Test fun testDefaultSingleQuoted() = assertConforms(case("defaultSingleQuoted"))
    @Test fun testDefaultInConditionParenKeyMissing() = assertConforms(case("defaultInConditionParenKeyMissing"))
    @Test fun testDefaultInConditionParenKeyPresent() = assertConforms(case("defaultInConditionParenKeyPresent"))
    @Test fun testBareDefaultKeyMissing() = assertConforms(case("bareDefaultKeyMissing"))
    @Test fun testBareDefaultKeyPresent() = assertConforms(case("bareDefaultKeyPresent"))
    @Test fun testBareDefaultInConditionKeyMissing() = assertConforms(case("bareDefaultInConditionKeyMissing"))
    @Test fun testBareDefaultInConditionKeyPresent() = assertConforms(case("bareDefaultInConditionKeyPresent"))
    @Test fun testTrimDefaultKeyPresent() = assertConforms(case("trimDefaultKeyPresent"))
    @Test fun testTrimDefaultEmptyStringDoesNotTrigger() = assertConforms(case("trimDefaultEmptyStringDoesNotTrigger"))

    // ── Nesting and structural ────────────────────────────────────────────────

    @Test fun testNestedIfBothTrue() = assertConforms(case("nestedIfBothTrue"))
    @Test fun testNestedIfInnerFalse() = assertConforms(case("nestedIfInnerFalse"))
    @Test fun testNestedIfOuterFalse() = assertConforms(case("nestedIfOuterFalse"))
    @Test fun testNestedIfWithTransformOuter() = assertConforms(case("nestedIfWithTransformOuter"))
    @Test fun testNestedIfWithTransformOuterBelowThreshold() = assertConforms(case("nestedIfWithTransformOuterBelowThreshold"))
    @Test fun testSwitchWithNestedIf() = assertConforms(case("switchWithNestedIf"))
    @Test fun testSwitchWithNestedIfFalse() = assertConforms(case("switchWithNestedIfFalse"))
    @Test fun testIfWithInterpolationAndTransformInBody() = assertConforms(case("ifWithInterpolationAndTransformInBody"))
    @Test fun testElseIfChainWithTransforms() = assertConforms(case("elseIfChainWithTransforms"))
    @Test fun testElseIfChainFallsToElse() = assertConforms(case("elseIfChainFallsToElse"))
    @Test fun testNumericElseIfChain() = assertConforms(case("numericElseIfChain"))
    @Test fun testNumericElseIfChainHigh() = assertConforms(case("numericElseIfChainHigh"))
    @Test fun testNumericElseIfChainZero() = assertConforms(case("numericElseIfChainZero"))

    // ── Edge case values ──────────────────────────────────────────────────────

    @Test fun testEmptyStringEquality() = assertConforms(case("emptyStringEquality"))
    @Test fun testSingleSpaceStringEquality() = assertConforms(case("singleSpaceStringEquality"))
    @Test fun testStringWithSpecialCharsEquality() = assertConforms(case("stringWithSpecialCharsEquality"))
    @Test fun testNumericIntegerZero() = assertConforms(case("numericIntegerZero"))
    @Test fun testNumericNegativeGtNegative() = assertConforms(case("numericNegativeGtNegative"))
    @Test fun testNumericLargeInt() = assertConforms(case("numericLargeInt"))
    @Test fun testStringWithGtCharInValue() = assertConforms(case("stringWithGtCharInValue"))
    @Test fun testLowerCaseWithDigitsAndSymbols() = assertConforms(case("lowerCaseWithDigitsAndSymbols"))
    @Test fun testUpperCaseWithDigitsAndSymbols() = assertConforms(case("upperCaseWithDigitsAndSymbols"))
    @Test fun testBooleanLowerTrue() = assertConforms(case("booleanLowerTrue"))
    @Test fun testBooleanLowerFalse() = assertConforms(case("booleanLowerFalse"))
    @Test fun testExistsCheckOnPresentEmptyString() = assertConforms(case("existsCheckOnPresentEmptyString"))
    @Test fun testExistsCheckOnMissing() = assertConforms(case("existsCheckOnMissing"))
    @Test fun testHasContentEmptyString() = assertConforms(case("hasContentEmptyString"))
    @Test fun testHasContentWhitespaceOnly() = assertConforms(case("hasContentWhitespaceOnly"))
    @Test fun testTrimHasContentWhitespaceOnlyFalse() = assertConforms(case("trimHasContentWhitespaceOnlyFalse"))
    @Test fun testTrimHasContentNonEmpty() = assertConforms(case("trimHasContentNonEmpty"))

    // ── Concat edge cases ─────────────────────────────────────────────────────

    @Test fun testConcatThreeParts() = assertConforms(case("concatThreeParts"))
    @Test fun testConcatFourParts() = assertConforms(case("concatFourParts"))
    @Test fun testConcatLiteralOnly() = assertConforms(case("concatLiteralOnly"))
    @Test fun testConcatWithLowerAndUpper() = assertConforms(case("concatWithLowerAndUpper"))
    @Test fun testConcatWithTrimBothSides() = assertConforms(case("concatWithTrimBothSides"))
    @Test fun testConcatWithTransformAndLiteral() = assertConforms(case("concatWithTransformAndLiteral"))

    // ── Real-world style templates ────────────────────────────────────────────

    @Test fun testRealWorldTierAndPoints() = assertConforms(case("realWorldTierAndPoints"))
    @Test fun testRealWorldTierAndPointsGoldLowPoints() = assertConforms(case("realWorldTierAndPointsGoldLowPoints"))
    @Test fun testRealWorldTierAndPointsStandard() = assertConforms(case("realWorldTierAndPointsStandard"))
    @Test fun testRealWorldPersonalisedGreeting() = assertConforms(case("realWorldPersonalisedGreeting"))
    @Test fun testRealWorldPersonalisedGreetingMissing() = assertConforms(case("realWorldPersonalisedGreetingMissing"))
    @Test fun testRealWorldCountrySwitch() = assertConforms(case("realWorldCountrySwitch"))
    @Test fun testRealWorldCountrySwitchDefault() = assertConforms(case("realWorldCountrySwitchDefault"))
    @Test fun testRealWorldOptInStatus() = assertConforms(case("realWorldOptInStatus"))

    // ── Parenthesized default × logical operators ─────────────────────────
    @Test fun testParenDefaultBooleanAndKeyAbsent() = assertConforms(case("parenDefaultBooleanAndKeyAbsent"))
    @Test fun testParenDefaultBooleanAndKeyPresent() = assertConforms(case("parenDefaultBooleanAndKeyPresent"))
    @Test fun testParenDefaultBooleanOrKeyAbsentTrueDefault() = assertConforms(case("parenDefaultBooleanOrKeyAbsentTrueDefault"))
    @Test fun testParenDefaultBooleanNegateKeyAbsentFalseDefault() = assertConforms(case("parenDefaultBooleanNegateKeyAbsentFalseDefault"))
    @Test fun testParenDefaultBooleanNegateKeyPresentTrue() = assertConforms(case("parenDefaultBooleanNegateKeyPresentTrue"))
    @Test fun testParenDefaultNumberGtKeyAbsentFalse() = assertConforms(case("parenDefaultNumberGtKeyAbsentFalse"))
    @Test fun testParenDefaultNumberGteKeyAbsentTrue() = assertConforms(case("parenDefaultNumberGteKeyAbsentTrue"))
    @Test fun testParenDefaultNumberGtKeyPresentOverrides() = assertConforms(case("parenDefaultNumberGtKeyPresentOverrides"))
    @Test fun testParenDefaultNumberGteAndStringKeyAbsent() = assertConforms(case("parenDefaultNumberGteAndStringKeyAbsent"))
    @Test fun testParenDefaultHasContentKeyAbsentEmptyDefault() = assertConforms(case("parenDefaultHasContentKeyAbsentEmptyDefault"))
    @Test fun testParenDefaultHasContentKeyAbsentNonEmptyDefault() = assertConforms(case("parenDefaultHasContentKeyAbsentNonEmptyDefault"))
    @Test fun testParenDefaultHasContentKeyPresentEmpty() = assertConforms(case("parenDefaultHasContentKeyPresentEmpty"))
    @Test fun testParenDefaultHasContentAndConditionBothTrue() = assertConforms(case("parenDefaultHasContentAndConditionBothTrue"))
    @Test fun testParenDefaultHasContentOrFallsToOther() = assertConforms(case("parenDefaultHasContentOrFallsToOther"))

    // ── Switch with default operator ──────────────────────────────────────
    @Test fun testSwitchDefaultKeyAbsentMatchesCase() = assertConforms(case("switchDefaultKeyAbsentMatchesCase"))
    @Test fun testSwitchDefaultKeyAbsentNoMatchHitsDefault() = assertConforms(case("switchDefaultKeyAbsentNoMatchHitsDefault"))
    @Test fun testSwitchDefaultKeyAbsentNoMatchNoDefault() = assertConforms(case("switchDefaultKeyAbsentNoMatchNoDefault"))
    @Test fun testSwitchDefaultKeyPresentOverridesDefault() = assertConforms(case("switchDefaultKeyPresentOverridesDefault"))
    @Test fun testSwitchDefaultKeyPresentNoMatch() = assertConforms(case("switchDefaultKeyPresentNoMatch"))
    @Test fun testSwitchDefaultWithInterpolationInBody() = assertConforms(case("switchDefaultWithInterpolationInBody"))

    // ── Exists check in <#elseif> ─────────────────────────────────────────
    @Test fun testElseIfExistsCheckPresent() = assertConforms(case("elseIfExistsCheckPresent"))
    @Test fun testElseIfExistsCheckMissing() = assertConforms(case("elseIfExistsCheckMissing"))
    @Test fun testElseIfNegatedExistsCheckMissing() = assertConforms(case("elseIfNegatedExistsCheckMissing"))
    @Test fun testElseIfExistsWithAnd() = assertConforms(case("elseIfExistsWithAnd"))

    // ── Concatenation without spaces around + ─────────────────────────────
    @Test fun testConcatNoSpacesTwoParts() = assertConforms(case("concatNoSpacesTwoParts"))
    @Test fun testConcatNoSpacesPropertyAndLiteral() = assertConforms(case("concatNoSpacesPropertyAndLiteral"))
    @Test fun testConcatMixedSpacing() = assertConforms(case("concatMixedSpacing"))
    @Test fun testConcatNoSpacesWithTransform() = assertConforms(case("concatNoSpacesWithTransform"))

    // ── Real-world templates using new features ───────────────────────────
    @Test fun testRealWorldSwitchWithDefault() = assertConforms(case("realWorldSwitchWithDefault"))
    @Test fun testRealWorldParenDefaultBooleanFlag() = assertConforms(case("realWorldParenDefaultBooleanFlag"))
    @Test fun testRealWorldParenDefaultNumberDiscount() = assertConforms(case("realWorldParenDefaultNumberDiscount"))
    @Test fun testRealWorldParenDefaultHasContentGreeting() = assertConforms(case("realWorldParenDefaultHasContentGreeting"))
    @Test fun testRealWorldCombinedDefaultsEliteGold() = assertConforms(case("realWorldCombinedDefaultsEliteGold"))
    @Test fun testRealWorldCombinedDefaultsGold() = assertConforms(case("realWorldCombinedDefaultsGold"))
    @Test fun testRealWorldCombinedDefaultsStandard() = assertConforms(case("realWorldCombinedDefaultsStandard"))

    // ── Parenthesized default with trailing transform in equality ─────────
    @Test fun testParenDefaultTransformLowerCaseMatchKeyAbsent() = assertConforms(case("parenDefaultTransformLowerCaseMatchKeyAbsent"))
    @Test fun testParenDefaultTransformLowerCaseMatchKeyPresent() = assertConforms(case("parenDefaultTransformLowerCaseMatchKeyPresent"))
    @Test fun testParenDefaultTransformLowerCaseNoMatchKeyPresent() = assertConforms(case("parenDefaultTransformLowerCaseNoMatchKeyPresent"))
    @Test fun testParenDefaultTransformUpperCaseMatchKeyAbsent() = assertConforms(case("parenDefaultTransformUpperCaseMatchKeyAbsent"))
    @Test fun testParenDefaultTransformTrimMatchKeyAbsent() = assertConforms(case("parenDefaultTransformTrimMatchKeyAbsent"))
    @Test fun testParenDefaultTransformLowerCaseWithAndKeyAbsent() = assertConforms(case("parenDefaultTransformLowerCaseWithAndKeyAbsent"))
    @Test fun testParenDefaultTransformLowerCaseWithAndBothMatch() = assertConforms(case("parenDefaultTransformLowerCaseWithAndBothMatch"))
    @Test fun testParenDefaultTransformInElseIf() = assertConforms(case("parenDefaultTransformInElseIf"))

    // ── (key!"default")?transform in ${} interpolation ──────────────────
    @Test fun testParenDefaultInterpolationUpperCaseKeyAbsent() = assertConforms(case("parenDefaultInterpolationUpperCaseKeyAbsent"))
    @Test fun testParenDefaultInterpolationUpperCaseKeyPresent() = assertConforms(case("parenDefaultInterpolationUpperCaseKeyPresent"))
    @Test fun testParenDefaultInterpolationLowerCaseKeyAbsent() = assertConforms(case("parenDefaultInterpolationLowerCaseKeyAbsent"))
    @Test fun testParenDefaultInterpolationTrimKeyAbsent() = assertConforms(case("parenDefaultInterpolationTrimKeyAbsent"))
    @Test fun testParenDefaultInterpolationTrimKeyPresent() = assertConforms(case("parenDefaultInterpolationTrimKeyPresent"))
    @Test fun testParenDefaultInterpolationNoTransformKeyAbsent() = assertConforms(case("parenDefaultInterpolationNoTransformKeyAbsent"))
    @Test fun testParenDefaultInterpolationChainedTransformKeyAbsent() = assertConforms(case("parenDefaultInterpolationChainedTransformKeyAbsent"))

    // ── bare ?boolean without transforms ─────────────────────────────────
    @Test fun testBareBooleanTrueKeyPresent() = assertConforms(case("bareBooleanTrueKeyPresent"))
    @Test fun testBareBooleanFalseKeyPresent() = assertConforms(case("bareBooleanFalseKeyPresent"))
    @Test fun testBareBooleanWithDefaultTrueKeyAbsent() = assertConforms(case("bareBooleanWithDefaultTrueKeyAbsent"))
    @Test fun testBareBooleanWithDefaultFalseKeyAbsent() = assertConforms(case("bareBooleanWithDefaultFalseKeyAbsent"))

    // ── (key!"default") with != operator ─────────────────────────────────
    @Test fun testParenDefaultNotEqualsKeyAbsentMatchesDefault() = assertConforms(case("parenDefaultNotEqualsKeyAbsentMatchesDefault"))
    @Test fun testParenDefaultNotEqualsKeyAbsentDefaultIsValue() = assertConforms(case("parenDefaultNotEqualsKeyAbsentDefaultIsValue"))
    @Test fun testParenDefaultNotEqualsKeyPresent() = assertConforms(case("parenDefaultNotEqualsKeyPresent"))
    @Test fun testParenDefaultTransformNotEqualsKeyAbsent() = assertConforms(case("parenDefaultTransformNotEqualsKeyAbsent"))
    @Test fun testParenDefaultTransformNotEqualsKeyPresentMatchesAfterTransform() = assertConforms(case("parenDefaultTransformNotEqualsKeyPresentMatchesAfterTransform"))

    // ── (key!"default")?number with lt / lte / != ─────────────────────────
    @Test fun testParenDefaultNumberLtKeyAbsentTrue() = assertConforms(case("parenDefaultNumberLtKeyAbsentTrue"))
    @Test fun testParenDefaultNumberLtKeyAbsentFalse() = assertConforms(case("parenDefaultNumberLtKeyAbsentFalse"))
    @Test fun testParenDefaultNumberLtKeyPresentOverrides() = assertConforms(case("parenDefaultNumberLtKeyPresentOverrides"))
    @Test fun testParenDefaultNumberLteKeyAbsentTrue() = assertConforms(case("parenDefaultNumberLteKeyAbsentTrue"))
    @Test fun testParenDefaultNumberLteKeyAbsentFalse() = assertConforms(case("parenDefaultNumberLteKeyAbsentFalse"))
    @Test fun testParenDefaultNumberNotEqualsKeyAbsentTrue() = assertConforms(case("parenDefaultNumberNotEqualsKeyAbsentTrue"))
    @Test fun testParenDefaultNumberNotEqualsKeyAbsentDefaultMatchesRhs() = assertConforms(case("parenDefaultNumberNotEqualsKeyAbsentDefaultMatchesRhs"))
    @Test fun testParenDefaultNumberNotEqualsKeyPresent() = assertConforms(case("parenDefaultNumberNotEqualsKeyPresent"))

    // ── ?? with || ────────────────────────────────────────────────────────
    @Test fun testExistsCheckOrFalse() = assertConforms(case("existsCheckOrFalse"))
    @Test fun testExistsCheckOrLeftTrue() = assertConforms(case("existsCheckOrLeftTrue"))
    @Test fun testExistsCheckOrRightTrue() = assertConforms(case("existsCheckOrRightTrue"))
    @Test fun testExistsCheckOrBothTrue() = assertConforms(case("existsCheckOrBothTrue"))

    // ── multiple ?? in same && condition ──────────────────────────────────
    @Test fun testDoubleExistsCheckBothPresent() = assertConforms(case("doubleExistsCheckBothPresent"))
    @Test fun testDoubleExistsCheckFirstMissing() = assertConforms(case("doubleExistsCheckFirstMissing"))
    @Test fun testDoubleExistsCheckSecondMissing() = assertConforms(case("doubleExistsCheckSecondMissing"))
    @Test fun testDoubleExistsCheckBothMissing() = assertConforms(case("doubleExistsCheckBothMissing"))

    // ── switch + default + transform combined ─────────────────────────────
    @Test fun testSwitchDefaultWithTransformKeyAbsent() = assertConforms(case("switchDefaultWithTransformKeyAbsent"))
    @Test fun testSwitchDefaultWithTransformKeyPresentAlreadyNormalized() = assertConforms(case("switchDefaultWithTransformKeyPresentAlreadyNormalized"))
    @Test fun testSwitchDefaultWithTransformKeyAbsentNoCase() = assertConforms(case("switchDefaultWithTransformKeyAbsentNoCase"))

    // ── concat with paren-default operands ────────────────────────────────
    @Test fun testConcatParenDefaultBothAbsent() = assertConforms(case("concatParenDefaultBothAbsent"))
    @Test fun testConcatParenDefaultFirstPresent() = assertConforms(case("concatParenDefaultFirstPresent"))
    @Test fun testConcatParenDefaultBothPresent() = assertConforms(case("concatParenDefaultBothPresent"))
    @Test fun testConcatParenDefaultWithTransform() = assertConforms(case("concatParenDefaultWithTransform"))

    // ── two paren-defaults combined with || ───────────────────────────────
    @Test fun testParenDefaultOrBothAbsentBothMatch() = assertConforms(case("parenDefaultOrBothAbsentBothMatch"))
    @Test fun testParenDefaultOrBothAbsentNeitherMatch() = assertConforms(case("parenDefaultOrBothAbsentNeitherMatch"))
    @Test fun testParenDefaultOrBothAbsentSecondMatch() = assertConforms(case("parenDefaultOrBothAbsentSecondMatch"))
    @Test fun testParenDefaultOrFirstPresentMatchesRhs() = assertConforms(case("parenDefaultOrFirstPresentMatchesRhs"))

    // =========================================================================
    // KNOWN DIVERGENCES — use assertDiverges instead of @Ignore
    //
    // These tests PASS when SDK and FreeMarker diverge (the expected state),
    // and FAIL if they ever agree — signalling the divergence was fixed and
    // the test should be promoted to a normal assertConforms call.
    // =========================================================================

    @Test
    fun testLowerCaseDefaultKeyMissing() =
        assertBothSuppress("\${Recipient.v?lower_case!\"fallback\"}")

    @Test
    fun testUpperCaseDefaultKeyMissing() =
        assertBothSuppress("\${Recipient.v?upper_case!\"fallback\"}")

    @Test
    fun testTrimDefaultKeyMissing() =
        assertBothSuppress("\${Recipient.v?trim!\"fallback\"}")

    @Test
    fun testLowerCaseHasContentMissingKey() =
        assertBothSuppress("<#if Recipient.v?lower_case?has_content>yes<#else>no</#if>")

    /**
     * DIVERGENCE: ?number on whitespace-padded input.
     * SDK: toDoubleOrNull() accepts " 10 " and evaluates to 10.
     * FreeMarker 2.3.23: throws NonNumericalException — ?number does not trim whitespace.
     * Workaround: use ?trim?number.
     */
    @Test
    fun testNumericEqWhitespacePadded() =
        assertDiverges("yes", "<#if Recipient.v?number == 10>yes<#else>no</#if>", mapOf("Recipient.v" to " 10 "))

    /**
     * DIVERGENCE: ?upper_case?boolean on "true" — SDK lowercases before comparing so accepts
     * "TRUE"; FreeMarker 2.3.23 rejects "TRUE" for ?boolean.
     */
    @Test
    fun testUpperCaseBooleanOnTrue() =
        assertDiverges("yes", "<#if Recipient.v?upper_case?boolean>yes<#else>no</#if>", mapOf("Recipient.v" to "true"))

    /**
     * DIVERGENCE: ?trim?upper_case?boolean — trim produces "true", upper_case produces "TRUE",
     * FreeMarker 2.3.23 rejects "TRUE" for ?boolean; SDK accepts it.
     */
    @Test
    fun testTrimUpperCaseBooleanDiverges() =
        assertDiverges("yes", "<#if Recipient.v?trim?upper_case?boolean>yes<#else>no</#if>", mapOf("Recipient.v" to "  true  "))

    /**
     * DIVERGENCE: ?upper_case?trim?boolean — upper_case of "  true  " → "  TRUE  ",
     * trim → "TRUE", FreeMarker 2.3.23 rejects "TRUE" for ?boolean; SDK accepts it.
     */
    @Test
    fun testUpperCaseTrimBooleanDiverges() =
        assertDiverges("yes", "<#if Recipient.v?upper_case?trim?boolean>yes<#else>no</#if>", mapOf("Recipient.v" to "  true  "))
}
