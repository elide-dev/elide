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
import com.oracle.truffle.api.source.Source;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpRootNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.nodes.statement.PhpBlockNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpFunction;
import dev.truffle.php.runtime.PhpGlobalScope;
import dev.truffle.php.runtime.PhpInterface;
import dev.truffle.php.runtime.PhpTrait;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main coordinator for PHP parsing.
 *
 * <p>This class orchestrates the parsing process by delegating to specialized parsers: - PhpLexer:
 * Tokenization and lexical analysis - PhpExpressionParser: Expression parsing (literals, operators,
 * variables, function calls) - PhpStatementParser: Statement parsing (control flow, assignments,
 * blocks) - PhpClassParser: Class and interface declarations - PhpTraitParser: Trait declarations -
 * PhpFunctionParser: Function declarations
 *
 * <p>Responsibilities: - Initialize and coordinate all sub-parsers - Parse top-level PHP source
 * code into AST (Abstract Syntax Tree) - Resolve inheritance relationships (class/interface
 * hierarchies) - Register all declarations (functions, classes, interfaces) in the context -
 * Validate semantic constraints (circular inheritance, abstract methods)
 *
 * <p>The modular architecture allows each parser to focus on one responsibility while maintaining
 * clean separation of concerns through context objects and delegate patterns.
 */
public final class PhpParser {

  private final PhpLanguage language;
  private final Source source;
  private final PhpLexer lexer;
  private final Map<String, Integer> variables = new HashMap<>();
  private final PhpGlobalScope globalScope;
  private final PhpExpressionParser.ParserContext expressionContext;
  private final PhpExpressionParser expressionParser;
  private final PhpStatementParser.StatementContext statementContext;
  private final PhpStatementParser statementParser;
  private final PhpClassParser.ClassParserContext classContext;
  private final PhpClassParser classParser;
  private final PhpTraitParser.TraitParserContext traitContext;
  private final PhpTraitParser traitParser;
  private final PhpFunctionParser.FunctionParserContext functionContext;
  private final PhpFunctionParser functionParser;

  public PhpParser(PhpLanguage language, Source source, PhpGlobalScope globalScope) {
    this.language = language;
    this.source = source;
    this.lexer = new PhpLexer(source.getCharacters().toString());
    this.globalScope = globalScope;
    this.expressionContext = new PhpExpressionParser.ParserContext(null, null);
    this.statementContext = new PhpStatementParser.StatementContext(null, null);
    this.classContext = new PhpClassParser.ClassParserContext();
    this.traitContext = new PhpTraitParser.TraitParserContext();
    this.functionContext = new PhpFunctionParser.FunctionParserContext();

    // Create expression parser with block delegate
    this.expressionParser =
        new PhpExpressionParser(
            language,
            lexer,
            variables,
            globalScope,
            expressionContext,
            new PhpExpressionParser.BlockParserDelegate() {
              @Override
              public PhpStatementNode parseBlock() {
                return PhpParser.this.parseBlock();
              }
            });

    // Create class parser with block delegate
    this.classParser =
        new PhpClassParser(
            language,
            lexer,
            variables,
            globalScope,
            expressionParser,
            statementContext,
            classContext,
            new PhpClassParser.BlockParserDelegate() {
              @Override
              public PhpStatementNode parseBlock() {
                return PhpParser.this.parseBlock();
              }
            });

    // Create trait parser with block delegate
    this.traitParser =
        new PhpTraitParser(
            language,
            lexer,
            variables,
            globalScope,
            expressionParser,
            statementContext,
            traitContext,
            new PhpTraitParser.BlockParserDelegate() {
              @Override
              public PhpStatementNode parseBlock() {
                return PhpParser.this.parseBlock();
              }
            });

    // Create function parser with block delegate
    this.functionParser =
        new PhpFunctionParser(
            language,
            lexer,
            variables,
            globalScope,
            expressionParser,
            statementContext,
            functionContext,
            new PhpFunctionParser.BlockParserDelegate() {
              @Override
              public PhpStatementNode parseBlock() {
                return PhpParser.this.parseBlock();
              }
            });

    // Create statement parser with delegate for class/interface/trait/function parsing
    this.statementParser =
        new PhpStatementParser(
            lexer,
            variables,
            globalScope,
            expressionParser,
            statementContext,
            new PhpStatementParser.ParserDelegate() {
              @Override
              public PhpStatementNode parseClass(boolean isAbstract) {
                return classParser.parseClass(isAbstract);
              }

              @Override
              public PhpStatementNode parseInterface() {
                return classParser.parseInterface();
              }

              @Override
              public PhpStatementNode parseTrait() {
                return traitParser.parseTrait();
              }

              @Override
              public PhpStatementNode parseFunction() {
                return functionParser.parseFunction();
              }
            });
  }

