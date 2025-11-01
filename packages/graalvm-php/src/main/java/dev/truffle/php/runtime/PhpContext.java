package dev.truffle.php.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import dev.truffle.php.PhpLanguage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * PHP execution context.
 *
 * Holds the state for a PHP language execution, including:
 * - Global variables and scope
 * - Standard I/O streams
 * - Language environment
 * - Function registry
 * - Built-in function registry
 * - Class registry
 * - Interface registry
 * - Trait registry
 */
public final class PhpContext {

    private final PhpLanguage language;
    private final TruffleLanguage.Env env;
    private final BufferedReader input;
    private final PrintWriter output;
    private final PrintWriter error;
    private final Map<String, PhpFunction> functions;
    private final Map<String, CallTarget> builtins;
    private final Map<String, PhpClass> classes;
    private final Map<String, PhpInterface> interfaces;
    private final Map<String, PhpTrait> traits;
    private final Map<String, Object> constants;
    private final Set<String> includedFiles;
    private final PhpGlobalScope globalScope;
    private final PhpNamespaceContext namespaceContext;

    public PhpContext(PhpLanguage language, TruffleLanguage.Env env) {
        this.language = language;
        this.env = env;
        this.input = new BufferedReader(new InputStreamReader(env.in()));
        this.output = new PrintWriter(env.out(), true);
        this.error = new PrintWriter(env.err(), true);
        this.functions = new HashMap<>();
        this.builtins = new HashMap<>();
        this.classes = new HashMap<>();
        this.interfaces = new HashMap<>();
        this.traits = new HashMap<>();
        this.constants = new HashMap<>();
        this.includedFiles = new HashSet<>();
        this.globalScope = new PhpGlobalScope();
        this.namespaceContext = new PhpNamespaceContext();
        // Initialize built-in functions for this context
        PhpBuiltinRegistry.initializeBuiltins(this, language);
        // Initialize built-in classes
        initializeBuiltinClasses();
    }

    /**
     * Initialize built-in PHP classes like Exception.
     */
    private void initializeBuiltinClasses() {
        // Create built-in Exception class
        Map<String, PhpClass.PropertyMetadata> exceptionProperties = new HashMap<>();
        exceptionProperties.put("message", new PhpClass.PropertyMetadata("message", Visibility.PUBLIC, false, ""));
        exceptionProperties.put("code", new PhpClass.PropertyMetadata("code", Visibility.PUBLIC, false, 0L));

        Map<String, PhpClass.MethodMetadata> exceptionMethods = new HashMap<>();

        // Create constructor for Exception class
        // Constructor: __construct($message = "", $code = 0)
        CallTarget exceptionConstructor = PhpExceptionConstructor.create(language).getCallTarget();

        PhpClass exceptionClass = new PhpClass("Exception", exceptionProperties, exceptionMethods, exceptionConstructor, false);
        registerClass(exceptionClass);
    }

    public PhpLanguage getLanguage() {
        return language;
    }

    public TruffleLanguage.Env getEnv() {
        return env;
    }

    public BufferedReader getInput() {
        return input;
    }

    public PrintWriter getOutput() {
        return output;
    }

    public PrintWriter getError() {
        return error;
    }

    /**
     * Register a function in the context.
     */
    public void registerFunction(PhpFunction function) {
        functions.put(function.getName(), function);
    }

    /**
     * Get a function by name (with namespace resolution).
     */
    public PhpFunction getFunction(String name) {
        // First try the name as-is (for backward compatibility)
        PhpFunction func = functions.get(name);
        if (func != null) {
            return func;
        }

        // Try resolving with namespace context
        String resolved = namespaceContext.resolveFunctionName(name);
        return functions.get(resolved);
    }

    /**
     * Get a function by its fully qualified name.
     */
    public PhpFunction getFunctionByQualifiedName(String qualifiedName) {
        return functions.get(qualifiedName);
    }

    /**
     * Register a built-in function in the context.
     */
    public void registerBuiltin(String name, CallTarget callTarget) {
        builtins.put(name, callTarget);
    }

    /**
     * Get a built-in function CallTarget by name.
     */
    public CallTarget getBuiltin(String name) {
        return builtins.get(name);
    }

    /**
     * Register a class in the context.
     */
    public void registerClass(PhpClass phpClass) {
        classes.put(phpClass.getName(), phpClass);
    }

