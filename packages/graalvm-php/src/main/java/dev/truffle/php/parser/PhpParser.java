package dev.truffle.php.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.Source;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpNodeFactory;
import dev.truffle.php.nodes.PhpRootNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.nodes.expression.*;
import dev.truffle.php.nodes.statement.PhpBlockNode;
import dev.truffle.php.nodes.statement.PhpEchoNode;
import dev.truffle.php.nodes.statement.PhpExpressionStatementNode;
import dev.truffle.php.nodes.statement.PhpIfNode;
import dev.truffle.php.nodes.statement.PhpFunctionNode;
import dev.truffle.php.nodes.statement.PhpReturnNode;
import dev.truffle.php.nodes.statement.PhpBreakNode;
import dev.truffle.php.nodes.statement.PhpContinueNode;
import dev.truffle.php.nodes.PhpFunctionRootNode;
import dev.truffle.php.runtime.PhpFunction;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple hand-written parser for a subset of PHP.
 * This is a basic implementation to get started.
 *
 * Supported syntax:
 * - Literals: integers, floats, strings, booleans, null
 * - Binary operators: +, -, *, /, ., ==, !=, <, >, <=, >=, &&, ||
 * - Unary operators: !
 * - Variables: $name
 * - echo statement
 * - if/else statements
 * - Loops: while, for, foreach
 * - Variable assignments
 * - Functions: definitions and calls
 * - return statement
 * - Arrays: literals, access, and assignment
 */
public final class PhpParser {

    private final PhpLanguage language;
    private final Source source;
    private final String code;
    private int pos = 0;
    private final Map<String, Integer> variables = new HashMap<>();
    private final FrameDescriptor.Builder frameBuilder = FrameDescriptor.newBuilder();
    private final List<PhpFunction> declaredFunctions = new ArrayList<>();

    public PhpParser(PhpLanguage language, Source source) {
        this.language = language;
        this.source = source;
        this.code = source.getCharacters().toString();
    }

    public PhpRootNode parse() {
        skipPhpOpenTag();
        List<PhpStatementNode> statements = new ArrayList<>();

        while (!isAtEnd()) {
            PhpStatementNode stmt = parseStatement();
            if (stmt != null) {
                statements.add(stmt);
            }
        }

        PhpStatementNode body = new PhpBlockNode(statements.toArray(new PhpStatementNode[0]));
        PhpRootNode rootNode = new PhpRootNode(language, frameBuilder.build(), body);

        // Register functions in the context after parsing
        PhpContext context = PhpContext.get(rootNode);
        for (PhpFunction function : declaredFunctions) {
            context.registerFunction(function);
        }

        return rootNode;
    }

    private void skipPhpOpenTag() {
        skipWhitespace();
        if (match("<?php")) {
            skipWhitespace();
        }
    }

    private PhpStatementNode parseStatement() {
        skipWhitespace();

        if (isAtEnd()) {
            return null;
        }

        // echo statement
        if (match("echo")) {
            return parseEcho();
        }

        // function definition
        if (match("function")) {
            return parseFunction();
        }

        // return statement
        if (match("return")) {
            return parseReturn();
        }

        // if statement
        if (match("if")) {
            return parseIf();
        }

        // while loop
        if (match("while")) {
            return parseWhile();
        }

        // foreach loop (check before 'for' to avoid prefix match)
        if (match("foreach")) {
            return parseForeach();
        }

        // for loop
        if (match("for")) {
            return parseFor();
        }

        // break statement
        if (match("break")) {
            return parseBreak();
        }

        // continue statement
        if (match("continue")) {
            return parseContinue();
        }

        // Variable assignment
        if (peek() == '$') {
            return parseAssignment();
        }

        // Expression statement (like function call, or increment/decrement)
        if (Character.isLetter(peek()) || peek() == '_' || peek() == '+' || peek() == '-') {
            PhpExpressionNode expr = parseExpression();
            expect(";");
            return new PhpExpressionStatementNode(expr);
        }

        // Skip semicolons
        if (peek() == ';') {
            pos++;
            return null;
        }

        throw new RuntimeException("Unexpected token at position " + pos + ": " + peek());
    }

