package dev.truffle.php.parser;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpMethodRootNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.nodes.statement.PhpBlockNode;
import dev.truffle.php.nodes.statement.PhpTraitNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpGlobalScope;
import dev.truffle.php.runtime.PhpNamespaceContext;
import dev.truffle.php.runtime.PhpTrait;
import dev.truffle.php.runtime.Visibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for PHP trait declarations.
 * Handles trait definitions including methods, properties, and nested trait usage.
 */
public final class PhpTraitParser {

    private final PhpLanguage language;
    private final PhpLexer lexer;
    private final Map<String, Integer> variables;
    private final PhpGlobalScope globalScope;
    private final PhpExpressionParser expressionParser;
    private final PhpStatementParser.StatementContext statementContext;
    private final TraitParserContext context;
    private final BlockParserDelegate blockDelegate;
    private PhpNamespaceContext namespaceContext;

    /**
     * Context shared between trait parser and main parser.
     */
    public static final class TraitParserContext {
        public final List<PhpTrait> declaredTraits = new ArrayList<>();
        public final Map<String, List<String>> traitUsedTraits = new HashMap<>();  // traitName -> List<usedTraitNames>
        public final Map<PhpTrait, String> traitNamespaces = new HashMap<>();  // Track namespace for each trait

        public String currentTraitName;
        public FrameDescriptor.Builder currentFrameBuilder;
    }

    /**
     * Delegate interface for parsing blocks (to avoid circular dependency).
     */
    public interface BlockParserDelegate {
        PhpStatementNode parseBlock();
    }

