package dev.truffle.php.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpNodeFactory;
import dev.truffle.php.nodes.expression.*;
import dev.truffle.php.runtime.PhpGlobalScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Expression parser for PHP, extracted from the main PhpParser.
 * Handles all expression parsing including operators, literals, variables, and function calls.
 */
public final class PhpExpressionParser {

    private final PhpLexer lexer;
    private final Map<String, Integer> variables;
    private final PhpGlobalScope globalScope;
    private final ParserContext context;

    /**
     * Context object to pass mutable parser state
     */
    public static final class ParserContext {
        public String currentClassName;
        public FrameDescriptor.Builder currentFrameBuilder;

        public ParserContext(String currentClassName, FrameDescriptor.Builder currentFrameBuilder) {
            this.currentClassName = currentClassName;
            this.currentFrameBuilder = currentFrameBuilder;
        }
    }

    public PhpExpressionParser(
            PhpLexer lexer,
            Map<String, Integer> variables,
            PhpGlobalScope globalScope,
            ParserContext context) {
        this.lexer = lexer;
        this.variables = variables;
        this.globalScope = globalScope;
        this.context = context;
    }

    public PhpExpressionNode parseExpression() {
        return parseAssignmentExpression();
    }

    public void updateContext(FrameDescriptor.Builder currentFrameBuilder, String currentClassName) {
        this.context.currentFrameBuilder = currentFrameBuilder;
        this.context.currentClassName = currentClassName;
    }

    private PhpExpressionNode parseAssignmentExpression() {
        PhpExpressionNode left = parseNullCoalescing();

        // Check if this is an assignment or compound assignment
        // Only handle if left side is a simple variable read
        if (left instanceof PhpReadVariableNode) {
            PhpReadVariableNode varNode = (PhpReadVariableNode) left;

            skipWhitespace();

            // Check for compound assignment operators
            PhpCompoundAssignmentNode.CompoundOp compoundOp = null;
            if (match("+=")) {
                compoundOp = PhpCompoundAssignmentNode.CompoundOp.ADD_ASSIGN;
            } else if (match("-=")) {
                compoundOp = PhpCompoundAssignmentNode.CompoundOp.SUB_ASSIGN;
            } else if (match("*=")) {
                compoundOp = PhpCompoundAssignmentNode.CompoundOp.MUL_ASSIGN;
            } else if (match("/=")) {
                compoundOp = PhpCompoundAssignmentNode.CompoundOp.DIV_ASSIGN;
            } else if (match(".=")) {
                compoundOp = PhpCompoundAssignmentNode.CompoundOp.CONCAT_ASSIGN;
            } else if (match("%=")) {
                compoundOp = PhpCompoundAssignmentNode.CompoundOp.MOD_ASSIGN;
            }

            if (compoundOp != null) {
                // Compound assignment
                skipWhitespace();
                PhpExpressionNode rightValue = parseAssignmentExpression();

                // Get variable slot from the read node
                int slot = varNode.getSlot();
                String varName = getVariableName(slot);

                return new PhpCompoundAssignmentNode(varName, slot, compoundOp, rightValue);
            }
        }

        return left;
    }

    private String getVariableName(int slot) {
        // Find variable name by slot
        for (Map.Entry<String, Integer> entry : variables.entrySet()) {
            if (entry.getValue() == slot) {
                return entry.getKey();
            }
        }
        return "unknown";
    }

    private PhpExpressionNode parseNullCoalescing() {
        PhpExpressionNode left = parseTernary();

        while (true) {
            skipWhitespace();
            // Check for ?? (null coalescing) - must check before single ?
            if (check("??")) {
                lexer.setPosition(lexer.getPosition() + 2); // consume ??
                skipWhitespace();
                PhpExpressionNode right = parseTernary();
                left = new PhpNullCoalescingNode(left, right);
            } else {
                break;
            }
        }

        return left;
    }