  public PhpRootNode parse() {
    lexer.skipPhpOpenTag();
    List<PhpStatementNode> statements = new ArrayList<>();

    // Get context and provide namespace context to statement parser
    PhpRootNode tempRootNode =
        new PhpRootNode(
            language,
            FrameDescriptor.newBuilder().build(),
            new PhpBlockNode(new PhpStatementNode[0]));
    PhpContext context = PhpContext.get(tempRootNode);
    statementParser.setNamespaceContext(context.getNamespaceContext());
    functionParser.setNamespaceContext(context.getNamespaceContext());
    classParser.setNamespaceContext(context.getNamespaceContext());
    traitParser.setNamespaceContext(context.getNamespaceContext());

    // Set source path in expression context for magic constants
    // Use getPath() for real files, fall back to getName() for in-memory sources (tests)
    String sourcePath = source.getPath();
    if (sourcePath == null || sourcePath.isEmpty()) {
      sourcePath = source.getName();
    }
    expressionContext.sourcePath = sourcePath != null ? sourcePath : "";

    while (!lexer.isAtEnd()) {
      PhpStatementNode stmt = parseStatement();
      if (stmt != null) {
        statements.add(stmt);
      }
    }

    PhpStatementNode body = new PhpBlockNode(statements.toArray(new PhpStatementNode[0]));

    // Use the global scope's frame descriptor for top-level code
    // This allows variables to be shared across included files
    FrameDescriptor frameDescriptor = globalScope.build();
    PhpRootNode rootNode = new PhpRootNode(language, frameDescriptor, body);

    // Register traits first (must be done before classes, as classes may use traits)
    // First, resolve trait composition (nested traits)
    resolveTraitComposition(context);

    for (PhpTrait phpTrait : traitContext.declaredTraits) {
      // Get the namespace this trait was declared in
      String declaredNamespace = traitContext.traitNamespaces.get(phpTrait);
      String traitName = phpTrait.getName();

      if (declaredNamespace != null && !declaredNamespace.isEmpty() && !traitName.contains("\\")) {
        // Create a new PhpTrait with namespaced name
        PhpTrait namespacedTrait =
            new PhpTrait(
                declaredNamespace + "\\" + traitName,
                phpTrait.getMethods(),
                phpTrait.getProperties());
        // Copy used traits if any
        for (PhpTrait usedTrait : phpTrait.getUsedTraits()) {
          namespacedTrait.addUsedTrait(usedTrait);
        }
        context.registerTrait(namespacedTrait);
      } else {
        context.registerTrait(phpTrait);
      }
    }

    // Resolve parent class and interface references
    resolveInterfaceInheritance(context);
    resolveClassInheritance(context);

    // Register functions, classes, and interfaces in the context after parsing
    // Use the namespace each function was declared in

    for (PhpFunction function : functionContext.declaredFunctions) {
      // Get the namespace this function was declared in
      String declaredNamespace = functionContext.functionNamespaces.get(function);
      String functionName = function.getName();

      if (declaredNamespace != null
          && !declaredNamespace.isEmpty()
          && !functionName.contains("\\")) {
        // Create a new PhpFunction with namespaced name
        PhpFunction namespacedFunction =
            new PhpFunction(
                declaredNamespace + "\\" + functionName,
                function.getCallTarget(),
                function.getParameterCount(),
                function.getParameterNames());
        context.registerFunction(namespacedFunction);
      } else {
        context.registerFunction(function);
      }
    }

    for (PhpInterface phpInterface : classContext.declaredInterfaces) {
      // Get the namespace this interface was declared in
      String declaredNamespace = classContext.interfaceNamespaces.get(phpInterface);
      String interfaceName = phpInterface.getName();

      if (declaredNamespace != null
          && !declaredNamespace.isEmpty()
          && !interfaceName.contains("\\")) {
        // Create a new PhpInterface with namespaced name
        PhpInterface namespacedInterface =
            new PhpInterface(declaredNamespace + "\\" + interfaceName, phpInterface.getMethods());
        // Copy parent interfaces if any
        for (PhpInterface parent : phpInterface.getParentInterfaces()) {
          namespacedInterface.addParentInterface(parent);
        }
        context.registerInterface(namespacedInterface);
      } else {
        context.registerInterface(phpInterface);
      }
    }

    for (PhpClass phpClass : classContext.declaredClasses) {
      // Get the namespace this class was declared in
      String declaredNamespace = classContext.classNamespaces.get(phpClass);
      String className = phpClass.getName();

      if (declaredNamespace != null && !declaredNamespace.isEmpty() && !className.contains("\\")) {
        // Create a new PhpClass with namespaced name
        PhpClass namespacedClass =
            new PhpClass(
                declaredNamespace + "\\" + className,
                phpClass.getProperties(),
                phpClass.getMethods(),
                phpClass.getConstructor(),
                phpClass.isAbstract());
        // Copy parent class if any
        if (phpClass.getParentClass() != null) {
          namespacedClass.setParentClass(phpClass.getParentClass());
        }
        // Copy implemented interfaces if any
        for (PhpInterface iface : phpClass.getImplementedInterfaces()) {
          namespacedClass.addImplementedInterface(iface);
        }
        context.registerClass(namespacedClass);
      } else {
        context.registerClass(phpClass);
      }
    }

    return rootNode;
  }

