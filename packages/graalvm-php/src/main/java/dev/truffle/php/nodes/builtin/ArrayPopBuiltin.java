package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

import java.util.List;

/**
 * Built-in function: array_pop
 * Pops and returns the last element from an array.
 */
public final class ArrayPopBuiltin extends PhpBuiltinRootNode {

    public ArrayPopBuiltin(PhpLanguage language) {
        super(language, "array_pop");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return null;
        }

        Object arrayArg = args[0];
        if (!(arrayArg instanceof PhpArray)) {
            return null;
        }

        PhpArray array = (PhpArray) arrayArg;
        List<Object> keys = array.keys();
        if (keys.isEmpty()) {
            return null;
        }

        // Get last key and remove it
        Object lastKey = keys.get(keys.size() - 1);
        Object value = array.get(lastKey);
        array.remove(lastKey);

        return value;
    }
}
