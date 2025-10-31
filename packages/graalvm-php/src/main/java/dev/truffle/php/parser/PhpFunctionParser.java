package dev.truffle.php.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpFunctionRootNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.nodes.statement.PhpFunctionNode;
import dev.truffle.php.runtime.PhpFunction;
import dev.truffle.php.runtime.PhpGlobalScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for PHP function declarations.
 * Handles function definitions, parameter parsing, and scope management.
 */
public final class PhpFunctionParser {

    private final PhpLanguage language;
    private final PhpLexer lexer;
    private final Map<String, Integer> variables;
    private final PhpGlobalScope globalScope;
    private final PhpExpressionParser expressionParser;
    private final PhpStatementParser.StatementContext statementContext;
    private final FunctionParserContext context;
    private final BlockParserDelegate blockDelegate;

    /**
     * Context shared between function parser and main parser.
     */
    public static final class FunctionParserContext {
        public final List<PhpFunction> declaredFunctions = new ArrayList<>();
        public FrameDescriptor.Builder currentFrameBuilder;
    }

    /**
     * Delegate interface for parsing blocks (to avoid circular dependency).
     */
    public interface BlockParserDelegate {
        PhpStatementNode parseBlock();
    }

    public PhpFunctionParser(
            PhpLanguage language,
            PhpLexer lexer,
            Map<String, Integer> variables,
            PhpGlobalScope globalScope,
            PhpExpressionParser expressionParser,
            PhpStatementParser.StatementContext statementContext,
            FunctionParserContext context,
            BlockParserDelegate blockDelegate) {
        this.language = language;
        this.lexer = lexer;
        this.variables = variables;
        this.globalScope = globalScope;
        this.expressionParser = expressionParser;
        this.statementContext = statementContext;
        this.context = context;
        this.blockDelegate = blockDelegate;
    }

    public PhpStatementNode parseFunction() {
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
        FrameDescriptor.Builder savedFrameBuilder = this.context.currentFrameBuilder;

        // Clear and set up function's variable scope
        this.variables.clear();
        this.context.currentFrameBuilder = functionFrameBuilder;
        this.expressionParser.updateContext(functionFrameBuilder, null);
        this.statementContext.currentFrameBuilder = functionFrameBuilder;
        this.statementContext.currentClassName = null;

        // Add parameters to function's variable map
        for (int i = 0; i < paramNames.size(); i++) {
            this.variables.put(paramNames.get(i), paramSlots[i]);
        }

        skipWhitespace();
        PhpStatementNode body = blockDelegate.parseBlock();

        // Restore parser state
        this.variables.clear();
        this.variables.putAll(savedVars);
        this.context.currentFrameBuilder = savedFrameBuilder;
        this.expressionParser.updateContext(savedFrameBuilder, null);
        this.statementContext.currentFrameBuilder = savedFrameBuilder;
        this.statementContext.currentClassName = null;

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
        context.declaredFunctions.add(function);

        return new PhpFunctionNode(functionName, function);
    }

    private String parseVariableName() {
        expect("$");
        StringBuilder name = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            name.append(advance());
        }
        return name.toString();
    }

    // Lexer delegation methods
    private void skipWhitespace() {
        lexer.skipWhitespace();
    }

    private boolean match(String expected) {
        return lexer.match(expected);
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
}
