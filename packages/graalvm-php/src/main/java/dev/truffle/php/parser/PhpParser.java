package dev.truffle.php.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpRootNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.nodes.statement.PhpBlockNode;
import dev.truffle.php.runtime.PhpFunction;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpInterface;
import dev.truffle.php.runtime.PhpGlobalScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main coordinator for PHP parsing.
 *
 * This class orchestrates the parsing process by delegating to specialized parsers:
 * - PhpLexer: Tokenization and lexical analysis
 * - PhpExpressionParser: Expression parsing (literals, operators, variables, function calls)
 * - PhpStatementParser: Statement parsing (control flow, assignments, blocks)
 * - PhpClassParser: Class and interface declarations
 * - PhpFunctionParser: Function declarations
 *
 * Responsibilities:
 * - Initialize and coordinate all sub-parsers
 * - Parse top-level PHP source code into AST (Abstract Syntax Tree)
 * - Resolve inheritance relationships (class/interface hierarchies)
 * - Register all declarations (functions, classes, interfaces) in the context
 * - Validate semantic constraints (circular inheritance, abstract methods)
 *
 * The modular architecture allows each parser to focus on one responsibility while
 * maintaining clean separation of concerns through context objects and delegate patterns.
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
    private final PhpFunctionParser.FunctionParserContext functionContext;
    private final PhpFunctionParser functionParser;

    public PhpParser(PhpLanguage language, Source source, PhpGlobalScope globalScope) {
        this.language = language;
        this.source = source;
        this.lexer = new PhpLexer(source.getCharacters().toString());
        this.globalScope = globalScope;
        this.expressionContext = new PhpExpressionParser.ParserContext(null, null);
        this.expressionParser = new PhpExpressionParser(lexer, variables, globalScope, expressionContext);
        this.statementContext = new PhpStatementParser.StatementContext(null, null);
        this.classContext = new PhpClassParser.ClassParserContext();
        this.functionContext = new PhpFunctionParser.FunctionParserContext();

        // Create class parser with block delegate
        this.classParser = new PhpClassParser(
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
            }
        );

        // Create function parser with block delegate
        this.functionParser = new PhpFunctionParser(
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
            }
        );

        // Create statement parser with delegate for class/interface/function parsing
        this.statementParser = new PhpStatementParser(
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
                public PhpStatementNode parseFunction() {
                    return functionParser.parseFunction();
                }
            }
        );
    }

    public PhpRootNode parse() {
        lexer.skipPhpOpenTag();
        List<PhpStatementNode> statements = new ArrayList<>();

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

        // Get context to access built-in classes
        PhpContext context = PhpContext.get(rootNode);

        // Resolve parent class and interface references
        resolveInterfaceInheritance(context);
        resolveClassInheritance(context);

        // Register functions, classes, and interfaces in the context after parsing
        for (PhpFunction function : functionContext.declaredFunctions) {
            context.registerFunction(function);
        }
        for (PhpInterface phpInterface : classContext.declaredInterfaces) {
            context.registerInterface(phpInterface);
        }
        for (PhpClass phpClass : classContext.declaredClasses) {
            context.registerClass(phpClass);
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
                    throw new RuntimeException("Parent class not found: " + parentName + " for class " + phpClass.getName());
                }

                // Check for circular inheritance
                if (hasCircularInheritance(phpClass.getName(), parentClass, classMap)) {
                    throw new RuntimeException("Circular inheritance detected for class: " + phpClass.getName());
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
                        throw new RuntimeException("Interface not found: " + interfaceName + " for class " + phpClass.getName());
                    }

                    // Add the interface to the class
                    phpClass.addImplementedInterface(phpInterface);
                }
            }
        }

        // Validate abstract method implementation after all inheritance is resolved
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
                // Find the parent interface - check current file interfaces first, then built-in interfaces (if any)
                PhpInterface parentInterface = interfaceMap.get(parentName);
                if (parentInterface == null) {
                    // Check for built-in interfaces in context (if we add that later)
                    parentInterface = context.getInterface(parentName);
                }
                if (parentInterface == null) {
                    throw new RuntimeException("Parent interface not found: " + parentName + " for interface " + phpInterface.getName());
                }

                // Check for circular inheritance
                if (hasCircularInterfaceInheritance(phpInterface.getName(), parentInterface, interfaceMap)) {
                    throw new RuntimeException("Circular inheritance detected for interface: " + phpInterface.getName());
                }

                // Set the parent interface
                phpInterface.addParentInterface(parentInterface);
            }
        }
    }

    private boolean hasCircularInterfaceInheritance(String originalInterfaceName, PhpInterface currentInterface, Map<String, PhpInterface> interfaceMap) {
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

    private boolean hasCircularInheritance(String originalClassName, PhpClass currentClass, Map<String, PhpClass> classMap) {
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

    private PhpStatementNode parseStatement() {
        return statementParser.parseStatement();
    }

    private PhpStatementNode parseBlock() {
        return statementParser.parseBlock();
    }
}
