package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

/**
 * Built-in function: trim
 * Strips whitespace from the beginning and end of a string.
 */
public final class TrimBuiltin extends PhpBuiltinRootNode {

    public TrimBuiltin(PhpLanguage language) {
        super(language, "trim");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return "";
        }
        return args[0].toString().trim();
    }
}
