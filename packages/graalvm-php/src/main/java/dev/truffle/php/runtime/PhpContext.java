package dev.truffle.php.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import dev.truffle.php.PhpLanguage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

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

    public PhpContext(PhpLanguage language, TruffleLanguage.Env env) {
        this.language = language;
        this.env = env;
        this.input = new BufferedReader(new InputStreamReader(env.in()));
        this.output = new PrintWriter(env.out(), true);
        this.error = new PrintWriter(env.err(), true);
        this.functions = new HashMap<>();
        this.builtins = new HashMap<>();
        this.classes = new HashMap<>();
        // Initialize built-in functions for this context
        PhpBuiltinRegistry.initializeBuiltins(this, language);
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
     * Get the context from a node.
     * This is a convenience method that looks up the context from the node's language.
     */
    public static PhpContext get(Node node) {
        return PhpLanguage.getContext(node);
    }
}