    /**
     * Get a class by name (with namespace resolution).
     */
    public PhpClass getClass(String name) {
        // First try the name as-is (for backward compatibility and built-ins)
        PhpClass phpClass = classes.get(name);
        if (phpClass != null) {
            return phpClass;
        }

        // Try resolving with namespace context
        String resolved = namespaceContext.resolveClassName(name);
        return classes.get(resolved);
    }

    /**
     * Get a class by its fully qualified name.
     */
    public PhpClass getClassByQualifiedName(String qualifiedName) {
        return classes.get(qualifiedName);
    }

    /**
     * Register an interface in the context.
     */
    public void registerInterface(PhpInterface phpInterface) {
        interfaces.put(phpInterface.getName(), phpInterface);
    }

    /**
     * Get an interface by name (with namespace resolution).
     */
    public PhpInterface getInterface(String name) {
        // First try the name as-is
        PhpInterface iface = interfaces.get(name);
        if (iface != null) {
            return iface;
        }

        // Try resolving with namespace context
        String resolved = namespaceContext.resolveClassName(name);  // Interfaces use class name resolution rules
        return interfaces.get(resolved);
    }

    /**
     * Get an interface by its fully qualified name.
     */
    public PhpInterface getInterfaceByQualifiedName(String qualifiedName) {
        return interfaces.get(qualifiedName);
    }

    /**
     * Register a trait in the context.
     */
    public void registerTrait(PhpTrait phpTrait) {
        traits.put(phpTrait.getName(), phpTrait);
    }

    /**
     * Get a trait by name (with namespace resolution).
     */
    public PhpTrait getTrait(String name) {
        // First try the name as-is
        PhpTrait trait = traits.get(name);
        if (trait != null) {
            return trait;
        }

        // Try resolving with namespace context (traits use class name resolution rules)
        String resolved = namespaceContext.resolveClassName(name);
        return traits.get(resolved);
    }

    /**
     * Get a trait by its fully qualified name.
     */
    public PhpTrait getTraitByQualifiedName(String qualifiedName) {
        return traits.get(qualifiedName);
    }

    /**
     * Check if a file has already been included (for *_once variants).
     */
    public boolean isFileIncluded(String path) {
        return includedFiles.contains(path);
    }

    /**
     * Mark a file as included (for *_once variants).
     */
    public void markFileIncluded(String path) {
        includedFiles.add(path);
    }

    /**
     * Get the global scope for this context.
     * The global scope manages all top-level variables and is shared across included files.
     */
    public PhpGlobalScope getGlobalScope() {
        return globalScope;
    }

    /**
     * Get the namespace context for this execution.
     * The namespace context manages namespace declarations and use statements.
     */
    public PhpNamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    /**
     * Find a class that uses the given trait and has the given static property.
     * This is needed for resolving self:: in trait methods at runtime.
     *
     * @param traitName The name of the trait
     * @param propertyName The name of the static property
     * @return The PhpClass that uses the trait and has the property, or null if not found
     */
    public PhpClass findClassUsingTraitWithProperty(String traitName, String propertyName) {
        // Iterate through all registered classes
        for (PhpClass phpClass : classes.values()) {
            // Check if this class uses the trait
            boolean usesTrait = false;
            for (PhpTrait usedTrait : phpClass.getUsedTraits()) {
                if (usedTrait.getName().equals(traitName)) {
                    usesTrait = true;
                    break;
                }
            }

            // If the class uses the trait and has the static property, return it
            if (usesTrait && phpClass.hasStaticProperty(propertyName)) {
                return phpClass;
            }
        }

        return null;
    }

    /**
     * Define a constant in the context.
     * Constants are case-sensitive by default.
     */
    public void defineConstant(String name, Object value) {
        constants.put(name, value);
    }

    /**
     * Get a constant by name.
     * Returns null if the constant is not defined.
     */
    public Object getConstant(String name) {
        return constants.get(name);
    }

    /**
     * Check if a constant is defined.
     */
    public boolean isConstantDefined(String name) {
        return constants.containsKey(name);
    }

    /**
     * Get the context from a node.
     * This is a convenience method that looks up the context from the node's language.
     */
    public static PhpContext get(Node node) {
        return PhpLanguage.getContext(node);
    }
}
