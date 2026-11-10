package com.wedgess.luas.data.timetable

import java.io.PushbackReader
import java.io.Reader

private const val MAX_FIELD_CHARS = 64 * 1024
private const val MAX_COLUMNS = 128

/** RFC4180 records: quoted commas, escaped quotes and embedded CR/LF are preserved. No line-based splitting. */
internal class GtfsCsv(reader: Reader) {
    private val input = PushbackReader(reader.buffered(), 1)
    private val headers = requireNotNull(record()) { "CSV has no header" }.map { it.trim().removePrefix("\uFEFF") }

    init {
        require(headers.all { it.isNotBlank() } && headers.distinct().size == headers.size) { "Invalid CSV header" }
    }

    fun forEach(action: (Map<String, String>) -> Unit) {
        while (true) {
            val fields = record() ?: return
            if (fields.size == 1 && fields[0].isEmpty()) continue
            require(fields.size == headers.size) { "CSV column count changed" }
            action(headers.zip(fields).toMap())
        }
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth") // Bounded CSV state machine, covered by format tests.
    private fun record(): List<String>? {
        val fields = mutableListOf<String>()
        val value = StringBuilder()
        var quoted = false
        var closedQuote = false
        var seen = false
        while (true) {
            val next = input.read()
            if (next == -1) {
                require(!quoted) { "Unclosed CSV quote" }
                return if (!seen) null else fields + value.toString()
            }
            seen = true
            val char = next.toChar()
            when {
                quoted && char == '"' -> {
                    val following = input.read()
                    if (following == '"'.code) {
                        value.append('"')
                    } else {
                        quoted = false
                        closedQuote = true
                        if (following != -1) input.unread(following)
                    }
                }
                quoted -> value.append(char)
                char == '"' && value.isEmpty() && !closedQuote -> quoted = true
                char == ',' -> {
                    fields.add(value.toString())
                    require(fields.size < MAX_COLUMNS) { "Too many CSV columns" }
                    value.clear()
                    closedQuote = false
                }
                char == '\r' || char == '\n' -> {
                    if (char == '\r') {
                        val following = input.read()
                        if (following != '\n'.code && following != -1) input.unread(following)
                    }
                    return fields + value.toString()
                }
                else -> {
                    require(!closedQuote && char != '"') { "Invalid CSV quote" }
                    value.append(char)
                }
            }
            require(value.length <= MAX_FIELD_CHARS) { "CSV field is too long" }
        }
    }
}

internal fun Map<String, String>.required(key: String): String =
    get(key)?.takeIf { it.isNotBlank() } ?: error("GTFS field missing: $key")