  private void resolveClassInheritance(PhpContext context) {
    // Build a map of class names to PhpClass objects for quick lookup
    Map<String, PhpClass> classMap = new HashMap<>();
    for (PhpClass phpClass : classContext.declaredClasses) {
      classMap.put(phpClass.getName(), phpClass);
    }

    // Build a map of interface names for quick lookup
    Map<String, PhpInterface> interfaceMap = new HashMap<>();
    for (PhpInterface phpInterface : classContext.declaredInterfaces) {
      interfaceMap.put(phpInterface.getName(), phpInterface);
    }

    // Resolve parent class references
    for (PhpClass phpClass : classContext.declaredClasses) {
      String parentName = classContext.classParentNames.get(phpClass.getName());
      if (parentName != null) {
        // Find the parent class - check current file classes first, then built-in classes
        PhpClass parentClass = classMap.get(parentName);
        if (parentClass == null) {
          // Check for built-in classes in context
          parentClass = context.getClass(parentName);
        }
        if (parentClass == null) {
          throw new RuntimeException(
              "Parent class not found: " + parentName + " for class " + phpClass.getName());
        }

        // Check for circular inheritance
        if (hasCircularInheritance(phpClass.getName(), parentClass, classMap)) {
          throw new RuntimeException(
              "Circular inheritance detected for class: " + phpClass.getName());
        }

        // Set the parent class
        phpClass.setParentClass(parentClass);
      }

      // Resolve implemented interfaces
      List<String> interfaceNames = classContext.classImplementedInterfaces.get(phpClass.getName());
      if (interfaceNames != null) {
        for (String interfaceName : interfaceNames) {
          // Find the interface - check current file interfaces first, then context
          PhpInterface phpInterface = interfaceMap.get(interfaceName);
          if (phpInterface == null) {
            phpInterface = context.getInterface(interfaceName);
          }
          if (phpInterface == null) {
            throw new RuntimeException(
                "Interface not found: " + interfaceName + " for class " + phpClass.getName());
          }

          // Add the interface to the class
          phpClass.addImplementedInterface(phpInterface);
        }
      }

      // Resolve used traits
      List<String> traitNames = classContext.classUsedTraits.get(phpClass.getName());
      if (traitNames != null) {
        for (String traitName : traitNames) {
          // Find the trait - check context
          PhpTrait phpTrait = context.getTrait(traitName);
          if (phpTrait == null) {
            throw new RuntimeException(
                "Trait not found: " + traitName + " for class " + phpClass.getName());
          }

          // Add the trait to the class
          phpClass.addUsedTrait(phpTrait);
        }
      }
    }

    // Compose traits into classes after all traits are resolved
    for (PhpClass phpClass : classContext.declaredClasses) {
      // Get conflict resolutions for this class (if any)
      List<PhpClass.TraitConflictResolution> classResolutions = new ArrayList<>();
      List<PhpClassParser.TraitConflictResolution> parserResolutions =
          classContext.classTraitResolutions.get(phpClass.getName());

      // Convert parser resolutions to runtime resolutions
      if (parserResolutions != null) {
        for (PhpClassParser.TraitConflictResolution parserRes : parserResolutions) {
          classResolutions.add(
              new PhpClass.TraitConflictResolution(
                  parserRes.type == PhpClassParser.TraitConflictResolution.Type.INSTEADOF
                      ? PhpClass.TraitConflictResolution.Type.INSTEADOF
                      : PhpClass.TraitConflictResolution.Type.ALIAS,
                  parserRes.traitName,
                  parserRes.methodName,
                  parserRes.excludedTraits,
                  parserRes.aliasName,
                  parserRes.aliasVisibility));
        }
      }

      phpClass.composeTraits(classResolutions);
    }

    // Validate abstract method implementation after all inheritance and trait composition is
    // resolved
    for (PhpClass phpClass : classContext.declaredClasses) {
      phpClass.validateAbstractMethodsImplemented();
    }
  }