    private PhpExpressionNode parseTernary() {
        PhpExpressionNode condition = parseLogicalOr();

        skipWhitespace();
        // Check for ? but not ?? (which is null coalescing)
        if (peek() == '?' && peekNext() != '?') {
            advance(); // consume ?
            skipWhitespace();
            PhpExpressionNode trueValue = parseExpression(); // Recursive for right-associativity
            skipWhitespace();
            expect(":");
            skipWhitespace();
            PhpExpressionNode falseValue = parseExpression(); // Recursive for right-associativity
            return new PhpTernaryNode(condition, trueValue, falseValue);
        }

        return condition;
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

        // Check for instanceof operator
        if (matchKeyword("instanceof")) {
            skipWhitespace();
            // Parse class name
            StringBuilder className = new StringBuilder();
            while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                className.append(advance());
            }
            String targetClassName = className.toString();
            if (targetClassName.isEmpty()) {
                throw new RuntimeException("Expected class name after instanceof");
            }
            return new PhpInstanceofNode(left, targetClassName);
        }

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
            // Check for + but not +=
            if (peek() == '+' && peekNext() != '=') {
                advance(); // consume +
                skipWhitespace();
                PhpExpressionNode right = parseMultiplication();
                left = PhpNodeFactory.createAdd(left, right);
            } else if (peek() == '-' && peekNext() != '=') {
                advance(); // consume -
                skipWhitespace();
                PhpExpressionNode right = parseMultiplication();
                left = PhpNodeFactory.createSub(left, right);
            } else if (peek() == '.' && peekNext() != '=' && peekNext() != '.') {
                advance(); // consume .
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
            // Check for * but not *=
            if (peek() == '*' && peekNext() != '=') {
                advance(); // consume *
                skipWhitespace();
                PhpExpressionNode right = parseUnary();
                left = PhpNodeFactory.createMul(left, right);
            } else if (peek() == '/' && peekNext() != '=') {
                advance(); // consume /
                skipWhitespace();
                PhpExpressionNode right = parseUnary();
                left = PhpNodeFactory.createDiv(left, right);
            } else if (peek() == '%' && peekNext() != '=') {
                advance(); // consume %
                skipWhitespace();
                PhpExpressionNode right = parseUnary();
                left = PhpNodeFactory.createMod(left, right);
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

        // Pre-increment operator (check before unary +)
        if (match("++")) {
            skipWhitespace();
            if (peek() != '$') {
                throw new RuntimeException("Pre-increment requires a variable");
            }
            String varName = parseVariableName();
            int slot = getOrCreateVariable(varName);
            return PhpNodeFactory.createPreIncrement(slot);
        }

        // Pre-decrement operator (check before unary -)
        if (match("--")) {
            skipWhitespace();
            if (peek() != '$') {
                throw new RuntimeException("Pre-decrement requires a variable");
            }
            String varName = parseVariableName();
            int slot = getOrCreateVariable(varName);
            return PhpNodeFactory.createPreDecrement(slot);
        }

        // Unary minus (negation)
        if (match("-")) {
            skipWhitespace();
            PhpExpressionNode operand = parseUnary();
            // Implement negation as 0 - operand
            return PhpNodeFactory.createSub(new PhpIntegerLiteralNode(0L), operand);
        }

        // Unary plus (just return the operand)
        if (match("+")) {
            skipWhitespace();
            return parseUnary();
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

            // Check for -> operator (property/method access)
            while (match("->")) {
                skipWhitespace();

                // Parse property or method name
                StringBuilder memberName = new StringBuilder();
                while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                    memberName.append(advance());
                }
                String member = memberName.toString();

                skipWhitespace();

                // Check if it's a method call or property access
                if (match("(")) {
                    // Method call
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

                    varNode = new PhpMethodCallNode(varNode, member, args.toArray(new PhpExpressionNode[0]), context.currentClassName);
                } else {
                    // Property access
                    varNode = new PhpPropertyAccessNode(varNode, member, context.currentClassName);
                }

                skipWhitespace();
            }

            // Check for () - variable function call / __invoke
            skipWhitespace();
            if (match("(")) {
                // Parse arguments
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
                varNode = new PhpInvokeNode(varNode, args.toArray(new PhpExpressionNode[0]));
                skipWhitespace();
            }

            // Check for postfix increment/decrement (only on simple variables, not array access or property access)
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
            return parseStringExpression();
        }

        // Boolean and null
        if (matchKeyword("true")) {
            return new PhpBooleanLiteralNode(true);
        }
        if (matchKeyword("false")) {
            return new PhpBooleanLiteralNode(false);
        }
        if (matchKeyword("null")) {
            return new PhpNullLiteralNode();
        }

        // Number
        if (Character.isDigit(peek())) {
            return parseNumber();
        }

        // Check for 'new' keyword
        if (matchKeyword("new")) {
            skipWhitespace();
            return parseNew();
        }

        // Function call or identifier
        if (Character.isLetter(peek()) || peek() == '_') {
            return parseFunctionCallOrIdentifier();
        }

        throw new RuntimeException("Unexpected character at position " + lexer.getPosition() + ": " + peek());
    }

