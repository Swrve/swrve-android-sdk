package com.swrve.sdk

internal class FMTokenizer(private val input: String) {
    private var pos = 0

    // Returns true when the character at directiveStart is preceded only by spaces/tabs back to the
    // start of its line (or to the start of the input). When true, the trailing newline after the
    // directive close is stripped — mirroring FreeMarker's directive-only-line whitespace rule.
    private fun isDirectiveOnOwnLine(directiveStart: Int): Boolean {
        var i = directiveStart - 1
        while (i >= 0 && input[i] != '\n' && input[i] != '\r') {
            if (input[i] != ' ' && input[i] != '\t') return false
            i--
        }
        return true
    }

    private fun skipLineBreak(directiveStart: Int) {
        if (!isDirectiveOnOwnLine(directiveStart)) return
        if (pos + 1 < input.length && input[pos] == '\r' && input[pos + 1] == '\n') pos += 2
        else if (pos < input.length && (input[pos] == '\n' || input[pos] == '\r')) pos++
    }

    /** Returns the index of the first `}` not inside a quoted string, scanning from startPos. Returns -1 if not found. */
    private fun findClosingBrace(startPos: Int): Int {
        var inQuotes = false
        var quoteChar = '"'
        var i = startPos
        while (i < input.length) {
            val c = input[i]
            if (inQuotes) {
                if (c == quoteChar) inQuotes = false
            } else if (c == '"' || c == '\'') {
                inQuotes = true
                quoteChar = c
            } else if (c == '}') {
                return i
            }
            i++
        }
        return -1
    }

    /** Returns the index of the first `>` not inside a quoted string, scanning from startPos. Returns -1 if not found. */
    private fun findClosingAngle(startPos: Int): Int {
        var inQuotes = false
        var quoteChar = '"'
        var i = startPos
        while (i < input.length) {
            val c = input[i]
            if (inQuotes) {
                if (c == quoteChar) inQuotes = false
            } else if (c == '"' || c == '\'') {
                inQuotes = true
                quoteChar = c
            } else if (c == '>') {
                return i
            }
            i++
        }
        return -1
    }

    fun tokenize(): List<FMToken> {
        val tokens = mutableListOf<FMToken>()
        while (pos < input.length) {
            when {
                input.startsWith("</#", pos) -> {
                    val directiveStart = pos
                    val endIdx = findClosingAngle(pos + 3)
                    if (endIdx == -1) throw FreemarkerException("Unclosed closing directive")
                    val inner = input.substring(pos + 3, endIdx).trim()
                    when (inner) {
                        "if" -> tokens.add(FMToken.EndIf)
                        "switch" -> tokens.add(FMToken.EndSwitch)
                        else -> throw FreemarkerException("Unsupported closing directive: </#$inner>")
                    }
                    pos = endIdx + 1
                    skipLineBreak(directiveStart)
                }

                input.startsWith("<#", pos) -> {
                    val directiveStart = pos
                    val endIdx = findClosingAngle(pos + 2)
                    if (endIdx == -1) throw FreemarkerException("Unclosed directive tag")
                    val inner = input.substring(pos + 2, endIdx).trim()
                    when {
                        inner == "else" -> tokens.add(FMToken.ElseDirective)
                        inner == "default" -> tokens.add(FMToken.DefaultDirective)
                        inner == "break" -> tokens.add(FMToken.BreakDirective)
                        inner.startsWith("if ") || inner.startsWith("if\t") ->
                            tokens.add(FMToken.IfDirective(inner.substring(3).trim()))

                        inner.startsWith("elseif ") || inner.startsWith("elseif\t") ->
                            tokens.add(FMToken.ElseIfDirective(inner.substring(7).trim()))

                        inner.startsWith("switch ") || inner.startsWith("switch\t") ->
                            tokens.add(FMToken.SwitchDirective(inner.substring(7).trim()))

                        inner.startsWith("case ") || inner.startsWith("case\t") ->
                            tokens.add(FMToken.CaseDirective(inner.substring(5).trim()))

                        else -> throw FreemarkerException("Unsupported directive: <#$inner>")
                    }
                    pos = endIdx + 1
                    skipLineBreak(directiveStart)
                }

                input.startsWith("\${", pos) -> {
                    val endIdx = findClosingBrace(pos + 2)
                    if (endIdx == -1) throw FreemarkerException("Unclosed interpolation \${ }")
                    tokens.add(FMToken.Interpolation(input.substring(pos + 2, endIdx)))
                    pos = endIdx + 1
                }

                else -> {
                    val markers = listOf("<#", "</#", "\${")
                    var nearest = -1
                    for (marker in markers) {
                        val idx = input.indexOf(marker, pos)
                        if (idx != -1 && (nearest == -1 || idx < nearest)) nearest = idx
                    }
                    if (nearest == -1) {
                        tokens.add(FMToken.Text(input.substring(pos)))
                        pos = input.length
                    } else {
                        tokens.add(FMToken.Text(input.substring(pos, nearest)))
                        pos = nearest
                    }
                }
            }
        }
        return tokens
    }
}
