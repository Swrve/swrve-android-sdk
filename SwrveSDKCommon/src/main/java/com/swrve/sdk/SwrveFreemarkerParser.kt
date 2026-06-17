package com.swrve.sdk

private enum class FMNestingContext {
    TOP_LEVEL, IF_BODY, SWITCH_BODY
}

internal class FMParser(private val tokens: List<FMToken>) {
    private var pos = 0

    fun parse(): List<FMASTNode> = parseBody(FMNestingContext.TOP_LEVEL, 0)

    private fun parseBody(nestingContext: FMNestingContext, depth: Int): List<FMASTNode> {
        if (depth > 100) throw FreemarkerException("Template nesting depth exceeds limit (max 100)")
        val nodes = mutableListOf<FMASTNode>()

        while (pos < tokens.size) {
            when (val token = tokens[pos]) {
                is FMToken.Text -> {
                    nodes.add(FMASTNode.Text(token.value))
                    pos++
                }

                is FMToken.Interpolation -> {
                    nodes.add(FMASTNode.Interpolation(parseInterpolation(token.expr)))
                    pos++
                }

                is FMToken.IfDirective -> {
                    pos++
                    val condition = parseCondition(token.condition)
                    val thenBody = parseBody(FMNestingContext.IF_BODY, depth + 1)
                    val elseIfClauses = mutableListOf<Pair<FMConditionExpr, List<FMASTNode>>>()
                    while (pos < tokens.size && tokens[pos] is FMToken.ElseIfDirective) {
                        val elseIfToken = tokens[pos] as FMToken.ElseIfDirective
                        pos++
                        val elseIfCond = parseCondition(elseIfToken.condition)
                        val elseIfBody = parseBody(FMNestingContext.IF_BODY, depth + 1)
                        elseIfClauses.add(Pair(elseIfCond, elseIfBody))
                    }
                    var elseBody: List<FMASTNode> = emptyList()
                    if (pos < tokens.size && tokens[pos] is FMToken.ElseDirective) {
                        pos++
                        elseBody = parseBody(FMNestingContext.IF_BODY, depth + 1)
                    }
                    if (pos >= tokens.size || tokens[pos] !is FMToken.EndIf) {
                        throw FreemarkerException("Unclosed <#if> — missing </#if>")
                    }
                    pos++
                    nodes.add(FMASTNode.IfStatement(condition, thenBody, elseIfClauses, elseBody))
                }

                is FMToken.ElseIfDirective -> return when (nestingContext) {
                    FMNestingContext.IF_BODY -> nodes
                    FMNestingContext.SWITCH_BODY -> throw FreemarkerException("Unexpected <#elseif> inside <#case> or <#default>")
                    FMNestingContext.TOP_LEVEL -> throw FreemarkerException("Unexpected <#elseif> without matching <#if>")
                }

                is FMToken.ElseDirective -> return when (nestingContext) {
                    FMNestingContext.IF_BODY -> nodes
                    FMNestingContext.SWITCH_BODY -> throw FreemarkerException("Unexpected <#else> inside <#case> or <#default>")
                    FMNestingContext.TOP_LEVEL -> throw FreemarkerException("Unexpected <#else> without matching <#if>")
                }

                is FMToken.EndIf -> return when (nestingContext) {
                    FMNestingContext.IF_BODY -> nodes
                    else -> throw FreemarkerException("Unexpected </#if> without matching <#if>")
                }

                is FMToken.SwitchDirective -> {
                    pos++
                    nodes.add(parseSwitch(token.expr, depth + 1))
                }

                is FMToken.CaseDirective -> return when (nestingContext) {
                    FMNestingContext.SWITCH_BODY -> nodes
                    FMNestingContext.IF_BODY -> throw FreemarkerException("Unexpected <#case> inside <#if>")
                    FMNestingContext.TOP_LEVEL -> throw FreemarkerException("Unexpected <#case> outside <#switch>")
                }

                is FMToken.DefaultDirective -> return when (nestingContext) {
                    FMNestingContext.SWITCH_BODY -> nodes
                    FMNestingContext.IF_BODY -> throw FreemarkerException("Unexpected <#default> inside <#if>")
                    FMNestingContext.TOP_LEVEL -> throw FreemarkerException("Unexpected <#default> outside <#switch>")
                }

                is FMToken.BreakDirective -> return when (nestingContext) {
                    FMNestingContext.SWITCH_BODY -> nodes
                    FMNestingContext.IF_BODY -> throw FreemarkerException("<#break> inside <#if> is not supported — <#break> must appear directly inside <#case> or <#default>")
                    FMNestingContext.TOP_LEVEL -> throw FreemarkerException("Unexpected <#break> outside <#switch>")
                }

                is FMToken.EndSwitch -> return when (nestingContext) {
                    FMNestingContext.SWITCH_BODY -> nodes
                    FMNestingContext.IF_BODY -> throw FreemarkerException("Unexpected </#switch> inside <#if>")
                    FMNestingContext.TOP_LEVEL -> throw FreemarkerException("Unexpected </#switch> without matching <#switch>")
                }
            }
        }

        when (nestingContext) {
            FMNestingContext.IF_BODY -> throw FreemarkerException("Unclosed <#if> — missing </#if>")
            FMNestingContext.SWITCH_BODY -> throw FreemarkerException("Unclosed <#switch> — missing </#switch>")
            FMNestingContext.TOP_LEVEL -> Unit
        }

        return nodes
    }

