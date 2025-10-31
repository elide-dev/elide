package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

/**
 * Built-in function: array_push
 * Pushes one or more elements onto the end of an array.
 * Returns the new array size.
 */
public final class ArrayPushBuiltin extends PhpBuiltinRootNode {

    public ArrayPushBuiltin(PhpLanguage language) {
        super(language, "array_push");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length < 2) {
            return 0L;
        }

        Object arrayArg = args[0];
        if (!(arrayArg instanceof PhpArray)) {
            return 0L;
        }

        PhpArray array = (PhpArray) arrayArg;
        for (int i = 1; i < args.length; i++) {
            array.append(args[i]);
        }

        return (long) array.size();
    }
}
