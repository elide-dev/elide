package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Built-in function: file_exists
 * Checks whether a file or directory exists.
 *
 * Usage: if (file_exists('path/to/file.txt')) { ... }
 */
public final class FileExistsBuiltin extends PhpBuiltinRootNode {

    public FileExistsBuiltin(PhpLanguage language) {
        super(language, "file_exists");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            throw new RuntimeException("file_exists() expects at least 1 parameter, 0 given");
        }

        Object fileArg = args[0];
        if (!(fileArg instanceof String)) {
            return false;
        }

        String filename = (String) fileArg;

        try {
            Path path = Paths.get(filename);
            return Files.exists(path);
        } catch (Exception e) {
            return false;
        }
    }
}
