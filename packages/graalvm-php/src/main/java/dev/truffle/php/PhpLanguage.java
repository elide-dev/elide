package dev.truffle.php;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import dev.truffle.php.parser.PhpParser;
import dev.truffle.php.runtime.PhpContext;

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
     * Get the context associated with a node.
     */
    public static PhpContext getContext(Node node) {
        return getCurrentContext(PhpLanguage.class);
    }
}