    private fun parseSwitch(expr: String, depth: Int): FMASTNode {
        val (candidate, transforms) = parseStringTransforms(expr.trim())
        if (candidate.isEmpty()) throw FreemarkerException("Empty variable name in <#switch>")
        val bangRange = findDefaultBang(candidate)
        val variable: String
        val switchDefault: String?
        if (bangRange != null) {
            variable = candidate.substring(0, bangRange.first).trim()
            if (variable.isEmpty()) throw FreemarkerException("Empty variable name in <#switch> before '!'")
            if (variable.contains("!")) throw FreemarkerException("Malformed <#switch> expression — unexpected '!' before the default operator: $variable")
            if (variable.contains("?")) throw FreemarkerException("Unsupported built-in in <#switch> expression: $variable")
            validateBareKey(variable, "<#switch>")
            switchDefault = parseStringLiteral(candidate.substring(bangRange.second).trim(), "!")
        } else {
            if (candidate.contains("!")) throw FreemarkerException("Unsupported '!' syntax in <#switch> expression — use variable!\"default\" with a quoted string literal")
            if (candidate.contains("?")) throw FreemarkerException("Unsupported built-in in <#switch> expression: $candidate")
            variable = candidate
            validateBareKey(variable, "<#switch>")
            switchDefault = null
        }
        val cases = mutableListOf<FMSwitchCase>()
        var defaultBody: List<FMASTNode> = emptyList()
        var hasDefault = false

        while (pos < tokens.size) {
            when (val token = tokens[pos]) {
                is FMToken.EndSwitch -> {
                    pos++
                    return FMASTNode.SwitchStatement(variable, switchDefault, transforms, cases, defaultBody)
                }

                is FMToken.CaseDirective -> {
                    if (hasDefault) throw FreemarkerException("<#case> after <#default> is not allowed — <#default> must be last")
                    pos++
                    val value = parseStringLiteral(token.value, "case")
                    val (body, hasBreak) = parseSwitchCaseBody(depth)
                    cases.add(FMSwitchCase(value, body, hasBreak))
                }

                is FMToken.DefaultDirective -> {
                    if (hasDefault) throw FreemarkerException("Duplicate <#default> inside <#switch>")
                    hasDefault = true
                    pos++
                    val (body, _) = parseSwitchCaseBody(depth)
                    defaultBody = body
                }

                is FMToken.Text -> {
                    if (token.value.trim().isNotEmpty()) {
                        val preview = token.value.take(30).replace("\n", "\\n")
                        throw FreemarkerException("Unexpected text inside <#switch> outside a <#case> or <#default>: \"$preview\"")
                    }
                    pos++
                }

                else -> throw FreemarkerException("Expected <#case>, <#default>, or </#switch> inside <#switch>")
            }
        }
        throw FreemarkerException("Unclosed <#switch> — missing </#switch>")
    }

