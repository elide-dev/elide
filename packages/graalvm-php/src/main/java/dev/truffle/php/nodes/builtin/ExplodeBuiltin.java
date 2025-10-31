package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;

/**
 * Built-in function: explode
 * Splits a string by a delimiter into an array.
 * explode(delimiter, string)
 */
public final class ExplodeBuiltin extends PhpBuiltinRootNode {

    public ExplodeBuiltin(PhpLanguage language) {
        super(language, "explode");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length < 2) {
            return new PhpArray();
        }

        String delimiter = args[0].toString();
        String string = args[1].toString();

        String[] parts = string.split(java.util.regex.Pattern.quote(delimiter));
        PhpArray result = new PhpArray();
        for (String part : parts) {
            result.append(part);
        }

        return result;
    }
}
