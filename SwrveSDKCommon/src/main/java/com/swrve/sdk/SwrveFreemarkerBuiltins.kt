package com.swrve.sdk

// Built-in helper methods for FMParser.
// Mirrors the structure of SwrveFreemarkerBuiltins.swift on iOS.

// MARK: - Date built-in helpers

internal fun FMParser.dateKindOf(expr: FMDateExpr): FMDateKind = when (expr) {
    is FMDateExpr.Property -> expr.kind
    is FMDateExpr.PropertyWithDefault -> expr.kind
    is FMDateExpr.Now -> expr.kind
}

internal fun FMParser.isDateExpression(s: String): Boolean =
    s.endsWith("?date") || s.endsWith("?datetime") || s == ".now"

// Throws if the expression looks like an attempted (key!"default") that could not be parsed —
// i.e. starts with '(' and contains '!', but didn't match the expected form.
internal fun FMParser.throwIfMalformedParenthesizedDefault(s: String, context: String) {
    if (s.startsWith("(") && s.contains("!"))
        throw FreemarkerException("Malformed parenthesized default in $context: $s — expected (key!\"default\") form")
}

// Detects (key!"default") form and returns (key, defaultValue), or null if not in that form.
internal fun FMParser.parseParenthesizedDefault(s: String): Pair<String, String>? {
    val trimmed = s.trim()
    if (!trimmed.startsWith("(") || !trimmed.endsWith(")")) return null
    val inner = trimmed.drop(1).dropLast(1).trim()
    val bangIdx = inner.indexOf('!')
    if (bangIdx == -1) return null
    val key = inner.substring(0, bangIdx).trim()
    if (key.isEmpty() || key.contains("?") || key.contains("(") || key.contains(")")) return null
    val defaultPart = inner.substring(bangIdx + 1).trim()
    val defaultValue = try { parseStringLiteral(defaultPart, "!") } catch (_: FreemarkerException) { return null }
    return Pair(key, defaultValue)
}

internal fun FMParser.parseDateExpr(s: String): FMDateExpr {
    val trimmed = s.trim()
    return when {
        trimmed.endsWith("?datetime?date") -> {
            val base = trimmed.dropLast("?datetime?date".length).trim()
            if (base == ".now") FMDateExpr.Now(FMDateKind.DATE) else {
                validateBareKey(base, "?datetime?date")
                FMDateExpr.Property(base, FMDateKind.STRICT_DATE)
            }
        }

        trimmed.endsWith("?datetime") -> {
            val base = trimmed.dropLast("?datetime".length).trim()
            if (base == ".now") FMDateExpr.Now(FMDateKind.DATE_TIME) else {
                validateBareKey(base, "?datetime")
                FMDateExpr.Property(base, FMDateKind.DATE_TIME)
            }
        }

        trimmed.endsWith("?date") -> {
            val base = trimmed.dropLast("?date".length).trim()
            when {
                base == ".now" -> FMDateExpr.Now(FMDateKind.DATE)
                else -> parseParenthesizedDefault(base)?.let { (key, defVal) ->
                    FMDateExpr.PropertyWithDefault(key, defVal, FMDateKind.DATE)
                } ?: run {
                    throwIfMalformedParenthesizedDefault(base, "?date")
                    validateBareKey(base, "?date")
                    FMDateExpr.Property(base, FMDateKind.DATE)
                }
            }
        }

        trimmed == ".now" -> FMDateExpr.Now(FMDateKind.DATE_TIME)
        else -> throw FreemarkerException("Expected ?date or ?datetime built-in on: $trimmed")
    }
}

// MARK: - String transform helpers

// Peels zero or more string-transform built-ins from the right of an expression,
// returning the base variable name and transforms in left-to-right application order.
// Recognised transforms: ?trim, ?lower_case, ?upper_case
internal fun FMParser.parseStringTransforms(s: String): Pair<String, List<FMStringTransform>> {
    val knownTransforms = listOf(
        "?lower_case" to FMStringTransform.LOWER_CASE,
        "?upper_case" to FMStringTransform.UPPER_CASE,
        "?trim" to FMStringTransform.TRIM
    )
    var remaining = s.trim()
    val transforms = mutableListOf<FMStringTransform>()
    var changed = true
    while (changed) {
        changed = false
        for ((suffix, transform) in knownTransforms) {
            if (remaining.endsWith(suffix)) {
                remaining = remaining.dropLast(suffix.length).trim()
                transforms.add(0, transform)
                changed = true
                break
            }
        }
    }
    return Pair(remaining, transforms)
}

// MARK: - Numeric built-in helpers

// Returns the index of the first occurrence of `sub` in `s` that is outside quotes and parens, or -1.
private fun indexOutsideQuotes(s: String, sub: String): Int {
    var i = 0
    var inQuotes = false
    var quoteChar = '"'
    var parenDepth = 0
    while (i < s.length) {
        val c = s[i]
        when {
            inQuotes -> { if (c == quoteChar) inQuotes = false; i++ }
            c == '"' || c == '\'' -> { inQuotes = true; quoteChar = c; i++ }
            c == '(' -> { parenDepth++; i++ }
            c == ')' -> { if (--parenDepth < 0) return -1; i++ }
            parenDepth == 0 && s.startsWith(sub, i) -> return i
            else -> i++
        }
    }
    return -1
}

