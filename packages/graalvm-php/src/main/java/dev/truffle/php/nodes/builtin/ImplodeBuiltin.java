package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

/**
 * Built-in function: implode
 * Joins array elements with a string.
 * implode(glue, array)
 */
public final class ImplodeBuiltin extends PhpBuiltinRootNode {

    public ImplodeBuiltin(PhpLanguage language) {
        super(language, "implode");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length < 2) {
            return "";
        }

        String glue = args[0].toString();
        Object arrayArg = args[1];

        if (!(arrayArg instanceof PhpArray)) {
            return "";
        }

        PhpArray array = (PhpArray) arrayArg;
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Object key : array.keys()) {
            if (!first) {
                result.append(glue);
            }
            result.append(array.get(key).toString());
            first = false;
        }

        return result.toString();
    }
}
