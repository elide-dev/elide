package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

/**
 * Built-in function: array_key_exists
 * Checks if the given key or index exists in the array.
 * Usage: array_key_exists(key, array)
 * Returns true if key exists, false otherwise.
 */
public final class ArrayKeyExistsBuiltin extends PhpBuiltinRootNode {

    public ArrayKeyExistsBuiltin(PhpLanguage language) {
        super(language, "array_key_exists");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length < 2) {
            throw new RuntimeException("array_key_exists() expects 2 parameters");
        }

        Object key = args[0];

        if (!(args[1] instanceof PhpArray)) {
            throw new RuntimeException("array_key_exists() expects parameter 2 to be array");
        }

        PhpArray array = (PhpArray) args[1];
        return array.containsKey(key);
    }
}
