package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Built-in function: basename
 * Returns trailing name component of path.
 *
 * Usage: $name = basename('/path/to/file.txt');  // Returns 'file.txt'
 */
public final class BasenameBuiltin extends PhpBuiltinRootNode {

    public BasenameBuiltin(PhpLanguage language) {
        super(language, "basename");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            throw new RuntimeException("basename() expects at least 1 parameter, 0 given");
        }

        Object pathArg = args[0];
        if (!(pathArg instanceof String)) {
            return "";
        }

        String pathname = (String) pathArg;

        try {
            Path path = Paths.get(pathname);
            Path filename = path.getFileName();
            if (filename == null) {
                // Root path or empty
                return "";
            }

            String basename = filename.toString();

            // Handle optional suffix parameter
            if (args.length >= 2 && args[1] instanceof String) {
                String suffix = (String) args[1];
                if (basename.endsWith(suffix)) {
                    basename = basename.substring(0, basename.length() - suffix.length());
                }
            }

            return basename;
        } catch (Exception e) {
            return "";
        }
    }
}
