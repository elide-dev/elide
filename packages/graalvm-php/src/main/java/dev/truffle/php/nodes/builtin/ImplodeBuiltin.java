package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;
import dev.truffle.php.runtime.PhpReference;

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
            Object value = array.get(key);
            // Unwrap PhpReference if present
            if (value instanceof PhpReference) {
                value = ((PhpReference) value).getValue();
            }
            result.append(value.toString());
            first = false;
        }

        return result.toString();
    }
}
