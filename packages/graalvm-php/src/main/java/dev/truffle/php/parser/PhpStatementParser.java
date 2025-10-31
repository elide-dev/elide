package dev.truffle.php.parser;

import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpNodeFactory;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.nodes.expression.*;
import dev.truffle.php.nodes.statement.*;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpGlobalScope;
import dev.truffle.php.runtime.PhpNamespaceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parser for PHP statements including control flow, assignments, and blocks.
 *
 * This parser handles:
 * - Control flow: if/else, while, for, foreach, switch
 * - Statements: echo, return, throw, try/catch/finally, break, continue
 * - Assignments: variables, arrays, properties, static properties
 * - Blocks: statement grouping with {}
 * - File inclusion: require, include (with _once variants)
 */
public final class PhpStatementParser {

    /**
     * Callback interface for parsing constructs that are defined elsewhere
     * (classes, interfaces, functions) to avoid circular dependencies.
     */
    public interface ParserDelegate {
        PhpStatementNode parseClass(boolean isAbstract);
        PhpStatementNode parseInterface();
        PhpStatementNode parseFunction();
    }

    /**
     * Context object to share mutable state between statement parser and other components.
     */
    public static final class StatementContext {
        public String currentClassName;
        public com.oracle.truffle.api.frame.FrameDescriptor.Builder currentFrameBuilder;

        public StatementContext(String currentClassName,
                              com.oracle.truffle.api.frame.FrameDescriptor.Builder currentFrameBuilder) {
            this.currentClassName = currentClassName;
            this.currentFrameBuilder = currentFrameBuilder;
        }
    }

    private final PhpLexer lexer;
    private final Map<String, Integer> variables;
    private final PhpGlobalScope globalScope;
    private final PhpExpressionParser expressionParser;
    private final StatementContext context;
    private final ParserDelegate delegate;
    private PhpNamespaceContext namespaceContext;  // Set during parsing

    public PhpStatementParser(
            PhpLexer lexer,
            Map<String, Integer> variables,
            PhpGlobalScope globalScope,
            PhpExpressionParser expressionParser,
            StatementContext context,
            ParserDelegate delegate) {
        this.lexer = lexer;
        this.variables = variables;
        this.globalScope = globalScope;
        this.expressionParser = expressionParser;
        this.context = context;
        this.delegate = delegate;
        this.namespaceContext = null;  // Will be set by PhpParser
    }

    /**
     * Set the namespace context for parsing.
     * Called by PhpParser to provide access to namespace resolution.
     */
    public void setNamespaceContext(PhpNamespaceContext namespaceContext) {
        this.namespaceContext = namespaceContext;
    }

