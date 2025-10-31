package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Built-in function: file_get_contents
 * Reads entire file into a string.
 *
 * Usage: $content = file_get_contents('path/to/file.txt');
 */
public final class FileGetContentsBuiltin extends PhpBuiltinRootNode {

    public FileGetContentsBuiltin(PhpLanguage language) {
        super(language, "file_get_contents");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            throw new RuntimeException("file_get_contents() expects at least 1 parameter, 0 given");
        }

        Object fileArg = args[0];
        if (!(fileArg instanceof String)) {
            throw new RuntimeException("file_get_contents() expects parameter 1 to be string, " + getType(fileArg) + " given");
        }

        String filename = (String) fileArg;

        try {
            Path path = Paths.get(filename);
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, "UTF-8");
        } catch (IOException e) {
            // PHP returns false on failure
            return false;
        }
    }

    private String getType(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "string";
        if (value instanceof Long) return "integer";
        if (value instanceof Double) return "double";
        if (value instanceof Boolean) return "boolean";
        return "unknown";
    }
}
