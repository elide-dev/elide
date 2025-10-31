package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

/**
 * Built-in function: strtoupper
 * Converts a string to uppercase.
 */
public final class StrtoupperBuiltin extends PhpBuiltinRootNode {

    public StrtoupperBuiltin(PhpLanguage language) {
        super(language, "strtoupper");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return "";
        }
        return args[0].toString().toUpperCase();
    }
}