    public PhpStatementNode parseStatement() {
        skipWhitespace();

        if (isAtEnd()) {
            return null;
        }

        // namespace statement
        if (matchKeyword("namespace")) {
            return parseNamespace();
        }

        // use statement
        if (matchKeyword("use")) {
            return parseUse();
        }

        // echo statement
        if (matchKeyword("echo")) {
            return parseEcho();
        }

        // require statement
        if (matchKeyword("require_once")) {
            skipWhitespace();
            PhpExpressionNode pathExpr = expressionParser.parseExpression();
            expect(";");
            return new PhpRequireNode(pathExpr, true);
        }

        if (matchKeyword("require")) {
            skipWhitespace();
            PhpExpressionNode pathExpr = expressionParser.parseExpression();
            expect(";");
            return new PhpRequireNode(pathExpr, false);
        }

        // include statement
        if (matchKeyword("include_once")) {
            skipWhitespace();
            PhpExpressionNode pathExpr = expressionParser.parseExpression();
            expect(";");
            return new PhpIncludeNode(pathExpr, true);
        }

        if (matchKeyword("include")) {
            skipWhitespace();
            PhpExpressionNode pathExpr = expressionParser.parseExpression();
            expect(";");
            return new PhpIncludeNode(pathExpr, false);
        }

        // interface definition - delegate to main parser
        if (matchKeyword("interface")) {
            return delegate.parseInterface();
        }

        // abstract class definition - delegate to main parser
        if (matchKeyword("abstract")) {
            skipWhitespace();
            if (matchKeyword("class")) {
                return delegate.parseClass(true);  // true = isAbstract
            } else {
                throw new RuntimeException("Expected 'class' after 'abstract' keyword at position " + lexer.getPosition());
            }
        }

        // class definition - delegate to main parser
        if (matchKeyword("class")) {
            return delegate.parseClass(false);  // false = not abstract
        }

        // function definition - delegate to main parser
        if (matchKeyword("function")) {
            return delegate.parseFunction();
        }

        // return statement
        if (matchKeyword("return")) {
            return parseReturn();
        }

        // throw statement
        if (matchKeyword("throw")) {
            return parseThrow();
        }

        // try/catch/finally statement
        if (matchKeyword("try")) {
            return parseTry();
        }

        // if statement
        if (matchKeyword("if")) {
            return parseIf();
        }

        // while loop
        if (matchKeyword("while")) {
            return parseWhile();
        }

        // foreach loop (check before 'for' to avoid prefix match)
        if (matchKeyword("foreach")) {
            return parseForeach();
        }

        // for loop
        if (matchKeyword("for")) {
            return parseFor();
        }

        // break statement
        if (matchKeyword("break")) {
            return parseBreak();
        }

        // continue statement
        if (matchKeyword("continue")) {
            return parseContinue();
        }

        // switch statement
        if (matchKeyword("switch")) {
            return parseSwitch();
        }

        // Variable assignment or expression starting with $
        if (peek() == '$') {
            // Determine if this is an assignment or expression statement
            // by looking ahead for '=' before ';'
            int savedPos = lexer.getPosition();
            boolean hasAssignmentOperator = false;

            // Scan ahead to find '=' or ';'
            while (!isAtEnd() && peek() != ';') {
                if (peek() == '=') {
                    // Check if it's assignment '=' or compound assignment (+=, -=, etc.)
                    char next = peekNext();
                    if (next != '=') {
                        // Also check previous character to avoid !=, <=, >=, but allow +=, -=, *=, /=, .=, %=
                        int pos = lexer.getPosition();
                        if (pos == 0 || (lexer.getCode().charAt(pos - 1) != '!' &&
                                          lexer.getCode().charAt(pos - 1) != '<' &&
                                          lexer.getCode().charAt(pos - 1) != '>')) {
                            hasAssignmentOperator = true;
                            break;
                        }
                    }
                } else if (peek() == '+' || peek() == '-' || peek() == '*' || peek() == '/' || peek() == '.' || peek() == '%') {
                    // Check for compound assignment operators
                    char next = peekNext();
                    if (next == '=') {
                        hasAssignmentOperator = true;
                        break;
                    }
                }
                advance();
            }

            // Restore position
            lexer.setPosition(savedPos);

            if (hasAssignmentOperator) {
                return parseAssignment();
            } else {
                // Expression statement (like method call: $obj->method())
                PhpExpressionNode expr = expressionParser.parseExpression();
                expect(";");
                return new PhpExpressionStatementNode(expr);
            }
        }

        // Expression statement (like function call, static method call, or increment/decrement)
        // Also handle static property assignment: ClassName::$property = value;
        // Also handle fully qualified names: \Namespace\function()
        if (Character.isLetter(peek()) || peek() == '_' || peek() == '+' || peek() == '-' || peek() == '\\') {
            // Check if this might be a static property assignment
            int savedPos = lexer.getPosition();
            boolean isStaticPropertyAssignment = false;

            // Try to parse identifier and check for ::$...=
            if (Character.isLetter(peek()) || peek() == '_') {
                while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                    advance();
                }
                skipWhitespace();
                if (check("::")) {
                    lexer.setPosition(lexer.getPosition() + 2);
                    skipWhitespace();
                    if (peek() == '$') {
                        // Skip variable name
                        advance(); // $
                        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                            advance();
                        }
                        skipWhitespace();
                        if (peek() == '=') {
                            isStaticPropertyAssignment = true;
                        }
                    }
                }
            }

            // Restore position
            lexer.setPosition(savedPos);

