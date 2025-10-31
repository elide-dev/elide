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
    private final Set<String> includedFiles;

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
        this.includedFiles = new HashSet<>();
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
     * Get a function by name.
     */
    public PhpFunction getFunction(String name) {
        return functions.get(name);
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
     * Get a class by name.
     */
    public PhpClass getClass(String name) {
        return classes.get(name);
    }

    /**
     * Register an interface in the context.
     */
    public void registerInterface(PhpInterface phpInterface) {
        interfaces.put(phpInterface.getName(), phpInterface);
    }

    /**
     * Get an interface by name.
     */
    public PhpInterface getInterface(String name) {
        return interfaces.get(name);
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
     * Get the context from a node.
     * This is a convenience method that looks up the context from the node's language.
     */
    public static PhpContext get(Node node) {
        return PhpLanguage.getContext(node);
    }
}
