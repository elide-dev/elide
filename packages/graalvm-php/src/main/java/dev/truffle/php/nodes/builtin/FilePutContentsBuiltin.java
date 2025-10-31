package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Built-in function: file_put_contents
 * Writes a string to a file.
 *
 * Usage: $bytes = file_put_contents('path/to/file.txt', $data);
 */
public final class FilePutContentsBuiltin extends PhpBuiltinRootNode {

    public FilePutContentsBuiltin(PhpLanguage language) {
        super(language, "file_put_contents");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length < 2) {
            throw new RuntimeException("file_put_contents() expects at least 2 parameters, " + args.length + " given");
        }

        Object fileArg = args[0];
        Object dataArg = args[1];

        if (!(fileArg instanceof String)) {
            throw new RuntimeException("file_put_contents() expects parameter 1 to be string");
        }

        String filename = (String) fileArg;
        String data = coerceToString(dataArg);

        try {
            Path path = Paths.get(filename);
            // Create parent directories if they don't exist
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            byte[] bytes = data.getBytes("UTF-8");
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return (long) bytes.length;
        } catch (IOException e) {
            // PHP returns false on failure
            return false;
        }
    }

    private String coerceToString(Object value) {
        if (value == null) return "";
        if (value instanceof String) return (String) value;
        if (value instanceof Long) return value.toString();
        if (value instanceof Double) return value.toString();
        if (value instanceof Boolean) return (Boolean) value ? "1" : "";
        return value.toString();
    }
}