    private PhpStatementNode parseEcho() {
        skipWhitespace();
        List<PhpExpressionNode> expressions = new ArrayList<>();

        do {
            expressions.add(parseExpression());
            skipWhitespace();
        } while (match(","));

        expect(";");
        return new PhpEchoNode(expressions.toArray(new PhpExpressionNode[0]));
    }

    private PhpStatementNode parseIf() {
        skipWhitespace();
        expect("(");
        PhpExpressionNode condition = parseExpression();
        expect(")");
        skipWhitespace();

        PhpStatementNode thenBranch = parseBlock();
        PhpStatementNode elseBranch = null;

        skipWhitespace();
        if (match("else")) {
            skipWhitespace();
            elseBranch = parseBlock();
        }

        return new PhpIfNode(condition, thenBranch, elseBranch);
    }

    private PhpStatementNode parseWhile() {
        skipWhitespace();
        expect("(");
        PhpExpressionNode condition = parseExpression();
        expect(")");
        skipWhitespace();

        PhpStatementNode body = parseBlock();
        return new dev.truffle.php.nodes.statement.PhpWhileNode(condition, body);
    }

    private PhpStatementNode parseFor() {
        skipWhitespace();
        expect("(");
        skipWhitespace();

        // Parse init (optional)
        PhpStatementNode init = null;
        if (!check(";")) {
            if (peek() == '$') {
                // For init, we need to parse assignment without the trailing semicolon
                String varName = parseVariableName();
                int slot = getOrCreateVariable(varName);
                skipWhitespace();
                expect("=");
                skipWhitespace();
                PhpExpressionNode value = parseExpression();
                PhpExpressionNode writeExpr = PhpNodeFactory.createWriteVariable(value, slot);
                init = new PhpExpressionStatementNode(writeExpr);
            } else {
                PhpExpressionNode initExpr = parseExpression();
                init = new PhpExpressionStatementNode(initExpr);
            }
        }
        expect(";");

        skipWhitespace();

        // Parse condition (optional)
        PhpExpressionNode condition = null;
        if (!check(";")) {
            condition = parseExpression();
        }
        expect(";");

        skipWhitespace();

        // Parse increment (optional)
        PhpExpressionNode increment = null;
        if (!check(")")) {
            // Parse increment - could be assignment or expression
            if (peek() == '$') {
                // Handle assignment in increment
                String varName = parseVariableName();
                int slot = getOrCreateVariable(varName);
                skipWhitespace();
                expect("=");
                skipWhitespace();
                PhpExpressionNode value = parseExpression();
                increment = PhpNodeFactory.createWriteVariable(value, slot);
            } else {
                increment = parseExpression();
            }
        }
        expect(")");

        skipWhitespace();
        PhpStatementNode body = parseBlock();
        return new dev.truffle.php.nodes.statement.PhpForNode(init, condition, increment, body);
    }

    private PhpStatementNode parseForeach() {
        skipWhitespace();
        expect("(");
        skipWhitespace();

        // Parse array expression
        PhpExpressionNode arrayExpr = parseExpression();

        skipWhitespace();
        expect("as");
        skipWhitespace();

        // Check for key => value syntax
        Integer keySlot = null;
        int valueSlot;

        // Parse first variable (could be key or value)
        String firstVar = parseVariableName();
        int firstSlot = getOrCreateVariable(firstVar);

        skipWhitespace();
        if (match("=>")) {
            // It's key => value syntax
            keySlot = firstSlot;
            skipWhitespace();
            String valueVar = parseVariableName();
            valueSlot = getOrCreateVariable(valueVar);
        } else {
            // It's just value syntax
            valueSlot = firstSlot;
        }

        expect(")");
        skipWhitespace();

        PhpStatementNode body = parseBlock();
        return new dev.truffle.php.nodes.statement.PhpForeachNode(arrayExpr, valueSlot, keySlot, body);
    }

