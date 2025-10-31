package dev.truffle.php;

import com.oracle.truffle.api.TruffleFile;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Detects PHP files based on file extension and content.
 */
public final class PhpFileDetector implements TruffleFile.FileTypeDetector {

    @Override
    public String findMimeType(TruffleFile file) throws IOException {
        String fileName = file.getName();
        if (fileName != null && fileName.endsWith(PhpLanguage.EXTENSION)) {
            return PhpLanguage.MIME_TYPE;
        }
        return null;
    }

    @Override
    public Charset findEncoding(TruffleFile file) throws IOException {
        // PHP files are UTF-8 by default
        return Charset.forName("UTF-8");
    }
}
