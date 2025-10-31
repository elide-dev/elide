package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

/**
 * Built-in function: array_keys
 * Returns all the keys of an array.
 */
public final class ArrayKeysBuiltin extends PhpBuiltinRootNode {

    public ArrayKeysBuiltin(PhpLanguage language) {
        super(language, "array_keys");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return new PhpArray();
        }

        Object arrayArg = args[0];
        if (!(arrayArg instanceof PhpArray)) {
            return new PhpArray();
        }

        PhpArray array = (PhpArray) arrayArg;
        PhpArray result = new PhpArray();

        for (Object key : array.keys()) {
            result.append(key);
        }

        return result;
    }
}