    private PhpStatementNode parseBlock() {
        skipWhitespace();
        if (match("{")) {
            List<PhpStatementNode> statements = new ArrayList<>();
            skipWhitespace();
            while (!check("}") && !isAtEnd()) {
                PhpStatementNode stmt = parseStatement();
                if (stmt != null) {
                    statements.add(stmt);
                }
                skipWhitespace();
            }
            expect("}");
            return new PhpBlockNode(statements.toArray(new PhpStatementNode[0]));
        } else {
            return parseStatement();
        }
    }

    private PhpStatementNode parseFunction() {
        skipWhitespace();

        // Parse function name
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        String functionName = name.toString();

        skipWhitespace();
        expect("(");
        skipWhitespace();

        // Parse parameters
        List<String> paramNames = new ArrayList<>();
        while (!check(")")) {
            String paramName = parseVariableName();
            paramNames.add(paramName);
            skipWhitespace();
            if (match(",")) {
                skipWhitespace();
            }
        }
        expect(")");

        // Create a new frame for the function
        FrameDescriptor.Builder functionFrameBuilder = FrameDescriptor.newBuilder();
        int[] paramSlots = new int[paramNames.size()];

        for (int i = 0; i < paramNames.size(); i++) {
            int slot = functionFrameBuilder.addSlot(FrameSlotKind.Illegal, paramNames.get(i), null);
            paramSlots[i] = slot;
        }

        // Save current parser state
        Map<String, Integer> savedVars = new HashMap<>(this.variables);
        FrameDescriptor.Builder savedFrameBuilder = this.currentFrameBuilder;

        // Clear and set up function's variable scope
        this.variables.clear();
        this.currentFrameBuilder = functionFrameBuilder;

        // Add parameters to function's variable map
        for (int i = 0; i < paramNames.size(); i++) {
            this.variables.put(paramNames.get(i), paramSlots[i]);
        }

        skipWhitespace();
        PhpStatementNode body = parseBlock();

        // Restore parser state
        this.variables.clear();
        this.variables.putAll(savedVars);
        this.currentFrameBuilder = savedFrameBuilder;

        // Create function root node
        PhpFunctionRootNode functionRoot = new PhpFunctionRootNode(
            language,
            functionFrameBuilder.build(),
            functionName,
            paramNames.toArray(new String[0]),
            paramSlots,
            body
        );

        // Create and register function
        PhpFunction function = new PhpFunction(
            functionName,
            functionRoot.getCallTarget(),
            paramNames.size(),
            paramNames.toArray(new String[0])
        );
        declaredFunctions.add(function);

        return new PhpFunctionNode(functionName, function);
    }

    private PhpStatementNode parseReturn() {
        skipWhitespace();
        PhpExpressionNode value = null;

        if (!check(";")) {
            value = parseExpression();
        }

        expect(";");
        return new PhpReturnNode(value);
    }

    private PhpStatementNode parseBreak() {
        skipWhitespace();
        int level = 1;

        // Optional level argument (e.g., break 2;)
        if (Character.isDigit(peek())) {
            level = Integer.parseInt(String.valueOf(advance()));
        }

        expect(";");
        return new PhpBreakNode(level);
    }

    private PhpStatementNode parseContinue() {
        skipWhitespace();
        int level = 1;

        // Optional level argument (e.g., continue 2;)
        if (Character.isDigit(peek())) {
            level = Integer.parseInt(String.valueOf(advance()));
        }

        expect(";");
        return new PhpContinueNode(level);
    }

