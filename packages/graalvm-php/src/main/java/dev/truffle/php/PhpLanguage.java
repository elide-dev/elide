package dev.truffle.php;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import dev.truffle.php.parser.PhpParser;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpBuiltinRegistry;

/**
 * TrufflePHP Language Implementation
 *
 * This is the main entry point for the PHP language implementation in Truffle.
 * It handles language initialization, context creation, and program execution.
 */
@TruffleLanguage.Registration(
    id = "php",
    name = "PHP",
    defaultMimeType = PhpLanguage.MIME_TYPE,
    characterMimeTypes = PhpLanguage.MIME_TYPE,
    contextPolicy = TruffleLanguage.ContextPolicy.SHARED,
    fileTypeDetectors = PhpFileDetector.class
)
public final class PhpLanguage extends TruffleLanguage<PhpContext> {

    public static final String ID = "php";
    public static final String MIME_TYPE = "application/x-php";
    public static final String EXTENSION = ".php";

    @Override
    protected PhpContext createContext(Env env) {
        return new PhpContext(this, env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        PhpParser parser = new PhpParser(this, request.getSource());
        return parser.parse().getCallTarget();
    }

    /**
     * Parse and execute a source file.
     * This is used by include/require statements to execute included files.
     */
    public Object parseAndExecute(Source source, VirtualFrame frame) {
        PhpParser parser = new PhpParser(this, source);
        CallTarget callTarget = parser.parse().getCallTarget();
        // Execute the included file with the current frame
        return callTarget.call();
    }

    /**
     * Get the language instance from a node.
     */
    public static PhpLanguage get(Node node) {
        return getCurrentLanguage(PhpLanguage.class);
    }

    /**
     * Get the context associated with a node.
     */
    public static PhpContext getContext(Node node) {
        return getCurrentContext(PhpLanguage.class);
    }
}