    private fun parseSwitchCaseBody(depth: Int): Pair<List<FMASTNode>, Boolean> {
        val nodes = parseBody(FMNestingContext.SWITCH_BODY, depth)
        if (pos < tokens.size && tokens[pos] is FMToken.BreakDirective) {
            pos++
            return Pair(nodes, true)
        }
        return Pair(nodes, false)
    }

    internal fun parseCondition(expr: String): FMConditionExpr {
        val s = expr.trim()

        // Parenthesized group — strip outer parens and recurse
        if (s.startsWith("(")) {
            val closeIdx = matchingParen(s, 0)
            if (closeIdx == s.length - 1) {
                return parseCondition(s.substring(1, closeIdx).trim())
            }
        }

        // OR — lowest precedence, split first so && binds tighter
        splitOutsideQuotes(s, "||")?.takeIf { it.size > 1 }?.let { parts ->
            var result = parseCondition(parts[0])
            for (part in parts.drop(1)) result = FMConditionExpr.Or(result, parseCondition(part))
            return result
        }

        // AND — higher precedence than OR
        splitOutsideQuotes(s, "&&")?.takeIf { it.size > 1 }?.let { parts ->
            var result = parseCondition(parts[0])
            for (part in parts.drop(1)) result = FMConditionExpr.And(result, parseCondition(part))
            return result
        }

        // Negation: !condition
        if (s.startsWith("!")) {
            return FMConditionExpr.Not(parseCondition(s.substring(1).trim()))
        }

        // Exists check: variable??
        if (s.endsWith("??")) {
            val varPart = s.dropLast(2).trim()
            if (varPart.contains("?")) throw FreemarkerException("Built-in before ?? is not supported: $varPart")
            validateBareKey(varPart, "??")
            return FMConditionExpr.Exists(varPart)
        }

        parseHasContent(s)?.let { return it }

        // Equality: variable == "literal" or variable?number == 10
        val eqIdx = findOperator("==", s)
        if (eqIdx != -1) {
            val varPart = s.substring(0, eqIdx).trim()
            val rhsRaw = s.substring(eqIdx + 2).trim()
            if (varPart.startsWith("\"") || varPart.startsWith("'"))
                throw FreemarkerException("String literal cannot be used as the left-hand side of == / != — put the variable on the left")
            if (varPart.endsWith("?number")) {
                val numericValue = rhsRaw.toDoubleOrNull()
                if (numericValue != null) {
                    if (numericValue.isNaN() || numericValue.isInfinite())
                        throw FreemarkerException("?number: '$rhsRaw' is not a valid finite numeric literal for == comparison")
                    val base = varPart.dropLast("?number".length).trim()
                    val (variable, transforms) = parseStringTransforms(base)
                    val parenDefault = parseParenthesizedDefault(variable)
                    return if (parenDefault != null)
                        FMConditionExpr.NumericCompareWithDefault(parenDefault.first, transforms, parenDefault.second, FMNumericOp.EQUALS, numericValue)
                    else {
                        throwIfMalformedParenthesizedDefault(variable, "?number")
                        validateBareKey(variable, "?number ==")
                        FMConditionExpr.NumericCompare(variable, FMNumericOp.EQUALS, numericValue, transforms)
                    }
                }
            }
            val literal = parseStringLiteral(rhsRaw, "==")
            val (variable, transforms, defaultValue) = parseVarExpr(varPart)
            return if (defaultValue != null) {
                val parenDefault = run { val t = varPart.trim(); val close = t.indexOf(')'); t.startsWith("(") && close > 0 && t.indexOf('!') in 1 until close }
                FMConditionExpr.EqualsWithDefault(variable, defaultValue, transforms, literal, parenDefault)
            } else
                FMConditionExpr.Equals(variable, transforms, literal)
        }

        // Inequality: variable != "literal" or variable?number != 10
        val neqIdx = findOperator("!=", s)
        if (neqIdx != -1) {
            val varPart = s.substring(0, neqIdx).trim()
            val rhsRaw = s.substring(neqIdx + 2).trim()
            if (varPart.startsWith("\"") || varPart.startsWith("'"))
                throw FreemarkerException("String literal cannot be used as the left-hand side of == / != — put the variable on the left")
            if (varPart.endsWith("?number")) {
                val numericValue = rhsRaw.toDoubleOrNull()
                if (numericValue != null) {
                    if (numericValue.isNaN() || numericValue.isInfinite())
                        throw FreemarkerException("?number: '$rhsRaw' is not a valid finite numeric literal for != comparison")
                    val base = varPart.dropLast("?number".length).trim()
                    val (variable, transforms) = parseStringTransforms(base)
                    val parenDefault = parseParenthesizedDefault(variable)
                    return if (parenDefault != null)
                        FMConditionExpr.NumericCompareWithDefault(parenDefault.first, transforms, parenDefault.second, FMNumericOp.NOT_EQUALS, numericValue)
                    else {
                        throwIfMalformedParenthesizedDefault(variable, "?number")
                        validateBareKey(variable, "?number !=")
                        FMConditionExpr.NumericCompare(variable, FMNumericOp.NOT_EQUALS, numericValue, transforms)
                    }
                }
            }
            val literal = parseStringLiteral(rhsRaw, "!=")
            val (variable, transforms, defaultValue) = parseVarExpr(varPart)
            return if (defaultValue != null) {
                val parenDefault = run { val t = varPart.trim(); val close = t.indexOf(')'); t.startsWith("(") && close > 0 && t.indexOf('!') in 1 until close }
                FMConditionExpr.NotEqualsWithDefault(variable, defaultValue, transforms, literal, parenDefault)
            } else
                FMConditionExpr.NotEquals(variable, transforms, literal)
        }

        parseNumericCompare(s)?.let { return it }
        parseBooleanBuiltin(s)?.let { return it }

        throw FreemarkerException("Unsupported condition expression: $s")
    }

