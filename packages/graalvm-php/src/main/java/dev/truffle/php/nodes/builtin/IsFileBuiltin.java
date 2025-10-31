package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Built-in function: is_file
 * Checks whether the given path is a regular file.
 *
 * Usage: if (is_file('path/to/file.txt')) { ... }
 */
public final class IsFileBuiltin extends PhpBuiltinRootNode {

    public IsFileBuiltin(PhpLanguage language) {
        super(language, "is_file");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            throw new RuntimeException("is_file() expects at least 1 parameter, 0 given");
        }

        Object fileArg = args[0];
        if (!(fileArg instanceof String)) {
            return false;
        }

        String filename = (String) fileArg;

        try {
            Path path = Paths.get(filename);
            return Files.isRegularFile(path);
        } catch (Exception e) {
            return false;
        }
    }
}
