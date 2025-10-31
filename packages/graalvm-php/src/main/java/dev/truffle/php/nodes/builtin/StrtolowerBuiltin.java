package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

/**
 * Built-in function: strtolower
 * Converts a string to lowercase.
 */
public final class StrtolowerBuiltin extends PhpBuiltinRootNode {

    public StrtolowerBuiltin(PhpLanguage language) {
        super(language, "strtolower");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return "";
        }
        return args[0].toString().toLowerCase();
    }
}
