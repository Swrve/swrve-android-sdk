package com.swrve.sdk

import java.util.Date

/**
 * Evaluates a FreeMarker template subset against a set of user properties.
 *
 * Pipeline: template string → FMTokenizer → FMParser → FMEvaluator → rendered string
 *
 * Supported directives: <#if> / <#elseif> / <#else> / </#if>, <#switch> / <#case> / <#default> / <#break> / </#switch>
 * Supported interpolations: ${Recipient.x}, ${Recipient.x!"default"}, ${Recipient.x?trim}, ${Recipient.x?lower_case}, ${Recipient.x?upper_case}, ${Recipient.x?trim?lower_case!"default"} (transforms apply only when key exists); ${(Recipient.x!"default")?upper_case} (paren-default form — transforms apply to both value and fallback); string concatenation with + (?trim/?lower_case/?upper_case allowed on variables)
 * Supported condition operators: ==, != (string or numeric — ?number == 10, ?number != 0), ?? (exists check), gt, gte, lt, lte (numeric and date), !, &&, ||
 * Supported built-ins: ?number, ?boolean, ?trim, ?lower_case, ?upper_case, ?has_content, ?date, ?datetime; chains e.g. ?trim?lower_case?boolean, ?lower_case?has_content, ?datetime?date
 * Parenthesised default in conditions: (key!"default")?boolean, (key!"default")?number gte N, (key!"default")?date gte .now?date, (key!"default")?has_content
 * <#switch> default: <#switch Recipient.tier!"bronze"> falls back to "bronze" when key is absent
 * Concatenation: ${a+b} and ${a + b} are both valid (spaces around + are optional)
 * Special variables: .now (current datetime), .now?date (current calendar date)
 */
class SwrveFreemarkerEvaluator {
    companion object {
        /** Injectable time provider for unit tests — not part of the public API. */
        var nowProvider: () -> Date = { Date() }

        @JvmStatic
        @Throws(FreemarkerException::class)
        fun evaluate(templateString: String?, properties: Map<String, Any>): String =
            evaluate(templateString, properties, useLocalTimezone = false)

        @JvmStatic
        @Throws(FreemarkerException::class)
        fun evaluate(templateString: String?, properties: Map<String, Any>, useLocalTimezone: Boolean): String {
            if (templateString == null) throw FreemarkerException("Missing template string")
            val stringProperties = properties.entries.mapNotNull { (k, v) ->
                when (v) {
                    is String -> k to v
                    is Number -> k to v.toString()
                    is Boolean -> k to v.toString()
                    else -> null
                }
            }.toMap()
            val tokens = FMTokenizer(templateString).tokenize()
            val nodes = FMParser(tokens).parse()
            return FMEvaluator(stringProperties, useLocalTimezone).evaluate(nodes)
        }
    }
}

// MARK: - Error

class FreemarkerException(message: String) : Exception(message)

// MARK: - Tokens

internal sealed class FMToken {
    data class Text(val value: String) : FMToken()
    data class IfDirective(val condition: String) : FMToken()
    data class ElseIfDirective(val condition: String) : FMToken()
    object ElseDirective : FMToken()
    object EndIf : FMToken()
    data class Interpolation(val expr: String) : FMToken()
    data class SwitchDirective(val expr: String) : FMToken()
    data class CaseDirective(val value: String) : FMToken()
    object DefaultDirective : FMToken()
    object BreakDirective : FMToken()
    object EndSwitch : FMToken()
}

// MARK: - AST

internal data class FMSwitchCase(
    val value: String,
    val body: List<FMASTNode>,
    val hasBreak: Boolean
)

internal sealed class FMASTNode {
    data class Text(val value: String) : FMASTNode()
    data class Interpolation(val expr: FMInterpolationExpr) : FMASTNode()
    data class IfStatement(
        val condition: FMConditionExpr,
        val thenBody: List<FMASTNode>,
        val elseIfClauses: List<Pair<FMConditionExpr, List<FMASTNode>>>,
        val elseBody: List<FMASTNode>
    ) : FMASTNode()

