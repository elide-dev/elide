package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node for calling built-in PHP functions.
 */
public final class PhpBuiltinCallNode extends PhpExpressionNode {

    private final String functionName;
    private final CallTarget callTarget;

    @Children
    private final PhpExpressionNode[] argumentNodes;

    public PhpBuiltinCallNode(String functionName, CallTarget callTarget, PhpExpressionNode[] argumentNodes) {
        this.functionName = functionName;
        this.callTarget = callTarget;
        this.argumentNodes = argumentNodes;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // Evaluate all argument expressions
        Object[] arguments = new Object[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            arguments[i] = argumentNodes[i].execute(frame);
        }

        // Call the built-in function
        return callTarget.call(arguments);
    }
}
