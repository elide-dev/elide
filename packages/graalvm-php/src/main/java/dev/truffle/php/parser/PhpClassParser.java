package dev.truffle.php.parser;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpFunctionRootNode;
import dev.truffle.php.nodes.PhpMethodRootNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.nodes.statement.PhpBlockNode;
import dev.truffle.php.nodes.statement.PhpClassNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpGlobalScope;
import dev.truffle.php.runtime.PhpInterface;
import dev.truffle.php.runtime.PhpNamespaceContext;
import dev.truffle.php.runtime.Visibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for PHP class and interface declarations.
 * Handles class definitions, inheritance, interface implementation, and member parsing.
 */
public final class PhpClassParser {

    private final PhpLanguage language;
    private final PhpLexer lexer;
    private final Map<String, Integer> variables;
    private final PhpGlobalScope globalScope;
    private final PhpExpressionParser expressionParser;
    private final PhpStatementParser.StatementContext statementContext;
    private final ClassParserContext context;
    private final BlockParserDelegate blockDelegate;
    private PhpNamespaceContext namespaceContext;

    /**
     * Context shared between class parser and main parser.
     */
    public static final class ClassParserContext {
        public final List<PhpClass> declaredClasses = new ArrayList<>();
        public final List<PhpInterface> declaredInterfaces = new ArrayList<>();
        public final Map<String, String> classParentNames = new HashMap<>();  // className -> parentClassName
        public final Map<String, List<String>> classImplementedInterfaces = new HashMap<>();  // className -> List<interfaceName>
        public final Map<String, String> interfaceParentNames = new HashMap<>();  // interfaceName -> parentInterfaceName
        public final Map<PhpClass, String> classNamespaces = new HashMap<>();  // Track namespace for each class
        public final Map<PhpInterface, String> interfaceNamespaces = new HashMap<>();  // Track namespace for each interface

        public String currentClassName;
        public FrameDescriptor.Builder currentFrameBuilder;
    }

    /**
     * Delegate interface for parsing blocks (to avoid circular dependency).
     */
    public interface BlockParserDelegate {
        PhpStatementNode parseBlock();
    }

    public PhpClassParser(
            PhpLanguage language,
            PhpLexer lexer,
            Map<String, Integer> variables,
            PhpGlobalScope globalScope,
            PhpExpressionParser expressionParser,
            PhpStatementParser.StatementContext statementContext,
            ClassParserContext context,
            BlockParserDelegate blockDelegate) {
        this.language = language;
        this.lexer = lexer;
        this.variables = variables;
        this.globalScope = globalScope;
        this.expressionParser = expressionParser;
        this.statementContext = statementContext;
        this.context = context;
        this.blockDelegate = blockDelegate;
        this.namespaceContext = null;
    }

    /**
     * Set the namespace context for parsing.
     */
    public void setNamespaceContext(PhpNamespaceContext namespaceContext) {
        this.namespaceContext = namespaceContext;
    }

    public PhpStatementNode parseClass(boolean isAbstract) {
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
            context.classParentNames.put(className, parentClassName);
            skipWhitespace();
        }