    internal fun parseInterpolation(expr: String): FMInterpolationExpr {
        val s = expr.trim()

        if (s.startsWith("!"))
            throw FreemarkerException("Leading '!' is not valid in \${} interpolation — use \${x!\"default\"} to provide a fallback value")

        // Concatenation: parts joined by "+" (spaces optional) — string literals, variable references, and string transforms (?trim/?lower_case/?upper_case)
        // Must precede parseStringTransformInterpolation to avoid matching a trailing transform on the last concat operand
        splitOutsideQuotes(s, "+")?.takeIf { it.size > 1 }?.let { parts ->
            val concatParts = parts.map { part ->
                val p = part.trim()
                if (p.isEmpty()) throw FreemarkerException("Malformed concatenation: empty operand in $s")
                when {
                    p.startsWith("\"") || p.startsWith("'") ->
                        FMConcatPart.Literal(parseStringLiteral(p, "+"))

                    else -> {
                        // (key!"default")?transforms* as a concat operand
                        if (p.startsWith("(")) {
                            val closeIdx = findMatchingParen(p)
                            if (closeIdx != -1) {
                                val inner = p.substring(1, closeIdx).trim()
                                val bangRange = findDefaultBang(inner)
                                if (bangRange != null) {
                                    val varSection = inner.substring(0, bangRange.first).trim()
                                    if (varSection.contains("?"))
                                        throw FreemarkerException("Built-in chains inside (key!\"default\") are not supported in + concatenation: $varSection")
                                    val defaultPart = inner.substring(bangRange.second).trim()
                                    val defaultValue = parseStringLiteral(defaultPart, "!")
                                    val (remaining, concatTransforms) = parseStringTransforms(p.substring(closeIdx + 1).trim())
                                    if (remaining.isNotEmpty())
                                        throw FreemarkerException("Unsupported syntax after ) in concat paren-default operand: $remaining — only ?trim, ?lower_case, ?upper_case are allowed")
                                    validateBareKey(varSection, "+ concatenation")
                                    return@map FMConcatPart.ParenDefault(varSection, concatTransforms, defaultValue)
                                }
                            }
                        }
                        val (varName, transforms) = parseStringTransforms(p)
                        if (!transforms.isEmpty()) {
                            if (varName.isEmpty()) throw FreemarkerException("Malformed concatenation: empty operand in $s")
                            validateBareKey(varName, "+ concatenation")
                            FMConcatPart.Variable(varName, transforms)
                        } else if (p.contains("!")) {
                            throw FreemarkerException("Default operator '!' is not supported in + concatenation — use \${x!\"default\"} outside the concatenation")
                        } else if (p.contains("?")) {
                            throw FreemarkerException("Only ?trim, ?lower_case, and ?upper_case are supported in + concatenation: $p")
                        } else {
                            validateBareKey(p, "+ concatenation")
                            FMConcatPart.Variable(p, emptyList())
                        }
                    }
                }
            }
            return FMInterpolationExpr.Concat(concatParts)
        }

        // (key!"default")?transforms* in interpolation — paren-default with optional trailing transforms
        if (s.startsWith("(")) {
            val closeIdx = findMatchingParen(s)
            if (closeIdx != -1) {
                val inner = s.substring(1, closeIdx).trim()
                val after = s.substring(closeIdx + 1).trim()
                val bangRange = findDefaultBang(inner)
                if (bangRange != null) {
                    val varSection = inner.substring(0, bangRange.first).trim()
                    if (varSection.contains("?"))
                        throw FreemarkerException("Built-in chains inside (key!\"default\") are not supported — use a plain property key: $varSection")
                    val defaultPart = inner.substring(bangRange.second).trim()
                    val defaultValue = parseStringLiteral(defaultPart, "!")
                    val (remaining, transforms) = parseStringTransforms(after)
                    if (remaining.isNotEmpty())
                        throw FreemarkerException("Unsupported syntax after ) in paren-default expression: $remaining — only ?trim, ?lower_case, ?upper_case are allowed")
                    validateBareKey(varSection, "\${} interpolation")
                    return FMInterpolationExpr.ParenDefaultWithTransform(varSection, transforms, defaultValue)
                }
            }
        }

        parseStringTransformInterpolation(s)?.let { return it }

        // Default value: variable!"literal" (no transforms)
        // Default value: variable!"literal" or bare variable! (empty-string default, with optional transforms)
        val bangIdx = defaultOperatorIndex(s)
        if (bangIdx != -1) {
            val varPart = s.substring(0, bangIdx).trim()
            val defaultPart = s.substring(bangIdx + 1).trim()
            val defaultValue = if (defaultPart.isEmpty()) "" else parseStringLiteral(defaultPart, "!")
            val (variable, transforms) = parseStringTransforms(varPart)
            if (variable.contains("?")) throw FreemarkerException("Unsupported built-in in \${} interpolation: $varPart")
            validateBareKey(variable, "\${} default")
            return FMInterpolationExpr.WithDefault(variable, transforms, defaultValue)
        }

        if (s.contains("?")) throw FreemarkerException("Unsupported built-in in \${} interpolation: $s")
        validateBareKey(s, "\${} interpolation")
        return FMInterpolationExpr.Variable(s, emptyList())
    }

