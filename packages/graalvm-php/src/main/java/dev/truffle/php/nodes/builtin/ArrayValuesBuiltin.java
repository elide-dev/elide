package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

/**
 * Built-in function: array_values
 * Returns all the values of an array.
 */
public final class ArrayValuesBuiltin extends PhpBuiltinRootNode {

    public ArrayValuesBuiltin(PhpLanguage language) {
        super(language, "array_values");
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
            result.append(array.get(key));
        }

        return result;
    }
}
