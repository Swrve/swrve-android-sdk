package com.swrve.sdk

import android.os.Build
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal class FMEvaluator(
    private val properties: Map<String, String>,
    private val useLocalTimezone: Boolean
) {
    private val timezone: TimeZone
        get() = if (useLocalTimezone) TimeZone.getDefault() else TimeZone.getTimeZone("UTC")

    fun evaluate(nodes: List<FMASTNode>): String {
        val sb = StringBuilder()
        for (node in nodes) {
            when (node) {
                is FMASTNode.Text -> sb.append(node.value)
                is FMASTNode.Interpolation -> sb.append(evaluateInterpolation(node.expr))
                is FMASTNode.IfStatement -> {
                    when {
                        evaluateCondition(node.condition) -> sb.append(evaluate(node.thenBody))
                        else -> {
                            val matchingElseIf = node.elseIfClauses.firstOrNull { evaluateCondition(it.first) }
                            if (matchingElseIf != null) {
                                sb.append(evaluate(matchingElseIf.second))
                            } else {
                                sb.append(evaluate(node.elseBody))
                            }
                        }
                    }
                }

                is FMASTNode.SwitchStatement -> {
                    val raw = properties[node.variable]
                        ?: node.switchDefault
                        ?: throw FreemarkerException("Missing required property key in <#switch>: ${node.variable}")
                    val actual = applyTransforms(raw, node.transforms)
                    val startIdx = node.cases.indexOfFirst { it.value == actual }
                    if (startIdx != -1) {
                        var broke = false
                        for (i in startIdx until node.cases.size) {
                            sb.append(evaluate(node.cases[i].body))
                            if (node.cases[i].hasBreak) {
                                broke = true; break
                            }
                        }
                        if (!broke) sb.append(evaluate(node.defaultBody))
                    } else {
                        sb.append(evaluate(node.defaultBody))
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun applyTransforms(value: String, transforms: List<FMStringTransform>): String =
        transforms.fold(value) { v, t ->
            when (t) {
                FMStringTransform.TRIM -> v.trim()
                FMStringTransform.LOWER_CASE -> v.lowercase(Locale.ROOT)
                FMStringTransform.UPPER_CASE -> v.uppercase(Locale.ROOT)
            }
        }

    private fun evaluateInterpolation(expr: FMInterpolationExpr): String = when (expr) {
        is FMInterpolationExpr.Variable -> {
            val raw = properties[expr.key]
                ?: throw FreemarkerException("Missing required property key: ${expr.key}")
            applyTransforms(raw, expr.transforms)
        }

        is FMInterpolationExpr.WithDefault -> {
            val raw = properties[expr.variable]
            if (raw == null) {
                if (expr.transforms.isNotEmpty()) throw FreemarkerException("Missing property key '${expr.variable}' with transforms before !: use (${expr.variable}!\"default\")?transform instead of ${expr.variable}?transform!\"default\"")
                expr.defaultValue
            } else {
                applyTransforms(raw, expr.transforms)
            }
        }

        is FMInterpolationExpr.ParenDefaultWithTransform ->
            properties[expr.variable]?.let { applyTransforms(it, expr.transforms) }
                ?: applyTransforms(expr.defaultValue, expr.transforms)

        is FMInterpolationExpr.Concat -> {
            val sb = StringBuilder()
            for (part in expr.parts) {
                when (part) {
                    is FMConcatPart.Literal -> sb.append(part.value)
                    is FMConcatPart.Variable -> {
                        val raw = properties[part.key]
                            ?: throw FreemarkerException("Missing required property key in concatenation: ${part.key}")
                        sb.append(applyTransforms(raw, part.transforms))
                    }
                    is FMConcatPart.ParenDefault -> {
                        val raw = properties[part.key] ?: part.defaultValue
                        sb.append(applyTransforms(raw, part.transforms))
                    }
                }
            }
            sb.toString()
        }
    }

    private fun evaluateCondition(condition: FMConditionExpr): Boolean = when (condition) {
        is FMConditionExpr.Exists -> properties[condition.key] != null

        is FMConditionExpr.Equals -> {
            val raw = properties[condition.key]
                ?: throw FreemarkerException("Missing property key in == condition: ${condition.key}")
            applyTransforms(raw, condition.transforms) == condition.value
        }

        is FMConditionExpr.EqualsWithDefault -> {
            val raw = properties[condition.key] ?: when {
                condition.parenDefault -> condition.defaultValue
                condition.transforms.isNotEmpty() -> throw FreemarkerException("Missing property key '${condition.key}' with transforms before ! in == condition: use (${condition.key}!\"default\")?transform instead of ${condition.key}?transform!\"default\"")
                else -> condition.defaultValue
            }
            applyTransforms(raw, condition.transforms) == condition.value
        }

        is FMConditionExpr.NotEquals -> {
            val raw = properties[condition.key]
                ?: throw FreemarkerException("Missing property key in != condition: ${condition.key}")
            applyTransforms(raw, condition.transforms) != condition.value
        }

        is FMConditionExpr.NotEqualsWithDefault -> {
            val raw = properties[condition.key] ?: when {
                condition.parenDefault -> condition.defaultValue
                condition.transforms.isNotEmpty() -> throw FreemarkerException("Missing property key '${condition.key}' with transforms before ! in != condition: use (${condition.key}!\"default\")?transform instead of ${condition.key}?transform!\"default\"")
                else -> condition.defaultValue
            }
            applyTransforms(raw, condition.transforms) != condition.value
        }

        is FMConditionExpr.NumericCompare -> {
            val raw = properties[condition.key]
                ?: throw FreemarkerException("Missing property key in numeric comparison: ${condition.key}")
            val prepared = applyTransforms(raw, condition.transforms)
            val lhs = prepared.toDoubleOrNull()
                ?: throw FreemarkerException("?number: cannot parse '$prepared' as a number for key: ${condition.key}")
            if (lhs.isNaN() || lhs.isInfinite())
                throw FreemarkerException("?number: '$prepared' is not a finite number for key: ${condition.key}")
            when (condition.op) {
                FMNumericOp.GREATER_THAN -> lhs > condition.rhs
                FMNumericOp.GREATER_THAN_OR_EQUAL -> lhs >= condition.rhs
                FMNumericOp.LESS_THAN -> lhs < condition.rhs
                FMNumericOp.LESS_THAN_OR_EQUAL -> lhs <= condition.rhs
                FMNumericOp.EQUALS -> lhs == condition.rhs
                FMNumericOp.NOT_EQUALS -> lhs != condition.rhs
            }
        }

        is FMConditionExpr.NumericCompareWithDefault -> {
            val raw = properties[condition.key] ?: condition.defaultValue
            val prepared = applyTransforms(raw, condition.transforms)
            val lhs = prepared.toDoubleOrNull()
                ?: throw FreemarkerException("?number: cannot parse '$prepared' as a number for key: ${condition.key}")
            if (lhs.isNaN() || lhs.isInfinite())
                throw FreemarkerException("?number: '$prepared' is not a finite number for key: ${condition.key}")
            when (condition.op) {
                FMNumericOp.GREATER_THAN -> lhs > condition.rhs
                FMNumericOp.GREATER_THAN_OR_EQUAL -> lhs >= condition.rhs
                FMNumericOp.LESS_THAN -> lhs < condition.rhs
                FMNumericOp.LESS_THAN_OR_EQUAL -> lhs <= condition.rhs
                FMNumericOp.EQUALS -> lhs == condition.rhs
                FMNumericOp.NOT_EQUALS -> lhs != condition.rhs
            }
        }

        is FMConditionExpr.BooleanValue -> {
            val raw = properties[condition.key]
                ?: throw FreemarkerException("Missing property key in ?boolean condition: ${condition.key}")
            val effective = applyTransforms(raw, condition.transforms)
            when (effective.lowercase(Locale.ROOT)) {
                "true" -> true
                "false" -> false
                else -> throw FreemarkerException("?boolean: '$effective' is not a valid boolean for key: ${condition.key}. Only 'true' or 'false' are accepted.")
            }
        }

        is FMConditionExpr.BooleanValueWithDefault -> {
            val raw = properties[condition.key] ?: condition.defaultValue
            val effective = applyTransforms(raw, condition.transforms)
            when (effective.lowercase(Locale.ROOT)) {
                "true" -> true
                "false" -> false
                else -> throw FreemarkerException("?boolean: '$effective' is not a valid boolean for key: ${condition.key}. Only 'true' or 'false' are accepted.")
            }
        }

        is FMConditionExpr.HasContent ->
            properties[condition.variable]?.let { applyTransforms(it, condition.transforms).isNotEmpty() }
                ?: if (condition.transforms.isNotEmpty()) throw FreemarkerException("Missing property key in ?has_content: ${condition.variable}")
                   else false

        is FMConditionExpr.HasContentWithDefault -> {
            val raw = properties[condition.variable] ?: condition.defaultValue
            applyTransforms(raw, condition.transforms).isNotEmpty()
        }

        is FMConditionExpr.DateCompare -> {
            val lhs = evaluateFMDateExpr(condition.lhs)
            val rhs = evaluateFMDateExpr(condition.rhs)
            compareDateValues(lhs, condition.op, rhs)
        }

        is FMConditionExpr.Not -> !evaluateCondition(condition.inner)
        is FMConditionExpr.And -> evaluateCondition(condition.left) && evaluateCondition(condition.right)
        is FMConditionExpr.Or -> evaluateCondition(condition.left) || evaluateCondition(condition.right)
    }

    // MARK: - Date helpers

    private fun evaluateFMDateExpr(expr: FMDateExpr): FMDateValue = when (expr) {
        is FMDateExpr.Property -> {
            val raw = properties[expr.variable]
                ?: throw FreemarkerException("Missing property key in date comparison: ${expr.variable}")
            parseDateString(raw, expr.kind, expr.variable)
        }

        is FMDateExpr.PropertyWithDefault -> {
            val raw = properties[expr.variable] ?: expr.defaultValue
            parseDateString(raw, expr.kind, expr.variable)
        }

        is FMDateExpr.Now -> {
            val now = SwrveFreemarkerEvaluator.nowProvider()
            when (expr.kind) {
                FMDateKind.DATE, FMDateKind.STRICT_DATE -> FMDateValue.CalendarDate(calendarDate(now))
                FMDateKind.DATE_TIME -> FMDateValue.DateTime(now)
            }
        }
    }

    private fun parseDateString(s: String, kind: FMDateKind, key: String): FMDateValue {
        // ISO 8601 datetimes have 'T' at position 10 (YYYY-MM-DDTxx). Using a positional check
        // avoids false positives from property values that merely contain the letter T.
        val isFullDatetime = s.length >= 11 && s[10] == 'T'
        return when (kind) {
            FMDateKind.DATE -> {
                if (isFullDatetime) {
                    val date = parseISO8601Datetime(s)
                        ?: throw FreemarkerException("?date: cannot parse '$s' as ISO 8601 date or datetime for key: $key")
                    FMDateValue.CalendarDate(calendarDate(date))
                } else {
                    val date = parseISO8601DateOnly(s)
                        ?: throw FreemarkerException("?date: cannot parse '$s' as ISO 8601 date for key: $key")
                    FMDateValue.CalendarDate(calendarDate(date))
                }
            }

            FMDateKind.DATE_TIME -> {
                if (!isFullDatetime) throw FreemarkerException(
                    "?datetime: '$s' is not a full ISO 8601 datetime for key: $key. Date-only strings are not accepted."
                )
                val date = parseISO8601Datetime(s)
                    ?: throw FreemarkerException("?datetime: cannot parse '$s' as ISO 8601 datetime for key: $key")
                FMDateValue.DateTime(date)
            }

            FMDateKind.STRICT_DATE -> {
                if (!isFullDatetime) throw FreemarkerException("?datetime?date: '$s' is not a full ISO 8601 datetime for key: $key.")
                val date = parseISO8601Datetime(s)
                    ?: throw FreemarkerException("?datetime?date: cannot parse '$s' as ISO 8601 datetime for key: $key")
                FMDateValue.CalendarDate(calendarDate(date))
            }
        }
    }

    private fun calendarDate(date: Date): FMCalendarDate {
        val cal = Calendar.getInstance(timezone)
        cal.time = date
        return FMCalendarDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun compareDateValues(lhs: FMDateValue, op: FMNumericOp, rhs: FMDateValue): Boolean {
        return when {
            lhs is FMDateValue.CalendarDate && rhs is FMDateValue.CalendarDate ->
                when (op) {
                    FMNumericOp.GREATER_THAN -> lhs.date > rhs.date
                    FMNumericOp.GREATER_THAN_OR_EQUAL -> lhs.date >= rhs.date
                    FMNumericOp.LESS_THAN -> lhs.date < rhs.date
                    FMNumericOp.LESS_THAN_OR_EQUAL -> lhs.date <= rhs.date
                    else -> throw FreemarkerException("== / != is not supported for date comparisons")
                }

            lhs is FMDateValue.DateTime && rhs is FMDateValue.DateTime ->
                when (op) {
                    FMNumericOp.GREATER_THAN -> lhs.date > rhs.date
                    FMNumericOp.GREATER_THAN_OR_EQUAL -> lhs.date >= rhs.date
                    FMNumericOp.LESS_THAN -> lhs.date < rhs.date
                    FMNumericOp.LESS_THAN_OR_EQUAL -> lhs.date <= rhs.date
                    else -> throw FreemarkerException("== / != is not supported for date comparisons")
                }

            else -> throw FreemarkerException(
                "Type mismatch: cannot compare ?date and ?datetime — use .now?date to compare against a ?date expression, or .now against a ?datetime expression"
            )
        }
    }

    private fun parseISO8601Datetime(s: String): Date? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return try {
                Date.from(Instant.parse(s))
            } catch (e: Exception) {
                null
            }
        }
        // Pre-API 26: normalize offset and truncate fractional seconds to 3 digits before parsing
        val normalized = s
            .replace(Regex("Z$"), "+0000")
            .replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
            .replace(Regex("(T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\d+"), "$1")  // truncate sub-ms digits, keep first 3
        val formats = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ssZ")
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.isLenient = false
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(normalized)
            } catch (e: Exception) { /* try next */
            }
        }
        return null
    }

    private fun parseISO8601DateOnly(s: String): Date? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ld = java.time.LocalDate.parse(s)
            val instant = ld.atStartOfDay(ZoneId.of(timezone.id)).toInstant()
            Date.from(instant)
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.timeZone = timezone
            sdf.parse(s)
        }
    } catch (e: Exception) {
        null
    }
}