    // MARK: - Private string scanning helpers

    /** Returns the index of `!` default operator (not inside quotes, not at position 0). -1 if not found. */
    private fun defaultOperatorIndex(s: String): Int {
        var inQuotes = false
        var quoteChar = '"'
        for (i in s.indices) {
            val c = s[i]
            if (inQuotes) {
                if (c == quoteChar) inQuotes = false
            } else if (c == '"' || c == '\'') {
                inQuotes = true; quoteChar = c
            } else if (c == '!' && i != 0) {
                return i
            }
        }
        return -1
    }

    /** Splits `s` on every occurrence of `on` not inside quotes or parentheses. Returns null if no split found. */
    private fun splitOutsideQuotes(s: String, on: String): List<String>? {
        val parts = mutableListOf<String>()
        var segmentStart = 0
        var i = 0
        var inQuotes = false
        var quoteChar = '"'
        var parenDepth = 0

        while (i < s.length) {
            val c = s[i]
            when {
                inQuotes -> {
                    if (c == quoteChar) inQuotes = false
                    i++
                }

                c == '"' || c == '\'' -> {
                    inQuotes = true
                    quoteChar = c
                    i++
                }

                c == '(' -> {
                    parenDepth++
                    i++
                }

                c == ')' -> {
                    parenDepth--
                    if (parenDepth < 0) return null
                    i++
                }

                parenDepth == 0 && s.startsWith(on, i) -> {
                    parts.add(s.substring(segmentStart, i).trim())
                    i += on.length
                    segmentStart = i
                }

                else -> i++
            }
        }

        if (parts.isEmpty() || parenDepth != 0) return null
        parts.add(s.substring(segmentStart).trim())
        return parts
    }