    private PhpStatementNode parseAssignment() {
        String varName = parseVariableName();
        int slot = getOrCreateVariable(varName);
        PhpExpressionNode varNode = PhpNodeFactory.createReadVariable(slot);

        skipWhitespace();

        // Check for array assignment ($arr[index] = value)
        if (match("[")) {
            List<PhpExpressionNode> indices = new ArrayList<>();
            do {
                skipWhitespace();
                PhpExpressionNode indexNode = null;
                if (!check("]")) {
                    indexNode = parseExpression();
                }
                indices.add(indexNode);
                expect("]");
                skipWhitespace();
            } while (match("["));

            expect("=");
            skipWhitespace();
            PhpExpressionNode value = parseExpression();
            expect(";");

            // Build nested array writes for multi-dimensional arrays
            PhpExpressionNode writeExpr = value;
            for (int i = indices.size() - 1; i >= 0; i--) {
                if (i == 0) {
                    writeExpr = new PhpArrayWriteNode(varNode, indices.get(i), writeExpr);
                } else {
                    // For multi-dimensional, we'd need to handle this differently
                    // For now, just support single-level
                    writeExpr = new PhpArrayWriteNode(varNode, indices.get(i), writeExpr);
                }
            }

            return new PhpExpressionStatementNode(writeExpr);
        }

        // Regular variable assignment
        expect("=");
        skipWhitespace();
        PhpExpressionNode value = parseExpression();
        expect(";");

        PhpExpressionNode writeExpr = PhpNodeFactory.createWriteVariable(value, slot);
        return new PhpExpressionStatementNode(writeExpr);
    }

    private PhpExpressionNode parseArrayLiteral() {
        // Parse array elements
        List<PhpExpressionNode> keys = new ArrayList<>();
        List<PhpExpressionNode> values = new ArrayList<>();
        boolean isAssociative = false;

        skipWhitespace();
        while (!check("]")) {
            PhpExpressionNode element = parseExpression();
            skipWhitespace();

            // Check for => (associative array)
            if (match("=>")) {
                isAssociative = true;
                skipWhitespace();
                PhpExpressionNode value = parseExpression();
                keys.add(element);
                values.add(value);
                skipWhitespace();
            } else {
                values.add(element);
                if (isAssociative) {
                    // Mixed syntax not fully supported, but add null key
                    keys.add(new PhpNullLiteralNode());
                }
            }

            skipWhitespace();
            if (match(",")) {
                skipWhitespace();
            }
        }
        expect("]");

        if (isAssociative) {
            return new PhpArrayLiteralNode(
                keys.toArray(new PhpExpressionNode[0]),
                values.toArray(new PhpExpressionNode[0])
            );
        } else {
            return new PhpArrayLiteralNode(values.toArray(new PhpExpressionNode[0]));
        }
    }

    private PhpExpressionNode parseExpression() {
        return parseLogicalOr();
    }

    private PhpExpressionNode parseLogicalOr() {
        PhpExpressionNode left = parseLogicalAnd();

        while (true) {
            skipWhitespace();
            if (match("||")) {
                skipWhitespace();
                PhpExpressionNode right = parseLogicalAnd();
                left = PhpNodeFactory.createLogicalOr(left, right);
            } else {
                break;
            }
        }

        return left;
    }

    private PhpExpressionNode parseLogicalAnd() {
        PhpExpressionNode left = parseComparison();

        while (true) {
            skipWhitespace();
            if (match("&&")) {
                skipWhitespace();
                PhpExpressionNode right = parseComparison();
                left = PhpNodeFactory.createLogicalAnd(left, right);
            } else {
                break;
            }
        }

        return left;
    }

    private PhpExpressionNode parseComparison() {
        PhpExpressionNode left = parseAddition();

        skipWhitespace();
        // Order matters: check longer operators first (<=, >=, !=, ==)
        if (match("==")) {
            skipWhitespace();
            PhpExpressionNode right = parseAddition();
            return PhpNodeFactory.createEqual(left, right);
        } else if (match("!=")) {
            skipWhitespace();
            PhpExpressionNode right = parseAddition();
            return PhpNodeFactory.createNotEqual(left, right);
        } else if (match("<=")) {
            skipWhitespace();
            PhpExpressionNode right = parseAddition();
            return PhpNodeFactory.createLessOrEqual(left, right);
        } else if (match(">=")) {
            skipWhitespace();
            PhpExpressionNode right = parseAddition();
            return PhpNodeFactory.createGreaterOrEqual(left, right);
        } else if (match("<")) {
            skipWhitespace();
            PhpExpressionNode right = parseAddition();
            return PhpNodeFactory.createLessThan(left, right);
        } else if (match(">")) {
            skipWhitespace();
            PhpExpressionNode right = parseAddition();
            return PhpNodeFactory.createGreaterThan(left, right);
        }

        return left;
    }

