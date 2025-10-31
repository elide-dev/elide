package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

/**
 * Built-in function: is_array
 * Checks if a variable is an array.
 */
public final class IsArrayBuiltin extends PhpBuiltinRootNode {

    public IsArrayBuiltin(PhpLanguage language) {
        super(language, "is_array");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return false;
        }
        return args[0] instanceof PhpArray;
    }
}
