package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

import java.util.List;

/**
 * Built-in function: array_reverse
 * Return an array with elements in reverse order.
 * Usage: array_reverse(array)
 */
public final class ArrayReverseBuiltin extends PhpBuiltinRootNode {

    public ArrayReverseBuiltin(PhpLanguage language) {
        super(language, "array_reverse");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length < 1) {
            throw new RuntimeException("array_reverse() expects at least 1 parameter");
        }

        if (!(args[0] instanceof PhpArray)) {
            throw new RuntimeException("array_reverse() expects parameter 1 to be array");
        }

        PhpArray array = (PhpArray) args[0];
        List<Object> keys = array.keys();

        // Create result array with reversed elements
        PhpArray result = new PhpArray();
        for (int i = keys.size() - 1; i >= 0; i--) {
            Object key = keys.get(i);
            Object value = array.get(key);
            result.append(value);
        }

        return result;
    }
}