    public PhpTraitParser(
            PhpLanguage language,
            PhpLexer lexer,
            Map<String, Integer> variables,
            PhpGlobalScope globalScope,
            PhpExpressionParser expressionParser,
            PhpStatementParser.StatementContext statementContext,
            TraitParserContext context,
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

    public PhpStatementNode parseTrait() {
        skipWhitespace();

        // Parse trait name
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        String traitName = name.toString();
        if (traitName.isEmpty()) {
            throw new RuntimeException("Expected trait name");
        }

        skipWhitespace();
        expect("{");

        Map<String, PhpClass.PropertyMetadata> properties = new HashMap<>();
        Map<String, PhpClass.MethodMetadata> methods = new HashMap<>();
        List<String> usedTraitNames = new ArrayList<>();

        skipWhitespace();
        while (!check("}") && !isAtEnd()) {
            // Check for 'use' keyword (trait using another trait)
            if (matchKeyword("use")) {
                skipWhitespace();

                // Parse list of traits (can use multiple traits)
                do {
                    StringBuilder usedTraitName = new StringBuilder();
                    while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
                        usedTraitName.append(advance());
                    }
                    String traitToUse = usedTraitName.toString();
                    if (traitToUse.isEmpty()) {
                        throw new RuntimeException("Expected trait name after 'use'");
                    }
                    usedTraitNames.add(traitToUse);
                    skipWhitespace();

                    if (match(",")) {
                        skipWhitespace();
                    } else {
                        break;
                    }
                } while (true);

                expect(";");
                skipWhitespace();
                continue;
            }

            // Check for abstract modifier first
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
                if (methodName.isEmpty()) {
                    throw new RuntimeException("Expected method name");
                }

                skipWhitespace();
                expect("(");
                skipWhitespace();

                // Parse parameters
                List<String> paramNames = new ArrayList<>();
                int variadicParamIndex = -1;

                while (!check(")")) {
                    // Check for variadic parameter (...)
                    boolean isVariadic = false;
                    if (match("...")) {
                        isVariadic = true;
                        skipWhitespace();
                    }

                    // Check for optional type hint
                    parseOptionalTypeHint();

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
                if (match(":")) {
                    skipWhitespace();
                    parseTypeHint();
                }

                skipWhitespace();

                // Handle abstract methods
                if (isMethodAbstract) {
                    if (visibility == Visibility.PRIVATE) {
                        throw new RuntimeException("Abstract method " + methodName + " cannot be private");
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
                    continue;
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
                String savedTraitName = this.context.currentTraitName;

                // Clear and set up method's variable scope
                this.variables.clear();
                this.context.currentFrameBuilder = methodFrameBuilder;
                this.context.currentTraitName = traitName;
                this.expressionParser.updateContext(methodFrameBuilder, traitName);
                this.expressionParser.updateFunctionName(methodName);
                this.statementContext.currentFrameBuilder = methodFrameBuilder;
                this.statementContext.currentClassName = traitName;  // Traits can use $this

                // Add $this to variable map only for non-static methods
                if (!isStatic) {
                    this.variables.put("this", thisSlot);
                }

                // Add parameters to method's variable map
                for (int i = 0; i < paramNames.size(); i++) {
                    this.variables.put(paramNames.get(i), paramSlots[i]);
                }

                skipWhitespace();
                PhpStatementNode body = blockDelegate.parseBlock();

                // Restore parser state
                this.variables.clear();
                this.variables.putAll(savedVars);
                this.context.currentFrameBuilder = savedFrameBuilder;
                this.context.currentTraitName = savedTraitName;
                this.expressionParser.updateContext(savedFrameBuilder, savedTraitName);
                this.expressionParser.updateFunctionName("");
                this.statementContext.currentFrameBuilder = savedFrameBuilder;
                this.statementContext.currentClassName = savedTraitName;

                // Create method call target
                // For traits, we always use PhpMethodRootNode (even for static) since traits are composed into classes
                PhpMethodRootNode methodRoot = new PhpMethodRootNode(
                    language,
                    methodFrameBuilder.build(),
                    traitName,
                    methodName,
                    paramNames.toArray(new String[0]),
                    paramSlots,
                    thisSlot,
                    body,
                    variadicParamIndex
                );
                CallTarget methodCallTarget = methodRoot.getCallTarget();

                methods.put(methodName, new PhpClass.MethodMetadata(
                    methodName,
                    visibility,
                    isStatic,
                    false,  // isAbstract = false for concrete methods
                    methodCallTarget,
                    paramNames.toArray(new String[0])
                ));

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
                throw new RuntimeException("Unexpected token in trait body at position " + lexer.getPosition());
            }

            skipWhitespace();
        }

        expect("}");

        // Create PhpTrait
        PhpTrait phpTrait = new PhpTrait(traitName, methods, properties);
        context.declaredTraits.add(phpTrait);

        // Track which namespace this trait was declared in
        if (namespaceContext != null) {
            context.traitNamespaces.put(phpTrait, namespaceContext.getCurrentNamespace());
        }

        // Store used traits for later resolution
        if (!usedTraitNames.isEmpty()) {
            context.traitUsedTraits.put(traitName, usedTraitNames);
        }

        return new PhpTraitNode(phpTrait);
    }

    private String parseVariableName() {
        expect("$");
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        if (name.length() == 0) {
            throw new RuntimeException("Expected variable name");
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
     * Currently just skips type hints for traits.
     */
    private void parseOptionalTypeHint() {
        skipWhitespace();

        // Check for nullable type (?)
        if (match("?")) {
            skipWhitespace();
        }

        // Check if next token looks like a type (not a variable)
        if (peek() == '$') {
            return; // No type hint
        }

        // Check if it's a known type or identifier
        if (Character.isLetter(peek()) || peek() == '_') {
            int savedPos = lexer.getPosition();
            while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_' || peek() == '\\')) {
                advance();
            }
            skipWhitespace();

            // If not followed by $, restore position
            if (peek() != '$') {
                lexer.setPosition(savedPos);
            }
        }
    }

    /**
     * Parse required type hint (for return types).
     * Currently just skips type hints for traits.
     */
    private void parseTypeHint() {
        skipWhitespace();

        // Check for nullable type (?)
        if (match("?")) {
            skipWhitespace();
        }

        // Parse type name
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_' || peek() == '\\')) {
            advance();
        }
    }
}