            if (isStaticPropertyAssignment) {
                return parseStaticPropertyAssignment();
            } else {
                PhpExpressionNode expr = expressionParser.parseExpression();
                expect(";");
                return new PhpExpressionStatementNode(expr);
            }
        }

        // Skip semicolons
        if (peek() == ';') {
            lexer.setPosition(lexer.getPosition() + 1);
            return null;
        }

        throw new RuntimeException("Unexpected token at position " + lexer.getPosition() + ": " + peek());
    }

    private PhpStatementNode parseEcho() {
        skipWhitespace();
        List<PhpExpressionNode> expressions = new ArrayList<>();

        do {
            expressions.add(expressionParser.parseExpression());
            skipWhitespace();
        } while (match(","));

        expect(";");
        return new PhpEchoNode(expressions.toArray(new PhpExpressionNode[0]));
    }

    private PhpStatementNode parseIf() {
        skipWhitespace();
        expect("(");
        PhpExpressionNode condition = expressionParser.parseExpression();
        expect(")");
        skipWhitespace();

        PhpStatementNode thenBranch = parseBlock();
        PhpStatementNode elseBranch = null;

        skipWhitespace();
        if (matchKeyword("else")) {
            skipWhitespace();
            elseBranch = parseBlock();
        }

        return new PhpIfNode(condition, thenBranch, elseBranch);
    }

    private PhpStatementNode parseWhile() {
        skipWhitespace();
        expect("(");
        PhpExpressionNode condition = expressionParser.parseExpression();
        expect(")");
        skipWhitespace();

        PhpStatementNode body = parseBlock();
        return new PhpWhileNode(condition, body);
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
                PhpExpressionNode value = expressionParser.parseExpression();
                PhpExpressionNode writeExpr = PhpNodeFactory.createWriteVariable(value, slot);
                init = new PhpExpressionStatementNode(writeExpr);
            } else {
                PhpExpressionNode initExpr = expressionParser.parseExpression();
                init = new PhpExpressionStatementNode(initExpr);
            }
        }
        expect(";");

        skipWhitespace();

        // Parse condition (optional)
        PhpExpressionNode condition = null;
        if (!check(";")) {
            condition = expressionParser.parseExpression();
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
                PhpExpressionNode value = expressionParser.parseExpression();
                increment = PhpNodeFactory.createWriteVariable(value, slot);
            } else {
                increment = expressionParser.parseExpression();
            }
        }
        expect(")");

        skipWhitespace();
        PhpStatementNode body = parseBlock();
        return new PhpForNode(init, condition, increment, body);
    }

    private PhpStatementNode parseForeach() {
        skipWhitespace();
        expect("(");
        skipWhitespace();

        // Parse array expression
        PhpExpressionNode arrayExpr = expressionParser.parseExpression();

        skipWhitespace();
        if (!matchKeyword("as")) {
            throw new RuntimeException("Expected 'as' in foreach loop");
        }
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
        return new PhpForeachNode(arrayExpr, valueSlot, keySlot, body);
    }

    public PhpStatementNode parseBlock() {
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

    private PhpStatementNode parseReturn() {
        skipWhitespace();
        PhpExpressionNode value = null;

        if (!check(";")) {
            value = expressionParser.parseExpression();
        }

        expect(";");
        return new PhpReturnNode(value);
    }

    private PhpStatementNode parseThrow() {
        skipWhitespace();
        PhpExpressionNode exception = expressionParser.parseExpression();
        expect(";");
        return new PhpThrowNode(exception);
    }

    private PhpStatementNode parseTry() {
        skipWhitespace();

        // Parse try block
        PhpStatementNode tryBody = parseBlock();

        // Parse catch clauses (at least one catch or a finally is required)
        List<PhpTryNode.PhpCatchClause> catchClauses = new ArrayList<>();
        skipWhitespace();

        while (matchKeyword("catch")) {
            skipWhitespace();
            expect("(");
            skipWhitespace();

            // Parse exception class name
            StringBuilder exceptionClassName = new StringBuilder();
            while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                exceptionClassName.append(advance());
            }
            String className = exceptionClassName.toString();

            skipWhitespace();

            // Parse exception variable name
            String varName = parseVariableName();

            // Create a frame slot for the exception variable
            int varSlot = getOrCreateVariable(varName);

            skipWhitespace();
            expect(")");
            skipWhitespace();

            // Parse catch body
            PhpStatementNode catchBody = parseBlock();

            catchClauses.add(new PhpTryNode.PhpCatchClause(
                className,
                varName,
                varSlot,
                catchBody
            ));

            skipWhitespace();
        }

        // Parse optional finally block
        PhpStatementNode finallyBody = null;
        if (matchKeyword("finally")) {
            skipWhitespace();
            finallyBody = parseBlock();
        }

        // Validate: must have at least one catch or a finally
        if (catchClauses.isEmpty() && finallyBody == null) {
            throw new RuntimeException("try statement must have at least one catch or finally block");
        }

        return new PhpTryNode(
            tryBody,
            catchClauses.toArray(new PhpTryNode.PhpCatchClause[0]),
            finallyBody
        );
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

    private PhpStatementNode parseSwitch() {
        skipWhitespace();
        expect("(");
        PhpExpressionNode switchExpr = expressionParser.parseExpression();
        expect(")");
        skipWhitespace();
        expect("{");

        List<PhpSwitchNode.CaseClause> cases = new ArrayList<>();
        int defaultCaseIndex = -1;

        skipWhitespace();
        while (!check("}") && !isAtEnd()) {
            if (matchKeyword("case")) {
                skipWhitespace();
                PhpExpressionNode caseValue = expressionParser.parseExpression();
                skipWhitespace();
                expect(":");
                skipWhitespace();

                // Parse statements for this case until we hit another case/default or closing brace
                List<PhpStatementNode> caseStatements = new ArrayList<>();
                while (!check("case") && !check("default") && !check("}") && !isAtEnd()) {
                    PhpStatementNode stmt = parseStatement();
                    if (stmt != null) {
                        caseStatements.add(stmt);
                    }
                    skipWhitespace();
                }

                cases.add(new PhpSwitchNode.CaseClause(
                    caseValue,
                    caseStatements.toArray(new PhpStatementNode[0]),
                    false
                ));
            } else if (matchKeyword("default")) {
                skipWhitespace();
                expect(":");
                skipWhitespace();

                // Parse statements for default case
                List<PhpStatementNode> defaultStatements = new ArrayList<>();
                while (!check("case") && !check("default") && !check("}") && !isAtEnd()) {
                    PhpStatementNode stmt = parseStatement();
                    if (stmt != null) {
                        defaultStatements.add(stmt);
                    }
                    skipWhitespace();
                }

                defaultCaseIndex = cases.size();
                cases.add(new PhpSwitchNode.CaseClause(
                    null,
                    defaultStatements.toArray(new PhpStatementNode[0]),
                    true
                ));
            } else {
                throw new RuntimeException("Expected 'case' or 'default' in switch statement at position " + lexer.getPosition());
            }
            skipWhitespace();
        }

        expect("}");

        return new PhpSwitchNode(
            switchExpr,
            cases.toArray(new PhpSwitchNode.CaseClause[0]),
            defaultCaseIndex
        );
    }

    private PhpStatementNode parseStaticPropertyAssignment() {
        // Parse ClassName::$property = value;
        StringBuilder className = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            className.append(advance());
        }
        String identifier = className.toString();

        skipWhitespace();
        expect("::");
        skipWhitespace();

        String propertyName = parseVariableName();
        skipWhitespace();
        expect("=");
        skipWhitespace();

        PhpExpressionNode value = expressionParser.parseExpression();
        expect(";");

        PhpExpressionNode writeExpr;
        if (identifier.equals("self")) {
            if (context.currentClassName == null) {
                throw new RuntimeException("Cannot use self:: outside of class context");
            }
            writeExpr = new PhpSelfPropertyWriteNode(propertyName, value, context.currentClassName);
        } else if (identifier.equals("parent")) {
            throw new RuntimeException("parent::$property write not yet supported");
        } else {
            writeExpr = new PhpStaticPropertyWriteNode(identifier, propertyName, value);
        }
        return new PhpExpressionStatementNode(writeExpr);
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
                    indexNode = expressionParser.parseExpression();
                }
                indices.add(indexNode);
                expect("]");
                skipWhitespace();
            } while (match("["));

            // Check for property assignment after array access ($arr[idx]->property = value)
            if (match("->")) {
                skipWhitespace();

                // Parse property name
                StringBuilder memberName = new StringBuilder();
                while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                    memberName.append(advance());
                }
                String propertyName = memberName.toString();

                skipWhitespace();
                expect("=");
                skipWhitespace();
                PhpExpressionNode value = expressionParser.parseExpression();
                expect(";");

                // Build the array access first
                PhpExpressionNode arrayAccess = varNode;
                for (PhpExpressionNode index : indices) {
                    arrayAccess = new PhpArrayAccessNode(arrayAccess, index);
                }

                // Then create property write on the result
                PhpExpressionNode writeExpr = new PhpPropertyWriteNode(arrayAccess, propertyName, value, context.currentClassName);
                return new PhpExpressionStatementNode(writeExpr);
            }

            expect("=");
            skipWhitespace();
            PhpExpressionNode value = expressionParser.parseExpression();
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

        // Check for property assignment ($obj->property = value)
        if (match("->")) {
            skipWhitespace();

            // Parse property name
            StringBuilder memberName = new StringBuilder();
            while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                memberName.append(advance());
            }
            String propertyName = memberName.toString();

            skipWhitespace();

            // Check for compound assignment operators on properties
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
                // Compound assignment on property: $obj->prop += value
                skipWhitespace();
                PhpExpressionNode rightValue = expressionParser.parseExpression();
                expect(";");

                // Read current value, apply operation, write back
                PhpExpressionNode readExpr = new PhpPropertyAccessNode(varNode, propertyName, context.currentClassName);
                PhpExpressionNode newValue;
                switch (compoundOp) {
                    case ADD_ASSIGN:
                        newValue = PhpNodeFactory.createAdd(readExpr, rightValue);
                        break;
                    case SUB_ASSIGN:
                        newValue = PhpNodeFactory.createSub(readExpr, rightValue);
                        break;
                    case MUL_ASSIGN:
                        newValue = PhpNodeFactory.createMul(readExpr, rightValue);
                        break;
                    case DIV_ASSIGN:
                        newValue = PhpNodeFactory.createDiv(readExpr, rightValue);
                        break;
                    case CONCAT_ASSIGN:
                        newValue = PhpNodeFactory.createConcat(readExpr, rightValue);
                        break;
                    case MOD_ASSIGN:
                        newValue = PhpNodeFactory.createMod(readExpr, rightValue);
                        break;
                    default:
                        throw new RuntimeException("Unknown compound operator");
                }
                PhpExpressionNode writeExpr = new PhpPropertyWriteNode(varNode, propertyName, newValue, context.currentClassName);
                return new PhpExpressionStatementNode(writeExpr);
            }

            // Regular property assignment
            expect("=");
            skipWhitespace();
            PhpExpressionNode value = expressionParser.parseExpression();
            expect(";");

            PhpExpressionNode writeExpr = new PhpPropertyWriteNode(varNode, propertyName, value, context.currentClassName);
            return new PhpExpressionStatementNode(writeExpr);
        }

        // Check for compound assignment operators (+=, -=, *=, /=, .=, %=)
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
            PhpExpressionNode rightValue = expressionParser.parseExpression();
            expect(";");

            PhpExpressionNode compoundExpr = new PhpCompoundAssignmentNode(varName, slot, compoundOp, rightValue);
            return new PhpExpressionStatementNode(compoundExpr);
        }

        // Regular variable assignment
        expect("=");
        skipWhitespace();
        PhpExpressionNode value = expressionParser.parseExpression();
        expect(";");

        PhpExpressionNode writeExpr = PhpNodeFactory.createWriteVariable(value, slot);
        return new PhpExpressionStatementNode(writeExpr);
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
                return context.currentFrameBuilder.addSlot(com.oracle.truffle.api.frame.FrameSlotKind.Illegal, n, null);
            });
        } else {
            // At top level - use global scope
            return globalScope.getOrCreateGlobalSlot(name);
        }
    }

    // Lexing methods - delegate to PhpLexer
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

    /**
     * Parse namespace declaration.
     * Syntax: namespace MyApp\Controllers;
     */
    private PhpStatementNode parseNamespace() {
        skipWhitespace();

        // Parse namespace name
        StringBuilder namespaceName = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_' || peek() == '\\')) {
            namespaceName.append(advance());
        }

        skipWhitespace();
        expect(";");

        // Update the namespace context
        if (namespaceContext != null) {
            namespaceContext.setCurrentNamespace(namespaceName.toString());
        }

        // Namespace declarations don't produce runtime nodes
        return null;
    }

    /**
     * Parse use statement.
     * Syntax:
     *   use MyApp\Models\User;
     *   use MyApp\Models\User as UserModel;
     *   use function MyApp\Functions\helper;
     *   use const MyApp\Constants\VERSION;
     */
    private PhpStatementNode parseUse() {
        skipWhitespace();

        // Check for function or const use
        boolean isFunction = false;
        boolean isConst = false;

        if (matchKeyword("function")) {
            isFunction = true;
            skipWhitespace();
        } else if (matchKeyword("const")) {
            isConst = true;
            skipWhitespace();
        }

        // Parse qualified name
        StringBuilder qualifiedName = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_' || peek() == '\\')) {
            qualifiedName.append(advance());
        }

        String fqn = qualifiedName.toString();
        String alias = null;

        skipWhitespace();

        // Check for 'as' alias
        if (matchKeyword("as")) {
            skipWhitespace();
            StringBuilder aliasBuilder = new StringBuilder();
            while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                aliasBuilder.append(advance());
            }
            alias = aliasBuilder.toString();
            skipWhitespace();
        }

        expect(";");

        // Update the namespace context
        if (namespaceContext != null) {
            if (isFunction) {
                namespaceContext.addFunctionUse(fqn, alias);
            } else if (isConst) {
                namespaceContext.addConstUse(fqn, alias);
            } else {
                namespaceContext.addClassUse(fqn, alias);
            }
        }

        // Use statements don't produce runtime nodes
        return null;
    }
}