    private PhpExpressionNode parseAddition() {
        PhpExpressionNode left = parseMultiplication();

        while (true) {
            skipWhitespace();
            if (match("+")) {
                skipWhitespace();
                PhpExpressionNode right = parseMultiplication();
                left = PhpNodeFactory.createAdd(left, right);
            } else if (match("-")) {
                skipWhitespace();
                PhpExpressionNode right = parseMultiplication();
                left = PhpNodeFactory.createSub(left, right);
            } else if (match(".") && !check(".")) { // . but not ..
                skipWhitespace();
                PhpExpressionNode right = parseMultiplication();
                left = PhpNodeFactory.createConcat(left, right);
            } else {
                break;
            }
        }

        return left;
    }

    private PhpExpressionNode parseMultiplication() {
        PhpExpressionNode left = parseUnary();

        while (true) {
            skipWhitespace();
            if (match("*")) {
                skipWhitespace();
                PhpExpressionNode right = parseUnary();
                left = PhpNodeFactory.createMul(left, right);
            } else if (match("/")) {
                skipWhitespace();
                PhpExpressionNode right = parseUnary();
                left = PhpNodeFactory.createDiv(left, right);
            } else {
                break;
            }
        }

        return left;
    }

    private PhpExpressionNode parseUnary() {
        skipWhitespace();

        // Logical NOT operator
        if (match("!")) {
            skipWhitespace();
            PhpExpressionNode operand = parseUnary();
            return PhpNodeFactory.createLogicalNot(operand);
        }

        // Pre-increment operator
        if (match("++")) {
            skipWhitespace();
            if (peek() != '$') {
                throw new RuntimeException("Pre-increment requires a variable");
            }
            String varName = parseVariableName();
            int slot = getOrCreateVariable(varName);
            return PhpNodeFactory.createPreIncrement(slot);
        }

        // Pre-decrement operator
        if (match("--")) {
            skipWhitespace();
            if (peek() != '$') {
                throw new RuntimeException("Pre-decrement requires a variable");
            }
            String varName = parseVariableName();
            int slot = getOrCreateVariable(varName);
            return PhpNodeFactory.createPreDecrement(slot);
        }

        return parsePrimary();
    }

    private PhpExpressionNode parsePrimary() {
        skipWhitespace();

        // Parentheses
        if (match("(")) {
            PhpExpressionNode expr = parseExpression();
            expect(")");
            return expr;
        }

        // Variable or array access
        if (peek() == '$') {
            String varName = parseVariableName();
            int slot = getOrCreateVariable(varName);
            PhpExpressionNode varNode = PhpNodeFactory.createReadVariable(slot);

            // Check for array access
            skipWhitespace();
            while (match("[")) {
                skipWhitespace();
                PhpExpressionNode indexNode = null;
                if (!check("]")) {
                    indexNode = parseExpression();
                }
                expect("]");
                varNode = new PhpArrayAccessNode(varNode, indexNode);
                skipWhitespace();
            }

            // Check for postfix increment/decrement (only on simple variables, not array access)
            if (varNode instanceof PhpReadVariableNode) {
                skipWhitespace();
                if (match("++")) {
                    return PhpNodeFactory.createPostIncrement(slot);
                } else if (match("--")) {
                    return PhpNodeFactory.createPostDecrement(slot);
                }
            }

            return varNode;
        }

        // Array literal with []
        if (match("[")) {
            return parseArrayLiteral();
        }

        // String literal
        if (peek() == '"' || peek() == '\'') {
            return new PhpStringLiteralNode(parseString());
        }

        // Boolean and null
        if (match("true")) {
            return new PhpBooleanLiteralNode(true);
        }
        if (match("false")) {
            return new PhpBooleanLiteralNode(false);
        }
        if (match("null")) {
            return new PhpNullLiteralNode();
        }

        // Number
        if (Character.isDigit(peek())) {
            return parseNumber();
        }

        // Function call or identifier
        if (Character.isLetter(peek()) || peek() == '_') {
            return parseFunctionCallOrIdentifier();
        }

        throw new RuntimeException("Unexpected character at position " + pos + ": " + peek());
    }