// gt/gte/lt/lte comparisons for both ?number and ?date/?datetime operands
internal fun FMParser.parseNumericCompare(s: String): FMConditionExpr? {
    val ops = listOf(
        " gte " to FMNumericOp.GREATER_THAN_OR_EQUAL,
        " lte " to FMNumericOp.LESS_THAN_OR_EQUAL,
        " gt " to FMNumericOp.GREATER_THAN,
        " lt " to FMNumericOp.LESS_THAN
    )
    for ((opStr, op) in ops) {
        val idx = indexOutsideQuotes(s, opStr)
        if (idx != -1) {
            val varPart = s.substring(0, idx).trim()
            val rhsPart = s.substring(idx + opStr.length).trim()
            if (isDateExpression(varPart)) {
                val lhsExpr = parseDateExpr(varPart)
                val rhsExpr = parseDateExpr(rhsPart)
                if ((dateKindOf(lhsExpr) == FMDateKind.DATE_TIME) != (dateKindOf(rhsExpr) == FMDateKind.DATE_TIME))
                    throw FreemarkerException("Type mismatch: cannot mix ?date and ?datetime in the same comparison — use .now?date with ?date or ?datetime?date, or .now with ?datetime")
                return FMConditionExpr.DateCompare(lhsExpr, op, rhsExpr)
            }
            if (!varPart.endsWith("?number")) {
                throw FreemarkerException("Numeric comparison requires ?number built-in on left-hand side: $varPart")
            }
            val value = rhsPart.toDoubleOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }
                ?: throw FreemarkerException("Expected finite numeric literal for ${opStr.trim()} operator, got: $rhsPart")
            val (variable, transforms) = parseStringTransforms(varPart.dropLast("?number".length).trim())
            val parenDefault = parseParenthesizedDefault(variable)
            return if (parenDefault != null)
                FMConditionExpr.NumericCompareWithDefault(parenDefault.first, transforms, parenDefault.second, op, value)
            else {
                throwIfMalformedParenthesizedDefault(variable, "?number")
                validateBareKey(variable, "?number")
                FMConditionExpr.NumericCompare(variable, op, value, transforms)
            }
        }
    }
    return null
}

// MARK: - Boolean built-in helpers

// ?boolean — supports any string-transform chain before ?boolean: e.g. ?trim?boolean, ?lower_case?boolean, ?trim?lower_case?boolean
internal fun FMParser.parseBooleanBuiltin(s: String): FMConditionExpr? {
    if (!s.endsWith("?boolean")) return null
    val prefix = s.dropLast("?boolean".length).trim()
    val (variable, transforms) = parseStringTransforms(prefix)
    val parenDefault = parseParenthesizedDefault(variable)
    return if (parenDefault != null)
        FMConditionExpr.BooleanValueWithDefault(parenDefault.first, transforms, parenDefault.second)
    else {
        throwIfMalformedParenthesizedDefault(variable, "?boolean")
        validateBareKey(variable, "?boolean")
        FMConditionExpr.BooleanValue(variable, transforms)
    }
}

// MARK: - String built-in helpers

// ?has_content — supports any string-transform chain before ?has_content
internal fun FMParser.parseHasContent(s: String): FMConditionExpr? {
    if (!s.endsWith("?has_content")) return null
    val prefix = s.dropLast("?has_content".length).trim()
    val (variable, transforms) = parseStringTransforms(prefix)
    val parenDefault = parseParenthesizedDefault(variable)
    return if (parenDefault != null)
        FMConditionExpr.HasContentWithDefault(parenDefault.first, transforms, parenDefault.second)
    else {
        throwIfMalformedParenthesizedDefault(variable, "?has_content")
        validateBareKey(variable, "?has_content")
        FMConditionExpr.HasContent(variable, transforms)
    }
}

// String-transform built-ins and optional default in ${} interpolation context.
// Handles: variable?transforms*!"default", variable?transforms*
// Also validates that ?has_content cannot appear in interpolation.
// Returns null if no string-transform built-ins are present (caller handles bare variable and bare !default).
internal fun FMParser.parseStringTransformInterpolation(s: String): FMInterpolationExpr? {
    if (s.endsWith("?has_content")) {
        throw FreemarkerException("?has_content produces a boolean and cannot be used in \${} interpolation")
    }

    // Look for a default-operator bang followed by a quote char: variable?transforms*!"literal"
    var bangIdx = -1
    var inQuotes = false
    var quoteChar = '"'
    for (i in s.indices) {
        val c = s[i]
        val next = i + 1
        if (inQuotes) {
            if (c == quoteChar) inQuotes = false
        } else if (c == '"' || c == '\'') {
            inQuotes = true; quoteChar = c
        } else if (c == '!' && next < s.length && (s[next] == '"' || s[next] == '\'') && i != 0) {
            bangIdx = i
            break
        }
    }

    if (bangIdx != -1) {
        val prefix = s.substring(0, bangIdx).trim()
        val defaultPart = s.substring(bangIdx + 1).trim()
        val defaultValue = parseStringLiteral(defaultPart, "!")
        val (variable, transforms) = parseStringTransforms(prefix)
        if (transforms.isEmpty()) return null  // let caller handle bare variable!"default"
        validateBareKey(variable, "\${} transform+default")
        return FMInterpolationExpr.WithDefault(variable, transforms, defaultValue)
    }

    // No default operator — just transforms
    val (variable, transforms) = parseStringTransforms(s)
    if (transforms.isEmpty()) return null
    validateBareKey(variable, "\${} transform")
    return FMInterpolationExpr.Variable(variable, transforms)
}
