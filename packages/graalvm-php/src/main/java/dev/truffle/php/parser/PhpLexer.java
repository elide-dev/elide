package dev.truffle.php.parser;

/**
 * Lexer for PHP source code.
 *
 * Provides low-level token scanning and position management for the PHP parser.
 * Handles whitespace, comments, keywords, and basic token matching.
 */
public final class PhpLexer {

    private final String code;
    private int pos = 0;
    private int line = 1; // Track current line number (1-indexed)

    public PhpLexer(String code) {
        this.code = code;
    }

    /**
     * Get the current position in the source code.
     */
    public int getPosition() {
        return pos;
    }

    /**
     * Set the current position in the source code.
     * Used for lookahead and backtracking.
     */
    public void setPosition(int pos) {
        this.pos = pos;
    }

    /**
     * Get the current line number (1-indexed).
     */
    public int getCurrentLine() {
        return line;
    }

    /**
     * Check if we've reached the end of the source.
     */
    public boolean isAtEnd() {
        return pos >= code.length();
    }

    /**
     * Peek at the current character without consuming it.
     */
    public char peek() {
        if (isAtEnd()) return '\0';
        return code.charAt(pos);
    }

    /**
     * Peek at the next character without consuming it.
     */
    public char peekNext() {
        if (pos + 1 >= code.length()) return '\0';
        return code.charAt(pos + 1);
    }

    /**
     * Consume and return the current character.
     * Tracks line numbers when encountering newlines.
     */
    public char advance() {
        char c = code.charAt(pos++);
        if (c == '\n') {
            line++;
        }
        return c;
    }

    /**
     * Skip whitespace and comments.
     */
    public void skipWhitespace() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else if (c == '/' && peekNext() == '/') {
                // Skip line comment
                while (!isAtEnd() && peek() != '\n') {
                    advance();
                }
            } else if (c == '/' && peekNext() == '*') {
                // Skip block comment
                advance(); // /
                advance(); // *
                while (!isAtEnd() && !(peek() == '*' && peekNext() == '/')) {
                    advance();
                }
                if (!isAtEnd()) {
                    advance(); // *
                    advance(); // /
                }
            } else {
                break;
            }
        }
    }

    /**
     * Check if the current position matches the expected string.
     * Does not consume the string.
     */
    public boolean check(String expected) {
        if (pos + expected.length() > code.length()) {
            return false;
        }
        return code.substring(pos, pos + expected.length()).equals(expected);
    }

    /**
     * Try to match and consume the expected string.
     *
     * @return true if matched and consumed, false otherwise
     */
    public boolean match(String expected) {
        if (check(expected)) {
            pos += expected.length();
            return true;
        }
        return false;
    }

    /**
     * Match a keyword, ensuring it's followed by a word boundary.
     * Use this for matching PHP keywords to avoid matching prefixes of identifiers.
     *
     * @return true if matched and consumed, false otherwise
     */
    public boolean matchKeyword(String keyword) {
        if (pos + keyword.length() > code.length()) {
            return false;
        }

        // Check if the keyword matches
        if (!code.substring(pos, pos + keyword.length()).equals(keyword)) {
            return false;
        }

        // Check that it's followed by a word boundary (not alphanumeric or underscore)
        if (pos + keyword.length() < code.length()) {
            char nextChar = code.charAt(pos + keyword.length());
            if (Character.isLetterOrDigit(nextChar) || nextChar == '_') {
                return false; // Not a word boundary, so not a keyword match
            }
        }

        // It's a valid keyword match
        pos += keyword.length();
        return true;
    }

    /**
     * Expect a specific string at the current position.
     * Skips whitespace first, then throws an exception if not found.
     *
     * @throws RuntimeException if the expected string is not found
     */
    public void expect(String expected) {
        skipWhitespace();
        if (!match(expected)) {
            throw new RuntimeException("Expected '" + expected + "' at position " + pos);
        }
    }

    /**
     * Skip the PHP open tag (<?php) if present.
     */
    public void skipPhpOpenTag() {
        skipWhitespace();
        if (match("<?php")) {
            skipWhitespace();
        }
    }

    /**
     * Parse a variable name (without the $ prefix).
     * Expects the $ to be consumed already.
     */
    public String parseIdentifier() {
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        return name.toString();
    }

    /**
     * Parse a number string (digits and optional decimal point).
     */
    public String parseNumberString() {
        StringBuilder num = new StringBuilder();
        boolean hasDecimal = false;

        while (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) {
            if (peek() == '.') {
                if (hasDecimal) break;
                hasDecimal = true;
            }
            num.append(advance());
        }

        return num.toString();
    }

    /**
     * Get the full source code.
     */
    public String getCode() {
        return code;
    }
}
