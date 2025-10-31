package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

/**
 * Built-in function: is_null
 * Checks if a variable is null.
 */
public final class IsNullBuiltin extends PhpBuiltinRootNode {

    public IsNullBuiltin(PhpLanguage language) {
        super(language, "is_null");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return true;  // No argument is like null
        }
        return args[0] == null;
    }
}
