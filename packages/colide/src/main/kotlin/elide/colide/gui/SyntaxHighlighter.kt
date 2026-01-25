/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.colide.gui

/**
 * # Syntax Highlighter
 *
 * Token-based syntax highlighting for the CodeEditor.
 * Supports Kotlin, JavaScript, Python, Rust, and plain text.
 *
 * ## Usage
 * ```kotlin
 * val highlighter = SyntaxHighlighter.forExtension("kt")
 * val tokens = highlighter.tokenize("fun main() { println(\"Hello\") }")
 * for (token in tokens) {
 *     drawText(token.text, token.color)
 * }
 * ```
 */
public class SyntaxHighlighter private constructor(
    private val language: Language
) {
    
    /**
     * Supported languages.
     */
    public enum class Language {
        KOTLIN,
        JAVASCRIPT,
        PYTHON,
        RUST,
        C,
        JSON,
        MARKDOWN,
        PLAIN
    }
    
    /**
     * Token types for syntax coloring.
     */
    public enum class TokenType {
        KEYWORD,
        TYPE,
        STRING,
        NUMBER,
        COMMENT,
        OPERATOR,
        PUNCTUATION,
        IDENTIFIER,
        FUNCTION,
        ANNOTATION,
        PLAIN
    }
    
    /**
     * A syntax token with text, type, and color.
     */
    public data class Token(
        val text: String,
        val type: TokenType,
        val start: Int,
        val end: Int
    ) {
        val color: Int get() = when (type) {
            TokenType.KEYWORD -> Theme.keyword
            TokenType.TYPE -> Theme.type
            TokenType.STRING -> Theme.string
            TokenType.NUMBER -> Theme.number
            TokenType.COMMENT -> Theme.comment
            TokenType.OPERATOR -> Theme.operator
            TokenType.PUNCTUATION -> Theme.punctuation
            TokenType.IDENTIFIER -> Theme.identifier
            TokenType.FUNCTION -> Theme.function
            TokenType.ANNOTATION -> Theme.annotation
            TokenType.PLAIN -> Theme.plain
        }
    }
    
    /**
     * Color theme for syntax highlighting.
     */
    public object Theme {
        public var keyword: Int = 0x00c678dd      // Purple
        public var type: Int = 0x0061afef         // Blue
        public var string: Int = 0x0098c379       // Green
        public var number: Int = 0x00d19a66       // Orange
        public var comment: Int = 0x005c6370      // Gray
        public var operator: Int = 0x0056b6c2     // Cyan
        public var punctuation: Int = 0x00abb2bf  // Light gray
        public var identifier: Int = 0x00e5c07b   // Yellow
        public var function: Int = 0x0061afef     // Blue
        public var annotation: Int = 0x00e06c75   // Red
        public var plain: Int = 0x00abb2bf        // Light gray
        
        public fun setDarkTheme() {
            keyword = 0x00c678dd
            type = 0x0061afef
            string = 0x0098c379
            number = 0x00d19a66
            comment = 0x005c6370
            operator = 0x0056b6c2
            punctuation = 0x00abb2bf
            identifier = 0x00e5c07b
            function = 0x0061afef
            annotation = 0x00e06c75
            plain = 0x00abb2bf
        }
        
        public fun setLightTheme() {
            keyword = 0x00a626a4
            type = 0x004078f2
            string = 0x0050a14f
            number = 0x00986801
            comment = 0x00a0a1a7
            operator = 0x000184bc
            punctuation = 0x00383a42
            identifier = 0x00c18401
            function = 0x004078f2
            annotation = 0x00e45649
            plain = 0x00383a42
        }
    }
    
    /**
     * Tokenize a line of code.
     */
    public fun tokenizeLine(line: String): List<Token> {
        return when (language) {
            Language.KOTLIN -> tokenizeKotlin(line)
            Language.JAVASCRIPT -> tokenizeJavaScript(line)
            Language.PYTHON -> tokenizePython(line)
            Language.RUST -> tokenizeRust(line)
            Language.C -> tokenizeC(line)
            Language.JSON -> tokenizeJson(line)
            Language.MARKDOWN -> tokenizeMarkdown(line)
            Language.PLAIN -> listOf(Token(line, TokenType.PLAIN, 0, line.length))
        }
    }
    
    private fun tokenizeKotlin(line: String): List<Token> {
        return tokenizeGeneric(line, KOTLIN_KEYWORDS, KOTLIN_TYPES, "//", "/*", "*/", "@")
    }
    
    private fun tokenizeJavaScript(line: String): List<Token> {
        return tokenizeGeneric(line, JS_KEYWORDS, JS_TYPES, "//", "/*", "*/", null)
    }
    
    private fun tokenizePython(line: String): List<Token> {
        return tokenizeGeneric(line, PYTHON_KEYWORDS, PYTHON_TYPES, "#", "\"\"\"", "\"\"\"", "@")
    }
    
    private fun tokenizeRust(line: String): List<Token> {
        return tokenizeGeneric(line, RUST_KEYWORDS, RUST_TYPES, "//", "/*", "*/", "#")
    }
    
    private fun tokenizeC(line: String): List<Token> {
        return tokenizeGeneric(line, C_KEYWORDS, C_TYPES, "//", "/*", "*/", "#")
    }
    
    private fun tokenizeJson(line: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        
        while (i < line.length) {
            val ch = line[i]
            
            when {
                ch.isWhitespace() -> {
                    val start = i
                    while (i < line.length && line[i].isWhitespace()) i++
                    tokens.add(Token(line.substring(start, i), TokenType.PLAIN, start, i))
                }
                ch == '"' -> {
                    val start = i
                    i++
                    while (i < line.length && line[i] != '"') {
                        if (line[i] == '\\' && i + 1 < line.length) i++
                        i++
                    }
                    if (i < line.length) i++
                    tokens.add(Token(line.substring(start, i), TokenType.STRING, start, i))
                }
                ch.isDigit() || (ch == '-' && i + 1 < line.length && line[i + 1].isDigit()) -> {
                    val start = i
                    if (ch == '-') i++
                    while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] == 'e' || line[i] == 'E')) i++
                    tokens.add(Token(line.substring(start, i), TokenType.NUMBER, start, i))
                }
                ch in "{}[],:".toSet() -> {
                    tokens.add(Token(ch.toString(), TokenType.PUNCTUATION, i, i + 1))
                    i++
                }
                else -> {
                    val start = i
                    while (i < line.length && !line[i].isWhitespace() && line[i] !in "{}[],:\"".toSet()) i++
                    val word = line.substring(start, i)
                    val type = when (word) {
                        "true", "false", "null" -> TokenType.KEYWORD
                        else -> TokenType.PLAIN
                    }
                    tokens.add(Token(word, type, start, i))
                }
            }
        }
        
        return tokens
    }
    
    private fun tokenizeMarkdown(line: String): List<Token> {
        val tokens = mutableListOf<Token>()
        
        when {
            line.startsWith("#") -> {
                tokens.add(Token(line, TokenType.KEYWORD, 0, line.length))
            }
            line.startsWith("```") -> {
                tokens.add(Token(line, TokenType.STRING, 0, line.length))
            }
            line.startsWith(">") -> {
                tokens.add(Token(line, TokenType.COMMENT, 0, line.length))
            }
            line.startsWith("- ") || line.startsWith("* ") || line.matches(Regex("^\\d+\\..*")) -> {
                tokens.add(Token(line.take(2), TokenType.OPERATOR, 0, 2))
                if (line.length > 2) {
                    tokens.add(Token(line.drop(2), TokenType.PLAIN, 2, line.length))
                }
            }
            else -> {
                tokens.add(Token(line, TokenType.PLAIN, 0, line.length))
            }
        }
        
        return tokens
    }
    
    private fun tokenizeGeneric(
        line: String,
        keywords: Set<String>,
        types: Set<String>,
        lineComment: String,
        blockCommentStart: String,
        blockCommentEnd: String,
        annotationPrefix: String?
    ): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        
        while (i < line.length) {
            val ch = line[i]
            
            when {
                line.substring(i).startsWith(lineComment) -> {
                    tokens.add(Token(line.substring(i), TokenType.COMMENT, i, line.length))
                    return tokens
                }
                
                ch.isWhitespace() -> {
                    val start = i
                    while (i < line.length && line[i].isWhitespace()) i++
                    tokens.add(Token(line.substring(start, i), TokenType.PLAIN, start, i))
                }
                
                ch == '"' || ch == '\'' -> {
                    val start = i
                    val quote = ch
                    i++
                    while (i < line.length && line[i] != quote) {
                        if (line[i] == '\\' && i + 1 < line.length) i++
                        i++
                    }
                    if (i < line.length) i++
                    tokens.add(Token(line.substring(start, i), TokenType.STRING, start, i))
                }
                
                ch.isDigit() -> {
                    val start = i
                    while (i < line.length && (line[i].isDigit() || line[i] in "._xXabcdefABCDEFLlFf".toSet())) i++
                    tokens.add(Token(line.substring(start, i), TokenType.NUMBER, start, i))
                }
                
                annotationPrefix != null && ch.toString() == annotationPrefix && (i == 0 || !line[i - 1].isLetterOrDigit()) -> {
                    val start = i
                    i++
                    while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) i++
                    tokens.add(Token(line.substring(start, i), TokenType.ANNOTATION, start, i))
                }
                
                ch.isLetter() || ch == '_' -> {
                    val start = i
                    while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) i++
                    val word = line.substring(start, i)
                    
                    val type = when {
                        word in keywords -> TokenType.KEYWORD
                        word in types -> TokenType.TYPE
                        i < line.length && line[i] == '(' -> TokenType.FUNCTION
                        word[0].isUpperCase() -> TokenType.TYPE
                        else -> TokenType.IDENTIFIER
                    }
                    tokens.add(Token(word, type, start, i))
                }
                
                ch in "+-*/%=<>!&|^~?:".toSet() -> {
                    val start = i
                    while (i < line.length && line[i] in "+-*/%=<>!&|^~?:".toSet()) i++
                    tokens.add(Token(line.substring(start, i), TokenType.OPERATOR, start, i))
                }
                
                ch in "(){}[].,;".toSet() -> {
                    tokens.add(Token(ch.toString(), TokenType.PUNCTUATION, i, i + 1))
                    i++
                }
                
                else -> {
                    tokens.add(Token(ch.toString(), TokenType.PLAIN, i, i + 1))
                    i++
                }
            }
        }
        
        return tokens
    }
    
    public companion object {
        private val KOTLIN_KEYWORDS = setOf(
            "fun", "val", "var", "class", "interface", "object", "enum", "sealed",
            "data", "annotation", "companion", "private", "public", "protected", "internal",
            "override", "open", "final", "abstract", "if", "else", "when", "for", "while",
            "do", "return", "break", "continue", "throw", "try", "catch", "finally",
            "import", "package", "as", "is", "in", "out", "by", "where", "init",
            "constructor", "suspend", "inline", "crossinline", "noinline", "reified",
            "typealias", "expect", "actual", "true", "false", "null", "this", "super"
        )
        
        private val KOTLIN_TYPES = setOf(
            "Int", "Long", "Short", "Byte", "Float", "Double", "Boolean", "Char",
            "String", "Any", "Unit", "Nothing", "Array", "List", "Set", "Map",
            "MutableList", "MutableSet", "MutableMap", "Pair", "Triple"
        )
        
        private val JS_KEYWORDS = setOf(
            "function", "const", "let", "var", "class", "extends", "if", "else",
            "for", "while", "do", "switch", "case", "default", "break", "continue",
            "return", "throw", "try", "catch", "finally", "new", "delete", "typeof",
            "instanceof", "in", "of", "import", "export", "from", "as", "async",
            "await", "yield", "true", "false", "null", "undefined", "this", "super"
        )
        
        private val JS_TYPES = setOf(
            "Array", "Object", "String", "Number", "Boolean", "Function", "Symbol",
            "Map", "Set", "WeakMap", "WeakSet", "Promise", "Date", "RegExp", "Error"
        )
        
        private val PYTHON_KEYWORDS = setOf(
            "def", "class", "if", "elif", "else", "for", "while", "try", "except",
            "finally", "with", "as", "import", "from", "return", "yield", "raise",
            "break", "continue", "pass", "lambda", "and", "or", "not", "in", "is",
            "True", "False", "None", "global", "nonlocal", "assert", "async", "await"
        )
        
        private val PYTHON_TYPES = setOf(
            "int", "float", "str", "bool", "list", "dict", "set", "tuple", "bytes",
            "bytearray", "object", "type", "range", "frozenset", "complex"
        )
        
        private val RUST_KEYWORDS = setOf(
            "fn", "let", "mut", "const", "static", "struct", "enum", "impl", "trait",
            "pub", "priv", "mod", "use", "as", "if", "else", "match", "for", "while",
            "loop", "break", "continue", "return", "move", "ref", "self", "Self",
            "true", "false", "where", "async", "await", "dyn", "unsafe", "extern"
        )
        
        private val RUST_TYPES = setOf(
            "i8", "i16", "i32", "i64", "i128", "isize", "u8", "u16", "u32", "u64",
            "u128", "usize", "f32", "f64", "bool", "char", "str", "String", "Vec",
            "Box", "Rc", "Arc", "Option", "Result", "HashMap", "HashSet"
        )
        
        private val C_KEYWORDS = setOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if",
            "inline", "int", "long", "register", "restrict", "return", "short",
            "signed", "sizeof", "static", "struct", "switch", "typedef", "union",
            "unsigned", "void", "volatile", "while", "_Bool", "_Complex", "_Imaginary"
        )
        
        private val C_TYPES = setOf(
            "int", "char", "float", "double", "void", "long", "short", "unsigned",
            "signed", "size_t", "ptrdiff_t", "int8_t", "int16_t", "int32_t", "int64_t",
            "uint8_t", "uint16_t", "uint32_t", "uint64_t", "bool", "FILE"
        )
        
        private val EXTENSION_MAP = mapOf(
            "kt" to Language.KOTLIN,
            "kts" to Language.KOTLIN,
            "js" to Language.JAVASCRIPT,
            "ts" to Language.JAVASCRIPT,
            "jsx" to Language.JAVASCRIPT,
            "tsx" to Language.JAVASCRIPT,
            "py" to Language.PYTHON,
            "rs" to Language.RUST,
            "c" to Language.C,
            "h" to Language.C,
            "cpp" to Language.C,
            "hpp" to Language.C,
            "json" to Language.JSON,
            "md" to Language.MARKDOWN,
            "markdown" to Language.MARKDOWN
        )
        
        /**
         * Get highlighter for file extension.
         */
        @JvmStatic
        public fun forExtension(ext: String): SyntaxHighlighter {
            val lang = EXTENSION_MAP[ext.lowercase()] ?: Language.PLAIN
            return SyntaxHighlighter(lang)
        }
        
        /**
         * Get highlighter for language.
         */
        @JvmStatic
        public fun forLanguage(language: Language): SyntaxHighlighter {
            return SyntaxHighlighter(language)
        }
    }
}
