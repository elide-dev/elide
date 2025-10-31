package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

/**
 * Built-in function: strlen
 * Returns the length of a string.
 */
public final class StrlenBuiltin extends PhpBuiltinRootNode {

    public StrlenBuiltin(PhpLanguage language) {
        super(language, "strlen");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return 0L;
        }
        Object arg = args[0];
        if (arg instanceof String) {
            return (long) ((String) arg).length();
        }
        return 0L;
    }
}
