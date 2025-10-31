package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

/**
 * Built-in function: count
 * Returns the number of elements in an array.
 */
public final class CountBuiltin extends PhpBuiltinRootNode {

    public CountBuiltin(PhpLanguage language) {
        super(language, "count");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return 0L;
        }

        Object arg = args[0];
        if (arg instanceof PhpArray) {
            return (long) ((PhpArray) arg).size();
        }

        // Non-arrays have count of 1 in PHP (or 0 for null)
        return arg == null ? 0L : 1L;
    }
}