  private void resolveInterfaceInheritance(PhpContext context) {
    // Build a map of interface names to PhpInterface objects for quick lookup
    Map<String, PhpInterface> interfaceMap = new HashMap<>();
    for (PhpInterface phpInterface : classContext.declaredInterfaces) {
      interfaceMap.put(phpInterface.getName(), phpInterface);
    }

    // Resolve parent interface references
    for (PhpInterface phpInterface : classContext.declaredInterfaces) {
      String parentName = classContext.interfaceParentNames.get(phpInterface.getName());
      if (parentName != null) {
        // Find the parent interface - check current file interfaces first, then built-in interfaces
        // (if any)
        PhpInterface parentInterface = interfaceMap.get(parentName);
        if (parentInterface == null) {
          // Check for built-in interfaces in context (if we add that later)
          parentInterface = context.getInterface(parentName);
        }
        if (parentInterface == null) {
          throw new RuntimeException(
              "Parent interface not found: "
                  + parentName
                  + " for interface "
                  + phpInterface.getName());
        }

        // Check for circular inheritance
        if (hasCircularInterfaceInheritance(
            phpInterface.getName(), parentInterface, interfaceMap)) {
          throw new RuntimeException(
              "Circular inheritance detected for interface: " + phpInterface.getName());
        }

        // Set the parent interface
        phpInterface.addParentInterface(parentInterface);
      }
    }
  }

  private boolean hasCircularInterfaceInheritance(
      String originalInterfaceName,
      PhpInterface currentInterface,
      Map<String, PhpInterface> interfaceMap) {
    // Walk up the inheritance chain
    PhpInterface current = currentInterface;
    while (current != null) {
      if (current.getName().equals(originalInterfaceName)) {
        return true; // Found a cycle
      }
      String parentName = classContext.interfaceParentNames.get(current.getName());
      if (parentName == null) {
        break; // No more parents
      }
      current = interfaceMap.get(parentName);
    }
    return false;
  }

  private boolean hasCircularInheritance(
      String originalClassName, PhpClass currentClass, Map<String, PhpClass> classMap) {
    // Walk up the inheritance chain
    PhpClass current = currentClass;
    while (current != null) {
      if (current.getName().equals(originalClassName)) {
        return true; // Found a cycle
      }
      String parentName = classContext.classParentNames.get(current.getName());
      if (parentName == null) {
        break; // No more parents
      }
      current = classMap.get(parentName);
    }
    return false;
  }

  private void resolveTraitComposition(PhpContext context) {
    // Build a map of trait names to PhpTrait objects for quick lookup
    Map<String, PhpTrait> traitMap = new HashMap<>();
    for (PhpTrait phpTrait : traitContext.declaredTraits) {
      traitMap.put(phpTrait.getName(), phpTrait);
    }

    // Resolve trait usage (traits using other traits)
    for (PhpTrait phpTrait : traitContext.declaredTraits) {
      List<String> usedTraitNames = traitContext.traitUsedTraits.get(phpTrait.getName());
      if (usedTraitNames != null) {
        for (String usedTraitName : usedTraitNames) {
          // Find the used trait - check current file traits first, then context
          PhpTrait usedTrait = traitMap.get(usedTraitName);
          if (usedTrait == null) {
            usedTrait = context.getTrait(usedTraitName);
          }
          if (usedTrait == null) {
            throw new RuntimeException(
                "Trait not found: " + usedTraitName + " used by trait " + phpTrait.getName());
          }

          // Check for circular trait usage
          if (hasCircularTraitUsage(phpTrait.getName(), usedTrait, traitMap)) {
            throw new RuntimeException(
                "Circular trait usage detected for trait: " + phpTrait.getName());
          }

          // Add the used trait
          phpTrait.addUsedTrait(usedTrait);
        }
      }
    }
  }

  private boolean hasCircularTraitUsage(
      String originalTraitName, PhpTrait currentTrait, Map<String, PhpTrait> traitMap) {
    // Walk through the trait usage chain
    PhpTrait current = currentTrait;
    while (current != null) {
      if (current.getName().equals(originalTraitName)) {
        return true; // Found a cycle
      }
      List<String> usedTraitNames = traitContext.traitUsedTraits.get(current.getName());
      if (usedTraitNames == null || usedTraitNames.isEmpty()) {
        break; // No more traits used
      }
      // Check the first used trait (simplified circular detection)
      current = traitMap.get(usedTraitNames.get(0));
    }
    return false;
  }

  private PhpStatementNode parseStatement() {
    return statementParser.parseStatement();
  }

  private PhpStatementNode parseBlock() {
    return statementParser.parseBlock();
  }
}
