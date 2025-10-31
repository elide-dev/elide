package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

/**
 * Built-in function: str_replace
 * Replaces all occurrences of search with replace in subject.
 * str_replace(search, replace, subject)
 */
public final class StrReplaceBuiltin extends PhpBuiltinRootNode {

    public StrReplaceBuiltin(PhpLanguage language) {
        super(language, "str_replace");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length < 3) {
            return "";
        }

        String search = args[0].toString();
        String replace = args[1].toString();
        String subject = args[2].toString();

        return subject.replace(search, replace);
    }
}
