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
import dev.truffle.php.nodes.statement.PhpClassNode;
import dev.truffle.php.nodes.statement.PhpThrowNode;
import dev.truffle.php.nodes.statement.PhpTryNode;
import dev.truffle.php.nodes.PhpFunctionRootNode;
import dev.truffle.php.nodes.PhpMethodRootNode;
import dev.truffle.php.runtime.PhpFunction;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpArray;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpObject;
import dev.truffle.php.runtime.Visibility;
import com.oracle.truffle.api.CallTarget;

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
 * - Unary operators: !, -, +, ++, --
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
    private final List<PhpClass> declaredClasses = new ArrayList<>();
    private final Map<String, String> classParentNames = new HashMap<>();  // className -> parentClassName
    private String currentClassName = null;  // Track current class context during parsing

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

        // Get context to access built-in classes
        PhpContext context = PhpContext.get(rootNode);

        // Resolve parent class references
        resolveClassInheritance(context);

        // Register functions and classes in the context after parsing
        for (PhpFunction function : declaredFunctions) {
            context.registerFunction(function);
        }
        for (PhpClass phpClass : declaredClasses) {
            context.registerClass(phpClass);
        }

        return rootNode;
    }

    private void resolveClassInheritance(PhpContext context) {
        // Build a map of class names to PhpClass objects for quick lookup
        Map<String, PhpClass> classMap = new HashMap<>();
        for (PhpClass phpClass : declaredClasses) {
            classMap.put(phpClass.getName(), phpClass);
        }

        // Resolve parent class references
        for (PhpClass phpClass : declaredClasses) {
            String parentName = classParentNames.get(phpClass.getName());
            if (parentName != null) {
                // Find the parent class - check current file classes first, then built-in classes
                PhpClass parentClass = classMap.get(parentName);
                if (parentClass == null) {
                    // Check for built-in classes in context
                    parentClass = context.getClass(parentName);
                }
                if (parentClass == null) {
                    throw new RuntimeException("Parent class not found: " + parentName + " for class " + phpClass.getName());
                }

                // Check for circular inheritance
                if (hasCircularInheritance(phpClass.getName(), parentClass, classMap)) {
                    throw new RuntimeException("Circular inheritance detected for class: " + phpClass.getName());
                }

                // Set the parent class
                phpClass.setParentClass(parentClass);
            }
        }
    }

    private boolean hasCircularInheritance(String originalClassName, PhpClass currentClass, Map<String, PhpClass> classMap) {
        // Walk up the inheritance chain
        PhpClass current = currentClass;
        while (current != null) {
            if (current.getName().equals(originalClassName)) {
                return true; // Found a cycle
            }
            String parentName = classParentNames.get(current.getName());
            if (parentName == null) {
                break; // No more parents
            }
            current = classMap.get(parentName);
        }
        return false;
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
        if (matchKeyword("echo")) {
            return parseEcho();
        }

        // class definition
        if (matchKeyword("class")) {
            return parseClass();
        }

        // function definition
        if (matchKeyword("function")) {
            return parseFunction();
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
            int savedPos = pos;
            boolean hasAssignmentOperator = false;

            // Scan ahead to find '=' or ';'
            while (!isAtEnd() && peek() != ';') {
                if (peek() == '=') {
                    // Check if it's assignment '=' or compound assignment (+=, -=, etc.)
                    char next = peekNext();
                    if (next != '=') {
                        // Also check previous character to avoid !=, <=, >=, but allow +=, -=, *=, /=, .=, %=
                        if (pos == 0 || (code.charAt(pos - 1) != '!' &&
                                          code.charAt(pos - 1) != '<' &&
                                          code.charAt(pos - 1) != '>')) {
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
            pos = savedPos;

            if (hasAssignmentOperator) {
                return parseAssignment();
            } else {
                // Expression statement (like method call: $obj->method())
                PhpExpressionNode expr = parseExpression();
                expect(";");
                return new PhpExpressionStatementNode(expr);
            }
        }

        // Expression statement (like function call, static method call, or increment/decrement)
        // Also handle static property assignment: ClassName::$property = value;
        if (Character.isLetter(peek()) || peek() == '_' || peek() == '+' || peek() == '-') {
            // Check if this might be a static property assignment
            int savedPos = pos;
            boolean isStaticPropertyAssignment = false;

            // Try to parse identifier and check for ::$...=
            if (Character.isLetter(peek()) || peek() == '_') {
                while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                    advance();
                }
                skipWhitespace();
                if (check("::")) {
                    pos += 2;
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
            pos = savedPos;

            if (isStaticPropertyAssignment) {
                return parseStaticPropertyAssignment();
            } else {
                PhpExpressionNode expr = parseExpression();
                expect(";");
                return new PhpExpressionStatementNode(expr);
            }
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
        if (matchKeyword("else")) {
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

    private PhpStatementNode parseClass() {
        skipWhitespace();

        // Parse class name
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        String className = name.toString();

        skipWhitespace();

        // Check for extends keyword
        String parentClassName = null;
        if (matchKeyword("extends")) {
            skipWhitespace();
            StringBuilder parentName = new StringBuilder();
            while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                parentName.append(advance());
            }
            parentClassName = parentName.toString();
            if (parentClassName.isEmpty()) {
                throw new RuntimeException("Expected parent class name after 'extends'");
            }
            classParentNames.put(className, parentClassName);
            skipWhitespace();
        }

        expect("{");

        Map<String, PhpClass.PropertyMetadata> properties = new HashMap<>();
        Map<String, PhpClass.MethodMetadata> methods = new HashMap<>();
        CallTarget constructor = null;

        skipWhitespace();
        while (!check("}") && !isAtEnd()) {
            // Check for visibility modifiers
            Visibility visibility = Visibility.PUBLIC; // Default visibility is public
            boolean isStatic = false;

            if (matchKeyword("public")) {
                visibility = Visibility.PUBLIC;
                skipWhitespace();
            } else if (matchKeyword("protected")) {
                visibility = Visibility.PROTECTED;
                skipWhitespace();
            } else if (matchKeyword("private")) {
                visibility = Visibility.PRIVATE;
                skipWhitespace();
            }

            // Check for static modifier
            if (matchKeyword("static")) {
                isStatic = true;
                skipWhitespace();
            }

            // Check if it's a property or method
            if (matchKeyword("function")) {
                // Parse method
                skipWhitespace();

                // Parse method name
                StringBuilder methodNameBuilder = new StringBuilder();
                while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                    methodNameBuilder.append(advance());
                }
                String methodName = methodNameBuilder.toString();

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

                // Create a new frame for the method
                FrameDescriptor.Builder methodFrameBuilder = FrameDescriptor.newBuilder();

                int thisSlot = -1;
                // Reserve slot for $this only for non-static methods
                if (!isStatic) {
                    thisSlot = methodFrameBuilder.addSlot(FrameSlotKind.Illegal, "this", null);
                }

                // Add parameter slots
                int[] paramSlots = new int[paramNames.size()];
                for (int i = 0; i < paramNames.size(); i++) {
                    int slot = methodFrameBuilder.addSlot(FrameSlotKind.Illegal, paramNames.get(i), null);
                    paramSlots[i] = slot;
                }

                // Save current parser state
                Map<String, Integer> savedVars = new HashMap<>(this.variables);
                FrameDescriptor.Builder savedFrameBuilder = this.currentFrameBuilder;
                String savedClassName = this.currentClassName;

                // Clear and set up method's variable scope
                this.variables.clear();
                this.currentFrameBuilder = methodFrameBuilder;
                this.currentClassName = className;  // Track class context for visibility checking

                // Add $this to variable map only for non-static methods
                if (!isStatic) {
                    this.variables.put("this", thisSlot);
                }

                // Add parameters to method's variable map
                for (int i = 0; i < paramNames.size(); i++) {
                    this.variables.put(paramNames.get(i), paramSlots[i]);
                }

                skipWhitespace();
                PhpStatementNode body = parseBlock();

                // Restore parser state
                this.variables.clear();
                this.variables.putAll(savedVars);
                this.currentFrameBuilder = savedFrameBuilder;
                this.currentClassName = savedClassName;

                // Create method root node (use FunctionRootNode for static methods)
                CallTarget methodCallTarget;
                if (isStatic) {
                    PhpFunctionRootNode functionRoot = new PhpFunctionRootNode(
                        language,
                        methodFrameBuilder.build(),
                        className + "::" + methodName,
                        paramNames.toArray(new String[0]),
                        paramSlots,
                        body
                    );
                    methodCallTarget = functionRoot.getCallTarget();
                } else {
                    PhpMethodRootNode methodRoot = new PhpMethodRootNode(
                        language,
                        methodFrameBuilder.build(),
                        className,
                        methodName,
                        paramNames.toArray(new String[0]),
                        paramSlots,
                        thisSlot,
                        body
                    );
                    methodCallTarget = methodRoot.getCallTarget();
                }

                // Check if it's a constructor
                if (methodName.equals("__construct")) {
                    constructor = methodCallTarget;
                } else {
                    methods.put(methodName, new PhpClass.MethodMetadata(
                        methodName,
                        visibility,
                        isStatic,
                        methodCallTarget,
                        paramNames.toArray(new String[0])
                    ));
                }

            } else if (peek() == '$') {
                // Parse property
                String propName = parseVariableName();
                skipWhitespace();

                // Parse default value if present
                Object defaultValue = null;
                if (match("=")) {
                    skipWhitespace();
                    // For now, only support literal default values
                    if (peek() == '"' || peek() == '\'') {
                        defaultValue = parseString();
                    } else if (Character.isDigit(peek()) || peek() == '-') {
                        if (peek() == '-') {
                            advance(); // consume '-'
                            skipWhitespace();
                            defaultValue = -Long.parseLong(parseNumberString());
                        } else {
                            String numStr = parseNumberString();
                            if (numStr.contains(".")) {
                                defaultValue = Double.parseDouble(numStr);
                            } else {
                                defaultValue = Long.parseLong(numStr);
                            }
                        }
                    } else if (match("true")) {
                        defaultValue = true;
                    } else if (match("false")) {
                        defaultValue = false;
                    } else if (match("null")) {
                        defaultValue = null;
                    }
                }

                expect(";");

                properties.put(propName, new PhpClass.PropertyMetadata(propName, visibility, isStatic, defaultValue));

            } else {
                throw new RuntimeException("Unexpected token in class body at position " + pos);
            }

            skipWhitespace();
        }

        expect("}");

        // Create PhpClass
        PhpClass phpClass = new PhpClass(className, properties, methods, constructor);
        declaredClasses.add(phpClass);

        return new PhpClassNode(phpClass);
    }

    private String parseNumberString() {
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

    private PhpStatementNode parseReturn() {
        skipWhitespace();
        PhpExpressionNode value = null;

        if (!check(";")) {
            value = parseExpression();
        }

        expect(";");
        return new PhpReturnNode(value);
    }

    private PhpStatementNode parseThrow() {
        skipWhitespace();
        PhpExpressionNode exception = parseExpression();
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
        PhpExpressionNode switchExpr = parseExpression();
        expect(")");
        skipWhitespace();
        expect("{");

        List<dev.truffle.php.nodes.statement.PhpSwitchNode.CaseClause> cases = new ArrayList<>();
        int defaultCaseIndex = -1;

        skipWhitespace();
        while (!check("}") && !isAtEnd()) {
            if (matchKeyword("case")) {
                skipWhitespace();
                PhpExpressionNode caseValue = parseExpression();
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

                cases.add(new dev.truffle.php.nodes.statement.PhpSwitchNode.CaseClause(
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
                cases.add(new dev.truffle.php.nodes.statement.PhpSwitchNode.CaseClause(
                    null,
                    defaultStatements.toArray(new PhpStatementNode[0]),
                    true
                ));
            } else {
                throw new RuntimeException("Expected 'case' or 'default' in switch statement at position " + pos);
            }
            skipWhitespace();
        }

        expect("}");

        return new dev.truffle.php.nodes.statement.PhpSwitchNode(
            switchExpr,
            cases.toArray(new dev.truffle.php.nodes.statement.PhpSwitchNode.CaseClause[0]),
            defaultCaseIndex
        );
    }

    private PhpStatementNode parseStaticPropertyAssignment() {
        // Parse ClassName::$property = value;
        StringBuilder className = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            className.append(advance());
        }

        skipWhitespace();
        expect("::");
        skipWhitespace();

        String propertyName = parseVariableName();
        skipWhitespace();
        expect("=");
        skipWhitespace();

        PhpExpressionNode value = parseExpression();
        expect(";");

        PhpExpressionNode writeExpr = new PhpStaticPropertyWriteNode(className.toString(), propertyName, value);
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
                    indexNode = parseExpression();
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
                PhpExpressionNode value = parseExpression();
                expect(";");

                // Build the array access first
                PhpExpressionNode arrayAccess = varNode;
                for (PhpExpressionNode index : indices) {
                    arrayAccess = new PhpArrayAccessNode(arrayAccess, index);
                }

                // Then create property write on the result
                PhpExpressionNode writeExpr = new PhpPropertyWriteNode(arrayAccess, propertyName, value, currentClassName);
                return new PhpExpressionStatementNode(writeExpr);
            }

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
                PhpExpressionNode rightValue = parseExpression();
                expect(";");

                // Read current value, apply operation, write back
                PhpExpressionNode readExpr = new PhpPropertyAccessNode(varNode, propertyName, currentClassName);
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
                PhpExpressionNode writeExpr = new PhpPropertyWriteNode(varNode, propertyName, newValue, currentClassName);
                return new PhpExpressionStatementNode(writeExpr);
            }

            // Regular property assignment
            expect("=");
            skipWhitespace();
            PhpExpressionNode value = parseExpression();
            expect(";");

            PhpExpressionNode writeExpr = new PhpPropertyWriteNode(varNode, propertyName, value, currentClassName);
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
            PhpExpressionNode rightValue = parseExpression();
            expect(";");

            PhpExpressionNode compoundExpr = new PhpCompoundAssignmentNode(varName, slot, compoundOp, rightValue);
            return new PhpExpressionStatementNode(compoundExpr);
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
        return parseAssignmentExpression();
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
                pos += 2; // consume ??
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

                    varNode = new PhpMethodCallNode(varNode, member, args.toArray(new PhpExpressionNode[0]), currentClassName);
                } else {
                    // Property access
                    varNode = new PhpPropertyAccessNode(varNode, member, currentClassName);
                }

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

        throw new RuntimeException("Unexpected character at position " + pos + ": " + peek());
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

        // Check for :: (static member access)
        if (match("::")) {
            skipWhitespace();

            // Check if it's a static property ($) or static method
            if (peek() == '$') {
                // Static property access: ClassName::$property
                String propName = parseVariableName();
                return new PhpStaticPropertyAccessNode(identifier, propName);
            } else {
                // Static method call: ClassName::method()
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

    /**
     * Match a keyword, ensuring it's followed by a word boundary.
     * Use this for matching PHP keywords to avoid matching prefixes of identifiers.
     */
    private boolean matchKeyword(String keyword) {
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
