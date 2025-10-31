package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

/**
 * Built-in function: array_merge
 * Merges one or more arrays.
 */
public final class ArrayMergeBuiltin extends PhpBuiltinRootNode {

    public ArrayMergeBuiltin(PhpLanguage language) {
        super(language, "array_merge");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        PhpArray result = new PhpArray();

        for (Object arg : args) {
            if (arg instanceof PhpArray) {
                PhpArray array = (PhpArray) arg;
                for (Object key : array.keys()) {
                    result.append(array.get(key));
                }
            }
        }

        return result;
    }
}