    data class SwitchStatement(
        val variable: String,
        val switchDefault: String?,
        val transforms: List<FMStringTransform>,
        val cases: List<FMSwitchCase>,
        val defaultBody: List<FMASTNode>
    ) : FMASTNode()
}

internal enum class FMStringTransform {
    TRIM, LOWER_CASE, UPPER_CASE
}

internal enum class FMNumericOp {
    GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, EQUALS, NOT_EQUALS
}

internal sealed class FMConditionExpr {
    data class Exists(val key: String) : FMConditionExpr()
    data class Equals(val key: String, val transforms: List<FMStringTransform>, val value: String) : FMConditionExpr()
    data class EqualsWithDefault(val key: String, val defaultValue: String, val transforms: List<FMStringTransform>, val value: String, val parenDefault: Boolean = false) : FMConditionExpr()
    data class NotEquals(val key: String, val transforms: List<FMStringTransform>, val value: String) : FMConditionExpr()
    data class NotEqualsWithDefault(val key: String, val defaultValue: String, val transforms: List<FMStringTransform>, val value: String, val parenDefault: Boolean = false) : FMConditionExpr()
    data class NumericCompare(val key: String, val op: FMNumericOp, val rhs: Double, val transforms: List<FMStringTransform>) : FMConditionExpr()
    data class NumericCompareWithDefault(val key: String, val transforms: List<FMStringTransform>, val defaultValue: String, val op: FMNumericOp, val rhs: Double) : FMConditionExpr()
    data class BooleanValue(val key: String, val transforms: List<FMStringTransform>) : FMConditionExpr()
    data class BooleanValueWithDefault(val key: String, val transforms: List<FMStringTransform>, val defaultValue: String) : FMConditionExpr()
    data class HasContent(val variable: String, val transforms: List<FMStringTransform>) : FMConditionExpr()
    data class HasContentWithDefault(val variable: String, val transforms: List<FMStringTransform>, val defaultValue: String) : FMConditionExpr()
    data class DateCompare(val lhs: FMDateExpr, val op: FMNumericOp, val rhs: FMDateExpr) : FMConditionExpr()
    data class Not(val inner: FMConditionExpr) : FMConditionExpr()
    data class And(val left: FMConditionExpr, val right: FMConditionExpr) : FMConditionExpr()
    data class Or(val left: FMConditionExpr, val right: FMConditionExpr) : FMConditionExpr()
}

internal sealed class FMInterpolationExpr {
    data class Variable(val key: String, val transforms: List<FMStringTransform>) : FMInterpolationExpr()
    data class WithDefault(val variable: String, val transforms: List<FMStringTransform>, val defaultValue: String) : FMInterpolationExpr()
    // (key!"default")?transforms* — transforms apply to both the live value and the fallback
    data class ParenDefaultWithTransform(val variable: String, val transforms: List<FMStringTransform>, val defaultValue: String) : FMInterpolationExpr()
    data class Concat(val parts: List<FMConcatPart>) : FMInterpolationExpr()
}

internal sealed class FMConcatPart {
    data class Literal(val value: String) : FMConcatPart()
    data class Variable(val key: String, val transforms: List<FMStringTransform>) : FMConcatPart()
    // (key!"default")?transforms* — transforms apply to both the live value and the fallback
    data class ParenDefault(val key: String, val transforms: List<FMStringTransform>, val defaultValue: String) : FMConcatPart()
}

// MARK: - Date types

internal enum class FMDateKind {
    DATE, DATE_TIME, STRICT_DATE
}

internal sealed class FMDateExpr {
    data class Property(val variable: String, val kind: FMDateKind) : FMDateExpr()
    data class PropertyWithDefault(val variable: String, val defaultValue: String, val kind: FMDateKind) : FMDateExpr()
    data class Now(val kind: FMDateKind) : FMDateExpr()
}

internal data class FMCalendarDate(val year: Int, val month: Int, val day: Int) : Comparable<FMCalendarDate> {
    override fun compareTo(other: FMCalendarDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }
}

internal sealed class FMDateValue {
    data class CalendarDate(val date: FMCalendarDate) : FMDateValue()
    data class DateTime(val date: Date) : FMDateValue()
}
