package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

/**
 * Built-in function: gettype
 * Returns the type of a variable as a string.
 */
public final class GettypeBuiltin extends PhpBuiltinRootNode {

    public GettypeBuiltin(PhpLanguage language) {
        super(language, "gettype");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0 || args[0] == null) {
            return "NULL";
        }

        Object arg = args[0];
        if (arg instanceof Long) {
            return "integer";
        } else if (arg instanceof Double) {
            return "double";
        } else if (arg instanceof String) {
            return "string";
        } else if (arg instanceof Boolean) {
            return "boolean";
        } else if (arg instanceof PhpArray) {
            return "array";
        } else {
            return "unknown type";
        }
    }
}