    private PhpExpressionNode parseNew() {
        // Parse class name
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        String className = name.toString();

        skipWhitespace();
        expect("(");
        skipWhitespace();

        // Parse constructor arguments
        List<PhpExpressionNode> args = new ArrayList<>();
        while (!check(")")) {
            args.add(parseExpression());
            skipWhitespace();
            if (match(",")) {
                skipWhitespace();
            }
        }
        expect(")");

        return new PhpNewNode(className, args.toArray(new PhpExpressionNode[0]));
    }

    private PhpExpressionNode parseFunctionCallOrIdentifier() {
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        String identifier = name.toString();

        skipWhitespace();

        // Check for :: (static member access, self::, or parent:: keyword)
        if (match("::")) {
            skipWhitespace();

            // Check if it's a static property ($) or static method
            if (peek() == '$') {
                // Static property access: ClassName::$property, self::$property, or parent::$property
                String propName = parseVariableName();
                if (identifier.equals("parent")) {
                    throw new RuntimeException("parent::$property access not yet supported");
                } else if (identifier.equals("self")) {
                    if (context.currentClassName == null) {
                        throw new RuntimeException("Cannot use self:: outside of class context");
                    }
                    return new PhpSelfPropertyAccessNode(propName, context.currentClassName);
                }
                return new PhpStaticPropertyAccessNode(identifier, propName);
            } else {
                // Static method call: ClassName::method(), self::method(), or parent::method()
                StringBuilder methodNameBuilder = new StringBuilder();
                while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                    methodNameBuilder.append(advance());
                }
                String methodName = methodNameBuilder.toString();

                skipWhitespace();
                expect("(");
                skipWhitespace();

                List<PhpExpressionNode> args = new ArrayList<>();
                while (!check(")")) {
                    args.add(parseExpression());
                    skipWhitespace();
                    if (match(",")) {
                        skipWhitespace();
                    }
                }
                expect(")");

                // Check if it's a parent:: call
                if (identifier.equals("parent")) {
                    if (context.currentClassName == null) {
                        throw new RuntimeException("Cannot use parent:: outside of class context");
                    }
                    return new PhpParentMethodCallNode(methodName, args.toArray(new PhpExpressionNode[0]), context.currentClassName);
                }

                // Check if it's a self:: call
                if (identifier.equals("self")) {
                    if (context.currentClassName == null) {
                        throw new RuntimeException("Cannot use self:: outside of class context");
                    }
                    return new PhpSelfMethodCallNode(methodName, args.toArray(new PhpExpressionNode[0]), context.currentClassName);
                }

                return new PhpStaticMethodCallNode(identifier, methodName, args.toArray(new PhpExpressionNode[0]));
            }
        }