        // Check for implements keyword (can implement multiple interfaces)
        List<String> implementedInterfaceNames = new ArrayList<>();
        if (matchKeyword("implements")) {
            skipWhitespace();
            do {
                StringBuilder interfaceName = new StringBuilder();
                while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                    interfaceName.append(advance());
                }
                String ifaceName = interfaceName.toString();
                if (ifaceName.isEmpty()) {
                    throw new RuntimeException("Expected interface name after 'implements'");
                }
                implementedInterfaceNames.add(ifaceName);
                skipWhitespace();
                if (match(",")) {
                    skipWhitespace();  // Skip whitespace after comma
                } else {
                    break;  // No more interfaces
                }
            } while (true);
            skipWhitespace();
        }

        expect("{");

        Map<String, PhpClass.PropertyMetadata> properties = new HashMap<>();
        Map<String, PhpClass.MethodMetadata> methods = new HashMap<>();
        CallTarget constructor = null;

        skipWhitespace();
        while (!check("}") && !isAtEnd()) {
            // Check for abstract modifier first (can come before visibility)
            boolean isMethodAbstract = false;
            if (matchKeyword("abstract")) {
                isMethodAbstract = true;
                skipWhitespace();
            }

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

                // Parse parameters with optional type hints
                List<String> paramNames = new ArrayList<>();
                List<dev.truffle.php.runtime.PhpTypeHint> paramTypes = new ArrayList<>();
                int variadicParamIndex = -1;  // Index of variadic parameter, -1 if none

                while (!check(")")) {
                    // Check for variadic parameter (...)
                    boolean isVariadic = false;
                    if (match("...")) {
                        isVariadic = true;
                        skipWhitespace();
                    }

                    // Check for optional type hint
                    dev.truffle.php.runtime.PhpTypeHint typeHint = parseOptionalTypeHint();
                    paramTypes.add(typeHint);

                    // Parse parameter name
                    String paramName = parseVariableName();
                    paramNames.add(paramName);

                    if (isVariadic) {
                        if (variadicParamIndex != -1) {
                            throw new RuntimeException("Only one variadic parameter is allowed");
                        }
                        variadicParamIndex = paramNames.size() - 1;
                    }

                    skipWhitespace();

                    if (match(",")) {
                        if (isVariadic) {
                            throw new RuntimeException("Variadic parameter must be the last parameter");
                        }
                        skipWhitespace();
                    }
                }
                expect(")");

                // Parse optional return type hint
                skipWhitespace();
                dev.truffle.php.runtime.PhpTypeHint returnType = null;
                if (match(":")) {
                    skipWhitespace();
                    returnType = parseTypeHint();
                }

                skipWhitespace();

                // Handle abstract methods
                if (isMethodAbstract) {
                    // Validation for abstract methods
                    if (!isAbstract) {
                        throw new RuntimeException("Abstract method " + methodName + " cannot be declared in non-abstract class " + className);
                    }
                    if (visibility == Visibility.PRIVATE) {
                        throw new RuntimeException("Abstract method " + methodName + " cannot be private");
                    }
                    if (methodName.equals("__construct")) {
                        throw new RuntimeException("Constructor cannot be abstract");
                    }

                    // Abstract methods end with semicolon, no body
                    expect(";");

                    // Store abstract method with null CallTarget
                    methods.put(methodName, new PhpClass.MethodMetadata(
                        methodName,
                        visibility,
                        isStatic,
                        true,  // isAbstract = true
                        null,  // No CallTarget for abstract methods
                        paramNames.toArray(new String[0])
                    ));

                    skipWhitespace();
                    continue;  // Skip to next class member
                }

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
                FrameDescriptor.Builder savedFrameBuilder = this.context.currentFrameBuilder;
                String savedClassName = this.context.currentClassName;

                // Clear and set up method's variable scope
                this.variables.clear();
                this.context.currentFrameBuilder = methodFrameBuilder;
                this.context.currentClassName = className;  // Track class context for visibility checking
                this.expressionParser.updateContext(methodFrameBuilder, className);
                this.expressionParser.updateFunctionName(methodName);  // Track method name for __METHOD__
                this.statementContext.currentFrameBuilder = methodFrameBuilder;
                this.statementContext.currentClassName = className;

                // Add $this to variable map only for non-static methods
                if (!isStatic) {
                    this.variables.put("this", thisSlot);
                }

                // Add parameters to method's variable map
                for (int i = 0; i < paramNames.size(); i++) {
                    this.variables.put(paramNames.get(i), paramSlots[i]);
                }

                // Add parameter type checks at the beginning of the method body
                List<PhpStatementNode> bodyStatements = new ArrayList<>();

                // Insert parameter type checks
                for (int i = 0; i < paramNames.size(); i++) {
                    if (paramTypes.get(i) != null) {
                        bodyStatements.add(new dev.truffle.php.nodes.PhpParameterTypeCheckNode(
                            paramNames.get(i),
                            paramSlots[i],
                            paramTypes.get(i),
                            className  // Pass class context for self/parent types
                        ));
                    }
                }

                skipWhitespace();
                PhpStatementNode body = blockDelegate.parseBlock();

                // If we have type checks, wrap the body
                if (!bodyStatements.isEmpty()) {
                    bodyStatements.add(body);
                    body = new PhpBlockNode(
                        bodyStatements.toArray(new PhpStatementNode[0])
                    );
                }

                // Restore parser state
                this.variables.clear();
                this.variables.putAll(savedVars);
                this.context.currentFrameBuilder = savedFrameBuilder;
                this.context.currentClassName = savedClassName;
                this.expressionParser.updateContext(savedFrameBuilder, savedClassName);
                this.expressionParser.updateFunctionName("");  // Clear method name
                this.statementContext.currentFrameBuilder = savedFrameBuilder;
                this.statementContext.currentClassName = savedClassName;

                // Create method root node (use FunctionRootNode for static methods)
                CallTarget methodCallTarget;
                if (isStatic) {
                    PhpFunctionRootNode functionRoot = new PhpFunctionRootNode(
                        language,
                        methodFrameBuilder.build(),
                        className + "::" + methodName,
                        paramNames.toArray(new String[0]),
                        paramSlots,
                        body,
                        variadicParamIndex
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
                        body,
                        variadicParamIndex
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
                        false,  // isAbstract = false for concrete methods
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
                throw new RuntimeException("Unexpected token in class body at position " + lexer.getPosition());
            }

            skipWhitespace();
        }

        expect("}");

        // Create PhpClass
        PhpClass phpClass = new PhpClass(className, properties, methods, constructor, isAbstract);
        context.declaredClasses.add(phpClass);

        // Track which namespace this class was declared in
        if (namespaceContext != null) {
            context.classNamespaces.put(phpClass, namespaceContext.getCurrentNamespace());
        }

        // Store implemented interfaces for later resolution
        if (!implementedInterfaceNames.isEmpty()) {
            context.classImplementedInterfaces.put(className, implementedInterfaceNames);
        }

        return new PhpClassNode(phpClass);
    }

    public PhpStatementNode parseInterface() {
        skipWhitespace();

        // Parse interface name
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        String interfaceName = name.toString();

        skipWhitespace();

        // Check for extends keyword (interfaces can extend other interfaces)
        String parentInterfaceName = null;
        if (matchKeyword("extends")) {
            skipWhitespace();
            StringBuilder parentName = new StringBuilder();
            while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                parentName.append(advance());
            }
            parentInterfaceName = parentName.toString();
            if (parentInterfaceName.isEmpty()) {
                throw new RuntimeException("Expected parent interface name after 'extends'");
            }
            context.interfaceParentNames.put(interfaceName, parentInterfaceName);
            skipWhitespace();
        }

        expect("{");

        Map<String, PhpInterface.MethodSignature> methods = new HashMap<>();

        skipWhitespace();
        while (!check("}") && !isAtEnd()) {
            // Interfaces can only have method signatures (no implementations)
            // All interface methods are implicitly public

            // Check if it's a method signature
            if (matchKeyword("function")) {
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

                // Parse parameters with optional type hints
                List<String> paramNames = new ArrayList<>();
                while (!check(")")) {
                    // Check for optional type hint (for signature only, not enforced)
                    parseOptionalTypeHint();  // Parse but don't store for interfaces

                    String paramName = parseVariableName();
                    paramNames.add(paramName);
                    skipWhitespace();
                    if (match(",")) {
                        skipWhitespace();
                    }
                }
                expect(")");

                // Parse optional return type hint (for signature only)
                skipWhitespace();
                if (match(":")) {
                    skipWhitespace();
                    parseTypeHint();  // Parse but don't store for interfaces
                }

                skipWhitespace();
                expect(";"); // Interface methods end with semicolon, no body

                methods.put(methodName, new PhpInterface.MethodSignature(
                    methodName,
                    paramNames.toArray(new String[0])
                ));

            } else {
                throw new RuntimeException("Unexpected token in interface body at position " + lexer.getPosition() + ". Interfaces can only contain method signatures.");
            }

            skipWhitespace();
        }

        expect("}");

        // Create PhpInterface
        PhpInterface phpInterface = new PhpInterface(interfaceName, methods);
        context.declaredInterfaces.add(phpInterface);

        // Track which namespace this interface was declared in
        if (namespaceContext != null) {
            context.interfaceNamespaces.put(phpInterface, namespaceContext.getCurrentNamespace());
        }

        // For now, return a simple block node (interfaces don't execute code)
        return new PhpBlockNode(new PhpStatementNode[0]);
    }

    private String parseVariableName() {
        expect("$");
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        return name.toString();
    }

    private String parseNumberString() {
        return lexer.parseNumberString();
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

    // Lexer delegation methods
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

    private char advance() {
        return lexer.advance();
    }

    private boolean isAtEnd() {
        return lexer.isAtEnd();
    }

    /**
     * Parse optional type hint (for parameters).
     * Returns null if no type hint is present.
     */
    private dev.truffle.php.runtime.PhpTypeHint parseOptionalTypeHint() {
        skipWhitespace();

        // Check for nullable type (?)
        boolean nullable = false;
        if (match("?")) {
            nullable = true;
            skipWhitespace();
        }

        // Check if next token looks like a type (not a variable)
        if (peek() == '$') {
            // No type hint, just a parameter
            return null;
        }

        // Check if it's a known type or identifier
        if (Character.isLetter(peek()) || peek() == '_') {
            // Try to parse type name
            int savedPos = lexer.getPosition();
            StringBuilder typeName = new StringBuilder();
            while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_' || peek() == '\\')) {
                typeName.append(advance());
            }
            String type = typeName.toString();

            skipWhitespace();

            // If followed by $, this is a type hint
            if (peek() == '$') {
                return new dev.truffle.php.runtime.PhpTypeHint(type, nullable);
            } else {
                // Not a type hint, restore position
                lexer.setPosition(savedPos);
                return null;
            }
        }

        return null;
    }

    /**
     * Parse required type hint (for return types).
     */
    private dev.truffle.php.runtime.PhpTypeHint parseTypeHint() {
        skipWhitespace();

        // Check for nullable type (?)
        boolean nullable = false;
        if (match("?")) {
            nullable = true;
            skipWhitespace();
        }

        // Parse type name
        StringBuilder typeName = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_' || peek() == '\\')) {
            typeName.append(advance());
        }

        String type = typeName.toString();
        if (type.isEmpty()) {
            throw new RuntimeException("Expected type name");
        }

        return new dev.truffle.php.runtime.PhpTypeHint(type, nullable);
    }
}
