package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

/**
 * Built-in function: is_int
 * Checks if a variable is an integer.
 */
public final class IsIntBuiltin extends PhpBuiltinRootNode {

    public IsIntBuiltin(PhpLanguage language) {
        super(language, "is_int");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return false;
        }
        return args[0] instanceof Long;
    }
}
