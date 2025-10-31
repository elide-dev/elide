package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

/**
 * Built-in function: in_array
 * Checks if a value exists in an array.
 */
public final class InArrayBuiltin extends PhpBuiltinRootNode {

    public InArrayBuiltin(PhpLanguage language) {
        super(language, "in_array");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length < 2) {
            return false;
        }

        Object needle = args[0];
        Object haystackArg = args[1];

        if (!(haystackArg instanceof PhpArray)) {
            return false;
        }

        PhpArray haystack = (PhpArray) haystackArg;
        for (Object key : haystack.keys()) {
            Object value = haystack.get(key);
            if (equals(needle, value)) {
                return true;
            }
        }

        return false;
    }

    private boolean equals(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
