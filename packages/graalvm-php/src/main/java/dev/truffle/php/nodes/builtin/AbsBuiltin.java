package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

/**
 * Built-in function: abs
 * Returns the absolute value of a number.
 */
public final class AbsBuiltin extends PhpBuiltinRootNode {

    public AbsBuiltin(PhpLanguage language) {
        super(language, "abs");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return 0L;
        }

        Object arg = args[0];
        if (arg instanceof Long) {
            return Math.abs((Long) arg);
        } else if (arg instanceof Double) {
            return Math.abs((Double) arg);
        }

        return 0L;
    }
}
