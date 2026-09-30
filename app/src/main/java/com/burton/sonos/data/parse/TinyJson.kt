package com.burton.sonos.data.parse

object TinyJson {
    fun stringify(value: Any?): String = when (value) {
        null -> "null"
        is Boolean -> value.toString()
        is Number -> value.toString()
        is String -> "\"${escape(value)}\""
        is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { (key, nested) ->
            "\"${escape(key.toString())}\":${stringify(nested)}"
        }
        is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { stringify(it) }
        else -> "\"${escape(value.toString())}\""
    }

    fun parse(text: String): Any? = Parser(text).parseValue()

    @Suppress("UNCHECKED_CAST")
    fun parseObject(text: String): Map<String, Any?> =
        (parse(text) as? Map<String, Any?>).orEmpty()

    @Suppress("UNCHECKED_CAST")
    fun parseArray(text: String): List<Any?> =
        (parse(text) as? List<Any?>).orEmpty()

    fun Map<String, Any?>.str(key: String, fallback: String = ""): String =
        this[key] as? String ?: fallback

    fun Map<String, Any?>.int(key: String, fallback: Int = 0): Int {
        val value = this[key] ?: return fallback
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: fallback
            else -> fallback
        }
    }

    fun Map<String, Any?>.bool(key: String, fallback: Boolean = false): Boolean {
        val value = this[key] ?: return fallback
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            else -> fallback
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun Map<String, Any?>.objList(key: String): List<Map<String, Any?>> =
        (this[key] as? List<*>)?.mapNotNull { it as? Map<String, Any?> }.orEmpty()

    @Suppress("UNCHECKED_CAST")
    fun Map<String, Any?>.strList(key: String): List<String> =
        (this[key] as? List<*>)?.mapNotNull { it as? String }.orEmpty()

    private fun escape(value: String) = buildString(value.length) {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

    private class Parser(private val source: String) {
        private var index = 0

        fun parseValue(): Any? {
            skip()
            if (index >= source.length) return null
            return when (val char = source[index]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBool()
                'n' -> parseNull()
                '-', in '0'..'9' -> parseNumber()
                else -> error("Unexpected '$char' at $index")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val out = linkedMapOf<String, Any?>()
            skip()
            if (peek() == '}') {
                index++
                return out
            }
            while (true) {
                skip()
                val key = parseString()
                skip()
                expect(':')
                out[key] = parseValue()
                skip()
                when (peek()) {
                    ',' -> index++
                    '}' -> {
                        index++
                        return out
                    }
                    else -> error("Expected , or } at $index")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val out = ArrayList<Any?>()
            skip()
            if (peek() == ']') {
                index++
                return out
            }
            while (true) {
                out += parseValue()
                skip()
                when (peek()) {
                    ',' -> index++
                    ']' -> {
                        index++
                        return out
                    }
                    else -> error("Expected , or ] at $index")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val out = StringBuilder()
            while (index < source.length) {
                when (val char = source[index++]) {
                    '"' -> return out.toString()
                    '\\' -> {
                        val escaped = source.getOrNull(index++) ?: break
                        out.append(
                            when (escaped) {
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                else -> escaped
                            },
                        )
                    }
                    else -> out.append(char)
                }
            }
            error("Unterminated string")
        }

        private fun parseBool(): Boolean {
            return when {
                source.startsWith("true", index) -> {
                    index += 4
                    true
                }
                source.startsWith("false", index) -> {
                    index += 5
                    false
                }
                else -> error("Expected boolean at $index")
            }
        }

        private fun parseNull(): Any? {
            check(source.startsWith("null", index)) { "Expected null at $index" }
            index += 4
            return null
        }

        private fun parseNumber(): Number {
            val start = index
            if (peek() == '-') index++
            while (peek()?.isDigit() == true) index++
            var decimal = false
            if (peek() == '.') {
                decimal = true
                index++
                while (peek()?.isDigit() == true) index++
            }
            val raw = source.substring(start, index)
            return if (decimal) raw.toDouble() else raw.toLong()
        }

        private fun skip() {
            while (peek()?.isWhitespace() == true) index++
        }

        private fun expect(char: Char) {
            skip()
            check(peek() == char) { "Expected '$char' at $index" }
            index++
        }

        private fun peek(): Char? = source.getOrNull(index)
    }
}
