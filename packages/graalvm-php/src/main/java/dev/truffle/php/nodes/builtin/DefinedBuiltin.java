package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpContext;

/**
 * Built-in PHP function: defined($constant_name)
 * Checks whether a given named constant exists.
 */
public final class DefinedBuiltin extends PhpBuiltinRootNode {

    public DefinedBuiltin(PhpLanguage language) {
        super(language, "defined");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {

        // Require exactly 1 argument
        if (args.length < 1) {
            throw new RuntimeException("defined() expects exactly 1 argument");
        }

        // Get constant name (must be string)
        Object nameArg = args[0];
        if (!(nameArg instanceof String)) {
            throw new RuntimeException("defined() expects parameter 1 to be string");
        }
        String name = (String) nameArg;

        // Check if constant is defined in the context
        PhpContext context = PhpContext.get(this);
        return context.isConstantDefined(name);
    }
}
