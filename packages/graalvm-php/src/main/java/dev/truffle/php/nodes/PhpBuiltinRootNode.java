package dev.truffle.php.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import dev.truffle.php.PhpLanguage;

/**
 * Base class for built-in PHP function root nodes.
 * Built-in functions are implemented in Java and registered in the PhpBuiltinRegistry.
 */
public abstract class PhpBuiltinRootNode extends RootNode {

    private final String functionName;

    protected PhpBuiltinRootNode(PhpLanguage language, String functionName) {
        super(language);
        this.functionName = functionName;
    }

    public String getFunctionName() {
        return functionName;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // Arguments are passed as frame arguments
        Object[] args = frame.getArguments();
        return executeBuiltin(args);
    }

    /**
     * Execute the built-in function with the given arguments.
     * @param args The arguments passed to the function
     * @return The result of the function
     */
    protected abstract Object executeBuiltin(Object[] args);
}
