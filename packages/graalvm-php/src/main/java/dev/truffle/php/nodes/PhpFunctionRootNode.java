package dev.truffle.php.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.statement.PhpReturnNode;

/**
 * Root node for PHP function execution.
 */
public final class PhpFunctionRootNode extends RootNode {

    private final String functionName;
    private final String[] parameterNames;
    private final int[] parameterSlots;

    @Child
    private PhpStatementNode body;

    public PhpFunctionRootNode(
        PhpLanguage language,
        FrameDescriptor frameDescriptor,
        String functionName,
        String[] parameterNames,
        int[] parameterSlots,
        PhpStatementNode body
    ) {
        super(language, frameDescriptor);
        this.functionName = functionName;
        this.parameterNames = parameterNames;
        this.parameterSlots = parameterSlots;
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // Initialize parameters from arguments
        Object[] arguments = frame.getArguments();
        for (int i = 0; i < parameterSlots.length && i < arguments.length; i++) {
            frame.setObject(parameterSlots[i], arguments[i]);
        }

        try {
            body.executeVoid(frame);
            return null; // Functions without explicit return return null
        } catch (PhpReturnNode.PhpReturnException e) {
            return e.getResult();
        }
    }

    @Override
    public String getName() {
        return functionName;
    }
}