        if (match("(")) {
            // Check for special language constructs
            if (identifier.equals("isset")) {
                return parseIsset();
            } else if (identifier.equals("empty")) {
                return parseEmpty();
            } else if (identifier.equals("unset")) {
                return parseUnset();
            }

            // Regular function call
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

            // Create function call node (handles both built-in and user-defined functions)
            return new PhpFunctionCallNode(identifier, args.toArray(new PhpExpressionNode[0]));
        } else {
            // Just an identifier (could be a constant or future feature)
            throw new RuntimeException("Unexpected identifier: " + identifier);
        }
    }

    private PhpExpressionNode parseIsset() {
        // isset() can take multiple variables
        List<Integer> slots = new ArrayList<>();
        skipWhitespace();

        while (!check(")")) {
            if (peek() != '$') {
                throw new RuntimeException("isset() expects variable arguments");
            }
            String varName = parseVariableName();
            int slot = getOrCreateVariable(varName);
            slots.add(slot);

            skipWhitespace();
            if (match(",")) {
                skipWhitespace();
            }
        }
        expect(")");

        int[] slotArray = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            slotArray[i] = slots.get(i);
        }
        return new PhpIssetNode(slotArray);
    }

    private PhpExpressionNode parseEmpty() {
        // empty() takes exactly one variable
        skipWhitespace();
        if (peek() != '$') {
            throw new RuntimeException("empty() expects a variable argument");
        }
        String varName = parseVariableName();
        int slot = getOrCreateVariable(varName);
        expect(")");
        return new PhpEmptyNode(slot);
    }

    private PhpExpressionNode parseUnset() {
        // unset() can take multiple variables
        List<Integer> slots = new ArrayList<>();
        skipWhitespace();

        while (!check(")")) {
            if (peek() != '$') {
                throw new RuntimeException("unset() expects variable arguments");
            }
            String varName = parseVariableName();
            int slot = getOrCreateVariable(varName);
            slots.add(slot);

            skipWhitespace();
            if (match(",")) {
                skipWhitespace();
            }
        }
        expect(")");

        int[] slotArray = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            slotArray[i] = slots.get(i);
        }
        return new PhpUnsetNode(slotArray);
    }

    public PhpExpressionNode parseArrayLiteral() {
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

    private String parseVariableName() {
        expect("$");
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        return name.toString();
    }

    private int getOrCreateVariable(String name) {
        // If we're in a function/method scope, use the local frame builder
        // Otherwise, use the global scope for top-level variables
        if (context.currentFrameBuilder != null) {
            // Inside function/method - use local variables map and frame builder
            return variables.computeIfAbsent(name, n -> {
                return context.currentFrameBuilder.addSlot(FrameSlotKind.Illegal, n, null);
            });
        } else {
            // At top level - use global scope
            return globalScope.getOrCreateGlobalSlot(name);
        }
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

    private PhpExpressionNode parseStringExpression() {
        char quote = peek();

        // Single-quoted strings don't support interpolation
        if (quote == '\'') {
            return new PhpStringLiteralNode(parseString());
        }

        // Double-quoted strings support interpolation
        advance(); // consume opening quote
        List<PhpExpressionNode> parts = new ArrayList<>();
        StringBuilder currentLiteral = new StringBuilder();

        while (!isAtEnd() && peek() != quote) {
            if (peek() == '\\') {
                // Handle escape sequences
                advance();
                if (!isAtEnd()) {
                    char escaped = advance();
                    switch (escaped) {
                        case 'n': currentLiteral.append('\n'); break;
                        case 't': currentLiteral.append('\t'); break;
                        case 'r': currentLiteral.append('\r'); break;
                        case '\\': currentLiteral.append('\\'); break;
                        case '"': currentLiteral.append('"'); break;
                        case '$': currentLiteral.append('$'); break; // Escaped $
                        default: currentLiteral.append(escaped); break;
                    }
                }
            } else if (peek() == '$' && peekNext() != '\0' && (Character.isLetter(peekNext()) || peekNext() == '_')) {
                // Found a variable - add current literal if non-empty
                if (currentLiteral.length() > 0) {
                    parts.add(new PhpStringLiteralNode(currentLiteral.toString()));
                    currentLiteral = new StringBuilder();
                }

                // Parse variable
                String varName = parseVariableName();
                int slot = getOrCreateVariable(varName);
                parts.add(PhpNodeFactory.createReadVariable(slot));
            } else {
                currentLiteral.append(advance());
            }
        }

        if (isAtEnd()) {
            throw new RuntimeException("Unterminated string");
        }

        advance(); // consume closing quote

        // Add final literal if present
        if (currentLiteral.length() > 0) {
            parts.add(new PhpStringLiteralNode(currentLiteral.toString()));
        }

        // If no interpolation, return simple literal
        if (parts.isEmpty()) {
            return new PhpStringLiteralNode("");
        }
        if (parts.size() == 1 && parts.get(0) instanceof PhpStringLiteralNode) {
            return parts.get(0);
        }

        // Return interpolation node
        return new PhpStringInterpolationNode(parts.toArray(new PhpExpressionNode[0]));
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

    // Lexing method delegations
    private void skipWhitespace() {
        lexer.skipWhitespace();
    }

    private boolean match(String expected) {
        return lexer.match(expected);
    }

    private boolean matchKeyword(String keyword) {
        return lexer.matchKeyword(keyword);
    }

    private boolean check(String expected) {
        return lexer.check(expected);
    }

    private void expect(String expected) {
        lexer.expect(expected);
    }

    private char peek() {
        return lexer.peek();
    }

    private char peekNext() {
        return lexer.peekNext();
    }

    private char advance() {
        return lexer.advance();
    }

    private boolean isAtEnd() {
        return lexer.isAtEnd();
    }
}
