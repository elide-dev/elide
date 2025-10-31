package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

/**
 * Built-in function: is_string
 * Checks if a variable is a string.
 */
public final class IsStringBuiltin extends PhpBuiltinRootNode {

    public IsStringBuiltin(PhpLanguage language) {
        super(language, "is_string");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return false;
        }
        return args[0] instanceof String;
    }
}
