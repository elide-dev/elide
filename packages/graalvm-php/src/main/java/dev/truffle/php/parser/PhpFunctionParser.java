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
package dev.truffle.php.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpFunctionRootNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.nodes.statement.PhpFunctionNode;
import dev.truffle.php.runtime.PhpFunction;
import dev.truffle.php.runtime.PhpGlobalScope;
import dev.truffle.php.runtime.PhpNamespaceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for PHP function declarations. Handles function definitions, parameter parsing, and scope
 * management.
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
  private PhpNamespaceContext namespaceContext;

  /** Context shared between function parser and main parser. */
  public static final class FunctionParserContext {
    public final List<PhpFunction> declaredFunctions = new ArrayList<>();
    public final Map<PhpFunction, String> functionNamespaces =
        new HashMap<>(); // Track namespace for each function
    public FrameDescriptor.Builder currentFrameBuilder;
  }

  /** Delegate interface for parsing blocks (to avoid circular dependency). */
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
    this.namespaceContext = null;
  }

  /** Set the namespace context for parsing. */
  public void setNamespaceContext(PhpNamespaceContext namespaceContext) {
    this.namespaceContext = namespaceContext;
  }

  public PhpStatementNode parseFunction() {
    skipWhitespace();

    // Check for return-by-reference indicator (&)
    // Syntax: function &functionName()
    boolean returnsByReference = false;
    if (match("&")) {
      returnsByReference = true;
      skipWhitespace();
    }

    // Parse function name
    StringBuilder name = new StringBuilder();
    while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
      name.append(advance());
    }
    String functionName = name.toString();

    skipWhitespace();
    expect("(");
    skipWhitespace();

    // Parse parameters with optional type hints
    List<String> paramNames = new ArrayList<>();
    List<dev.truffle.php.runtime.PhpTypeHint> paramTypes = new ArrayList<>();
    List<Boolean> referenceParams = new ArrayList<>(); // Track which parameters are by-reference
    int variadicParamIndex = -1; // Index of variadic parameter, -1 if none

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

      // Check for reference parameter (&$param)
      boolean isReference = false;
      if (match("&")) {
        isReference = true;
        skipWhitespace();
      }
      referenceParams.add(isReference);

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
    this.expressionParser.updateFunctionName(functionName); // Track function name for __FUNCTION__
    this.statementContext.currentFrameBuilder = functionFrameBuilder;
    this.statementContext.currentClassName = null;

    // Add parameters to function's variable map
    for (int i = 0; i < paramNames.size(); i++) {
      this.variables.put(paramNames.get(i), paramSlots[i]);
    }

    // Add parameter type checks at the beginning of the function body
    List<PhpStatementNode> bodyStatements = new ArrayList<>();

    // Insert parameter type checks
    for (int i = 0; i < paramNames.size(); i++) {
      if (paramTypes.get(i) != null) {
        bodyStatements.add(
            new dev.truffle.php.nodes.PhpParameterTypeCheckNode(
                paramNames.get(i),
                paramSlots[i],
                paramTypes.get(i),
                null // No class context for standalone functions
                ));
      }
    }

    skipWhitespace();
    PhpStatementNode body = blockDelegate.parseBlock();

    // If we have type checks, wrap the body
    if (!bodyStatements.isEmpty()) {
      bodyStatements.add(body);
      body =
          new dev.truffle.php.nodes.statement.PhpBlockNode(
              bodyStatements.toArray(new PhpStatementNode[0]));
    }

    // Restore parser state
    this.variables.clear();
    this.variables.putAll(savedVars);
    this.context.currentFrameBuilder = savedFrameBuilder;
    this.expressionParser.updateContext(savedFrameBuilder, null);
    this.expressionParser.updateFunctionName(""); // Clear function name
    this.statementContext.currentFrameBuilder = savedFrameBuilder;
    this.statementContext.currentClassName = null;

    // Convert reference parameters list to array
    boolean[] referenceParamsArray = new boolean[referenceParams.size()];
    for (int i = 0; i < referenceParams.size(); i++) {
      referenceParamsArray[i] = referenceParams.get(i);
    }

    // Create function root node
    PhpFunctionRootNode functionRoot =
        new PhpFunctionRootNode(
            language,
            functionFrameBuilder.build(),
            functionName,
            paramNames.toArray(new String[0]),
            paramSlots,
            body,
            variadicParamIndex,
            referenceParamsArray,
            returnsByReference);

    // Create and register function
    PhpFunction function =
        new PhpFunction(
            functionName,
            functionRoot.getCallTarget(),
            paramNames.size(),
            paramNames.toArray(new String[0]),
            referenceParamsArray);
    context.declaredFunctions.add(function);

    // Track which namespace this function was declared in
    if (namespaceContext != null) {
      context.functionNamespaces.put(function, namespaceContext.getCurrentNamespace());
    }

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

  private boolean matchKeyword(String keyword) {
    return lexer.matchKeyword(keyword);
  }

  /** Parse optional type hint (for parameters). Returns null if no type hint is present. */
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

  /** Parse required type hint (for return types). */
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
