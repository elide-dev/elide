package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Built-in function: dirname
 * Returns a parent directory's path.
 *
 * Usage: $dir = dirname('/path/to/file.txt');  // Returns '/path/to'
 */
public final class DirnameBuiltin extends PhpBuiltinRootNode {

    public DirnameBuiltin(PhpLanguage language) {
        super(language, "dirname");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            throw new RuntimeException("dirname() expects at least 1 parameter, 0 given");
        }

        Object pathArg = args[0];
        if (!(pathArg instanceof String)) {
            return "";
        }

        String pathname = (String) pathArg;

        try {
            Path path = Paths.get(pathname);
            Path parent = path.getParent();
            if (parent == null) {
                // No parent directory (e.g., relative path like "file.txt" or root "/")
                return ".";
            }
            return parent.toString();
        } catch (Exception e) {
            return ".";
        }
    }
}
