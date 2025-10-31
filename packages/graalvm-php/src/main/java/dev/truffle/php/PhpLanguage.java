package dev.truffle.php;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import dev.truffle.php.nodes.PhpRootNode;
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
        // Get the context to access the global scope
        PhpContext context = getCurrentContext(PhpLanguage.class);
        PhpParser parser = new PhpParser(this, request.getSource(), context.getGlobalScope());
        return parser.parse().getCallTarget();
    }

    /**
     * Parse and execute a source file.
     * This is used by include/require statements to execute included files.
     * The included file shares the same global scope and frame as the parent file.
     */
    public Object parseAndExecute(Source source, VirtualFrame frame) {
        PhpContext context = getCurrentContext(PhpLanguage.class);
        PhpParser parser = new PhpParser(this, source, context.getGlobalScope());
        PhpRootNode rootNode = parser.parse();

        // Execute the included file's body directly in the current frame
        // This allows the included file to access and modify variables from the parent scope
        return rootNode.execute(frame);
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