    /** Returns the index of `)` matching the `(` at `from`. -1 if unbalanced or not `(` at from. */
    private fun matchingParen(s: String, from: Int): Int {
        if (s[from] != '(') return -1
        var depth = 0
        var inQuotes = false
        var quoteChar = '"'
        var i = from
        while (i < s.length) {
            val c = s[i]
            if (inQuotes) {
                if (c == quoteChar) inQuotes = false
            } else if (c == '"' || c == '\'') {
                inQuotes = true; quoteChar = c
            } else if (c == '(') {
                depth++
            } else if (c == ')') {
                depth--
                if (depth == 0) return i
            }
            i++
        }
        return -1
    }

    /** Returns the index of the first occurrence of `op` not inside a quoted string or parentheses. -1 if not found. */
    private fun findOperator(op: String, s: String): Int {
        var inQuotes = false
        var quoteChar = '"'
        var parenDepth = 0
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                inQuotes -> {
                    if (c == quoteChar) inQuotes = false
                    i++
                }

                c == '"' || c == '\'' -> {
                    inQuotes = true
                    quoteChar = c
                    i++
                }

                c == '(' -> { parenDepth++; i++ }
                c == ')' -> {
                    parenDepth--
                    if (parenDepth < 0) return -1  // unbalanced — stop, consistent with splitOutsideQuotes
                    i++
                }
                parenDepth == 0 && s.startsWith(op, i) -> return i
                else -> i++
            }
        }
        return -1
    }

    /**
     * Parses the LHS of a == / != condition.
     * Returns (variable, transforms, defaultValue?) where defaultValue is non-null when ! is present.
     */
    internal fun parseVarExpr(varPart: String): Triple<String, List<FMStringTransform>, String?> {
        var s = varPart.trim()
        // (key!"default")?transforms* — paren-default with optional trailing string transforms
        if (s.startsWith("(")) {
            val closeIdx = findMatchingParen(s)
            if (closeIdx != -1) {
                val inner = s.substring(1, closeIdx).trim()
                val after = s.substring(closeIdx + 1).trim()
                val bangRange = findDefaultBang(inner)
                if (bangRange != null) {
                    val varSection = inner.substring(0, bangRange.first).trim()
                    if (varSection.contains("?"))
                        throw FreemarkerException("Built-in chains inside (key!\"default\") are not supported — use a plain property key: $varSection")
                    val defaultPart = inner.substring(bangRange.second).trim()
                    val defaultValue = parseStringLiteral(defaultPart, "!")
                    val (remaining, transforms) = parseStringTransforms(after)
                    if (remaining.isNotEmpty())
                        throw FreemarkerException("Unsupported syntax after ) in paren-default expression: $remaining — only ?trim, ?lower_case, ?upper_case are allowed")
                    rejectTerminalBuiltin(varSection)
                    return Triple(varSection, transforms, defaultValue)
                }
            }
        }
        if (s.startsWith("(") && s.endsWith(")")) s = s.substring(1, s.length - 1).trim()
        if (s.startsWith("\"") || s.startsWith("'"))
            throw FreemarkerException("String literal cannot be used as the left-hand side of == / != — put the variable on the left")
        val bangRange = findDefaultBang(s)
        if (bangRange != null) {
            val varSection = s.substring(0, bangRange.first).trim()
            val defaultPart = s.substring(bangRange.second).trim()
            val defaultValue = parseStringLiteral(defaultPart, "!")
            val (variable, transforms) = parseStringTransforms(varSection)
            rejectTerminalBuiltin(variable)
            return Triple(variable, transforms, defaultValue)
        }
        // Bare ! (no explicit default) — x! or x?transforms! — uses "" as the default value
        if (s.endsWith("!")) {
            val varSection = s.dropLast(1).trim()
            val (variable, transforms) = parseStringTransforms(varSection)
            rejectTerminalBuiltin(variable)
            return Triple(variable, transforms, "")
        }
        val (variable, transforms) = parseStringTransforms(s)
        rejectTerminalBuiltin(variable)
        return Triple(variable, transforms, null)
    }

    /** Throws if a resolved key still contains parentheses — indicates an unrecognised paren form that
     *  would silently mis-key the property lookup. The legitimate (key!"default") form is fully consumed
     *  by the parser before any key reaches this point, so parens here are always a bug. */
    internal fun validateBareKey(key: String, context: String) {
        if (key.contains('(') || key.contains(')'))
            throw FreemarkerException("Invalid property key '$key' in $context — parentheses are not valid in a bare key name")
    }

    /** Throws if the variable name ends with a terminal built-in that can't be used in == / !=. */
    private fun rejectTerminalBuiltin(variable: String) {
        validateBareKey(variable, "== / != condition")
        when {
            variable.endsWith("?boolean") || variable.endsWith("?has_content") -> {
                val t = if (variable.endsWith("?boolean")) "?boolean" else "?has_content"
                throw FreemarkerException("$t produces a boolean — use it as a standalone <#if> condition")
            }
            variable.endsWith("?number") ->
                throw FreemarkerException("?number with a quoted string RHS is not supported — use an unquoted numeric literal (e.g. ?number == 10) or gt/gte/lt/lte operators")
            variable.endsWith("?datetime") || variable.endsWith("?date") -> {
                val t = if (variable.endsWith("?datetime")) "?datetime" else "?date"
                throw FreemarkerException("$t produces a date — use gt/gte/lt/lte for date comparisons")
            }
        }
    }

    /** Returns the index of the `)` that closes the `(` at s[0], respecting nested parens and quoted strings. */
    private fun findMatchingParen(s: String): Int {
        var depth = 0
        var inQuotes = false
        var quoteChar = '"'
        for (i in s.indices) {
            val c = s[i]
            if (inQuotes) {
                if (c == quoteChar) inQuotes = false
            } else if (c == '"' || c == '\'') {
                inQuotes = true; quoteChar = c
            } else if (c == '(') depth++
            else if (c == ')') { depth--; if (depth == 0) return i }
        }
        return -1
    }

    /** Returns a Pair<bangIndex, bangIndex+1> for `!` immediately followed by a quote char, outside quoted strings. */
    private fun findDefaultBang(s: String): Pair<Int, Int>? {
        var inQuotes = false
        var quoteChar = '"'
        for (i in s.indices) {
            val c = s[i]
            val next = i + 1
            if (inQuotes) {
                if (c == quoteChar) inQuotes = false
            } else if (c == '"' || c == '\'') {
                inQuotes = true; quoteChar = c
            } else if (c == '!' && next < s.length && (s[next] == '"' || s[next] == '\'')) {
                return Pair(i, next)
            }
        }
        return null
    }

    internal fun parseStringLiteral(s: String, context: String): String {
        val trimmed = s.trim()
        if (trimmed.length >= 2) {
            val quote = trimmed[0]
            if ((quote == '"' || quote == '\'') && trimmed.endsWith(quote)) {
                val interior = trimmed.substring(1, trimmed.length - 1)
                if (!interior.contains(quote)) {
                    return interior
                }
            }
        }
        throw FreemarkerException("Expected string literal for $context operator, got: $trimmed")
    }
}