    private PhpExpressionNode parseFunctionCallOrIdentifier() {
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        String identifier = name.toString();

        skipWhitespace();
        if (match("(")) {
            // Function call
            List<PhpExpressionNode> args = new ArrayList<>();
            skipWhitespace();

            while (!check(")")) {
                args.add(parseExpression());
                skipWhitespace();
                if (match(",")) {
                    skipWhitespace();
                }
            }
            expect(")");

            return new PhpFunctionCallNode(identifier, args.toArray(new PhpExpressionNode[0]));
        } else {
            // Just an identifier (could be a constant or future feature)
            throw new RuntimeException("Unexpected identifier: " + identifier);
        }
    }

    private String parseVariableName() {
        expect("$");
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        return name.toString();
    }

    private FrameDescriptor.Builder currentFrameBuilder;

    private int getOrCreateVariable(String name) {
        FrameDescriptor.Builder builder = currentFrameBuilder != null ? currentFrameBuilder : frameBuilder;
        return variables.computeIfAbsent(name, n -> {
            return builder.addSlot(FrameSlotKind.Illegal, n, null);
        });
    }

    private PhpExpressionNode parseNumber() {
        StringBuilder num = new StringBuilder();
        boolean hasDecimal = false;

        while (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) {
            if (peek() == '.') {
                if (hasDecimal) break;
                hasDecimal = true;
            }
            num.append(advance());
        }

        if (hasDecimal) {
            return new PhpFloatLiteralNode(Double.parseDouble(num.toString()));
        } else {
            return new PhpIntegerLiteralNode(Long.parseLong(num.toString()));
        }
    }

    private String parseString() {
        char quote = advance(); // consume opening quote
        StringBuilder str = new StringBuilder();

        while (!isAtEnd() && peek() != quote) {
            if (peek() == '\\') {
                advance();
                if (!isAtEnd()) {
                    char escaped = advance();
                    switch (escaped) {
                        case 'n': str.append('\n'); break;
                        case 't': str.append('\t'); break;
                        case 'r': str.append('\r'); break;
                        case '\\': str.append('\\'); break;
                        case '"': str.append('"'); break;
                        case '\'': str.append('\''); break;
                        default: str.append(escaped); break;
                    }
                }
            } else {
                str.append(advance());
            }
        }

        if (isAtEnd()) {
            throw new RuntimeException("Unterminated string");
        }

        advance(); // consume closing quote
        return str.toString();
    }

    private void skipWhitespace() {
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

    private boolean match(String expected) {
        if (check(expected)) {
            pos += expected.length();
            return true;
        }
        return false;
    }

    private boolean check(String expected) {
        if (pos + expected.length() > code.length()) {
            return false;
        }
        return code.substring(pos, pos + expected.length()).equals(expected);
    }

    private void expect(String expected) {
        skipWhitespace();
        if (!match(expected)) {
            throw new RuntimeException("Expected '" + expected + "' at position " + pos);
        }
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return code.charAt(pos);
    }

    private char peekNext() {
        if (pos + 1 >= code.length()) return '\0';
        return code.charAt(pos + 1);
    }

    private char advance() {
        return code.charAt(pos++);
    }

    private boolean isAtEnd() {
        return pos >= code.length();
    }
}
