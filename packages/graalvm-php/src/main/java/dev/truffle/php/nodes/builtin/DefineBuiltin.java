package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpContext;

/**
 * Built-in PHP function: define($name, $value, $case_insensitive = false)
 * Defines a named constant.
 */
public final class DefineBuiltin extends PhpBuiltinRootNode {

    public DefineBuiltin(PhpLanguage language) {
        super(language, "define");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {

        // Require at least 2 arguments (name and value)
        if (args.length < 2) {
            throw new RuntimeException("define() expects at least 2 arguments");
        }

        // Get constant name (must be string)
        Object nameArg = args[0];
        if (!(nameArg instanceof String)) {
            throw new RuntimeException("define() expects parameter 1 to be string");
        }
        String name = (String) nameArg;

        // Get constant value (can be any type)
        Object value = args[1];

        // Optional case_insensitive parameter (deprecated in PHP 7.3+, we'll ignore it)
        // PHP constants are case-sensitive by default

        // Define the constant in the context
        PhpContext context = PhpContext.get(this);
        context.defineConstant(name, value);

        // define() returns true on success
        return true;
    }
}
